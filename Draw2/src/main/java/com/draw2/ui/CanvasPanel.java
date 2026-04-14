package com.draw2.ui;

import com.draw2.model.CanvasModel;
import com.draw2.tool.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;

public class CanvasPanel extends JPanel {

    private static final int BORDER = 12;
    private static final Color BACKGROUND = new Color(0x80, 0x80, 0x80); // classic grey

    private final CanvasModel model;
    private Tool activeTool;
    private DrawContext drawContext;

    private BufferedImage overlayImage;
    private double scale = 1.0;
    private boolean gridEnabled;

    // Pan state
    private Point panStart;
    private double panTx, panTy;
    private double translateX, translateY;

    // TextField for TextTool
    private JTextField activeTextField;
    private Point textScreenPoint;

    // Callbacks
    private Runnable repaintHook;
    private java.util.function.BiConsumer<Integer, Integer> cursorCallback;
    private java.util.function.Consumer<Double> zoomCallback;

    public CanvasPanel(CanvasModel model) {
        this.model = model;
        setLayout(null); // for JTextField overlay
        rebuildOverlay();

        model.addPropertyChangeListener(this::onModelChange);

        setupMouseListeners();
        setupPropertyChangeListeners();
        setFocusable(true);

        // Keyboard: Space for pan detection is handled in MainWindow
    }

    // ── setup ─────────────────────────────────────────────────────────────────

    private void setupMouseListeners() {
        MouseAdapter ma = new MouseAdapter() {
            private boolean panning;
            private boolean rightButton;

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                rightButton = SwingUtilities.isRightMouseButton(e);

                if (SwingUtilities.isMiddleMouseButton(e)) {
                    startPan(e.getPoint());
                    return;
                }
                if (activeTool == null || drawContext == null) return;

                Point cp = screenToCanvas(e.getPoint());

                if (activeTool instanceof EyedropperTool) {
                    ((EyedropperTool) activeTool).setRightClick(rightButton);
                    activeTool.onPress(cp, drawContext);
                    return;
                }

                DrawContext ctx = rightButton ? drawContext.withSwappedColors() : drawContext;
                activeTool.onPress(cp, ctx);
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (panning) {
                    doPan(e.getPoint());
                    return;
                }
                if (activeTool == null || drawContext == null) return;
                Point cp = screenToCanvas(e.getPoint());
                updateCursor(cp);

                DrawContext ctx = rightButton ? drawContext.withSwappedColors() : drawContext;
                if (activeTool instanceof EyedropperTool)
                    ((EyedropperTool) activeTool).setRightClick(rightButton);

                activeTool.onDrag(cp, ctx);
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (panning) { panning = false; return; }
                if (activeTool == null || drawContext == null) return;
                Point cp = screenToCanvas(e.getPoint());

                DrawContext ctx = rightButton ? drawContext.withSwappedColors() : drawContext;
                activeTool.onRelease(cp, ctx);
                clearOverlay();
                repaint();
                rightButton = false;
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point cp = screenToCanvas(e.getPoint());
                updateCursor(cp);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown()) {
                    if (e.getWheelRotation() < 0) zoomIn();
                    else                          zoomOut();
                } else {
                    // Let the parent scroll pane handle it
                    getParent().dispatchEvent(e);
                }
            }

            private void startPan(Point screen) {
                panning = true;
                panStart = screen;
                panTx = translateX;
                panTy = translateY;
            }

            private void doPan(Point screen) {
                translateX = panTx + (screen.x - panStart.x);
                translateY = panTy + (screen.y - panStart.y);
                revalidate();
                repaint();
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);
    }

    private void setupPropertyChangeListeners() {
        addPropertyChangeListener("showTextField", (PropertyChangeEvent e) -> {
            if (activeTextField != null) removeTextField();
            JTextField tf = (JTextField) getClientProperty("textField");
            Point cp = (Point) getClientProperty("textPoint");
            if (tf == null || cp == null) return;
            activeTextField = tf;
            textScreenPoint = canvasToScreen(cp);
            tf.setBounds(textScreenPoint.x, textScreenPoint.y, 200, 28);
            add(tf);
            revalidate();
            repaint();
            tf.requestFocusInWindow();
        });

        addPropertyChangeListener("hideTextField", (PropertyChangeEvent e) -> {
            removeTextField();
        });
    }

    private void removeTextField() {
        if (activeTextField != null) {
            remove(activeTextField);
            activeTextField = null;
            revalidate();
            repaint();
        }
    }

    // ── public API ────────────────────────────────────────────────────────────

    public void setActiveTool(Tool tool) {
        if (activeTool != null) activeTool.onDeactivate();
        activeTool = tool;
        if (tool != null) {
            tool.onActivate();
            setCursor(tool.getCursor());
        }
        clearOverlay();
        repaint();
    }

    public void setDrawContext(DrawContext ctx) {
        this.drawContext = ctx;
    }

    public void setCursorCallback(java.util.function.BiConsumer<Integer, Integer> cb) {
        cursorCallback = cb;
    }

    public void setZoomCallback(java.util.function.Consumer<Double> cb) {
        zoomCallback = cb;
    }

    public double getScale() { return scale; }

    public void zoomIn()  { setScale(Math.min(scale * 2.0, 16.0)); }
    public void zoomOut() { setScale(Math.max(scale / 2.0, 0.0625)); }
    public void zoomReset() { setScale(1.0); }

    public void setGridEnabled(boolean b) { gridEnabled = b; repaint(); }

    public void clearOverlay() {
        if (overlayImage == null) return;
        Graphics2D g2 = overlayImage.createGraphics();
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, overlayImage.getWidth(), overlayImage.getHeight());
        g2.dispose();
    }

    // ── painting ──────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Grey background
        g2.setColor(BACKGROUND);
        g2.fillRect(0, 0, getWidth(), getHeight());

        int cx = (int) translateX + BORDER;
        int cy = (int) translateY + BORDER;
        int cw = (int) (model.getWidth()  * scale);
        int ch = (int) (model.getHeight() * scale);

        // Canvas shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(cx + 3, cy + 3, cw, ch);

        // Canvas image
        g2.drawImage(model.getCanvas(), cx, cy, cw, ch, null);

        // Overlay (shape preview)
        if (activeTool != null && drawContext != null && overlayImage != null) {
            activeTool.onPaint(overlayImage, drawContext);
            g2.drawImage(overlayImage, cx, cy, cw, ch, null);
        }

        // Grid
        if (gridEnabled && scale >= 4.0) {
            g2.setColor(new Color(0, 0, 0, 60));
            for (int x = 0; x <= model.getWidth(); x++)
                g2.drawLine(cx + (int)(x * scale), cy, cx + (int)(x * scale), cy + ch);
            for (int y = 0; y <= model.getHeight(); y++)
                g2.drawLine(cx, cy + (int)(y * scale), cx + cw, cy + (int)(y * scale));
        }

        // Canvas border
        g2.setColor(Color.DARK_GRAY);
        g2.drawRect(cx - 1, cy - 1, cw + 1, ch + 1);
    }

    @Override
    public Dimension getPreferredSize() {
        int w = (int)(model.getWidth()  * scale) + BORDER * 2;
        int h = (int)(model.getHeight() * scale) + BORDER * 2;
        return new Dimension(w, h);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void setScale(double newScale) {
        scale = newScale;
        rebuildOverlay();
        revalidate();
        repaint();
        if (zoomCallback != null) zoomCallback.accept(scale);
    }

    private void rebuildOverlay() {
        overlayImage = new BufferedImage(
            Math.max(1, model.getWidth()),
            Math.max(1, model.getHeight()),
            BufferedImage.TYPE_INT_ARGB);
    }

    private Point screenToCanvas(Point screen) {
        int cx = (int) translateX + BORDER;
        int cy = (int) translateY + BORDER;
        int x = (int) ((screen.x - cx) / scale);
        int y = (int) ((screen.y - cy) / scale);
        return new Point(x, y);
    }

    private Point canvasToScreen(Point canvas) {
        int cx = (int) translateX + BORDER;
        int cy = (int) translateY + BORDER;
        int x = (int) (canvas.x * scale) + cx;
        int y = (int) (canvas.y * scale) + cy;
        return new Point(x, y);
    }

    private void updateCursor(Point cp) {
        if (cursorCallback != null) cursorCallback.accept(cp.x, cp.y);
    }

    private void onModelChange(PropertyChangeEvent e) {
        if ("canvas".equals(e.getPropertyName())) {
            rebuildOverlay();
            revalidate();
            repaint();
        }
    }
}
