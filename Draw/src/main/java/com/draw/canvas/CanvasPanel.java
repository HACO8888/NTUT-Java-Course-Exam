package com.draw.canvas;

import com.draw.model.CanvasDocument;
import com.draw.tool.*;
import com.draw.tool.CropTool;
import com.draw.tool.RectSelectTool;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * The main drawing surface. Handles zoom/pan via AffineTransform and
 * delegates mouse events to the active Tool in canvas coordinates.
 *
 * Features:
 *  - Dynamic brush cursor ring showing actual brush size
 *  - Shift state propagated to shape tools for constraining
 *  - Alt+click for eyedropper (quick color pick)
 *  - Middle-mouse / Space+drag for panning
 */
public class CanvasPanel extends JPanel implements TextTool.TextToolListener {

    private CanvasDocument doc;
    private Tool activeTool;
    private DrawContext drawContext;

    private final CanvasRenderer renderer = new CanvasRenderer();
    private BufferedImage overlayImage;

    // View transform
    private double scale = 1.0;
    private double translateX = 40;
    private double translateY = 40;

    private boolean gridEnabled = false;
    private boolean rulersEnabled = true;
    private static final int GRID_SIZE = 32;
    private static final int RULER_SIZE = 20;

    // Pan tracking
    private Point panStart;
    private double panTX, panTY;
    private boolean spaceDown = false;
    private boolean shiftDown = false;

    // Brush cursor
    private Point cursorScreen;      // current mouse position in screen coords
    private boolean showBrushCursor = true;

    // Callbacks
    private StatusCallback statusCallback;
    private EyedropperTool eyedropperTool; // used for Alt+click
    private JPopupMenu rightClickMenu;
    private Runnable cropCallback;         // called when Enter pressed with CropTool

    private TexturePaint checkerPaint;

    public interface StatusCallback {
        void update(double zoom, int cursorX, int cursorY);
    }

    public CanvasPanel(CanvasDocument doc) {
        this.doc = doc;
        setLayout(null);
        setBackground(new Color(0x1e1e1e));
        overlayImage = new BufferedImage(doc.getWidth(), doc.getHeight(), BufferedImage.TYPE_INT_ARGB);

        initCheckerboard();
        fitCanvas();

        doc.getLayerModel().addPropertyChangeListener(e -> {
            renderer.markDirty();
            repaint();
        });

        setupMouseListeners();
        setupKeyListeners();
        setFocusable(true);

        // Marching ants / crop overlay animation timer (~15fps)
        new javax.swing.Timer(67, e -> {
            if (activeTool instanceof RectSelectTool || activeTool instanceof CropTool) repaint();
        }).start();
    }

    public void setDoc(CanvasDocument doc) {
        this.doc = doc;
        overlayImage = new BufferedImage(doc.getWidth(), doc.getHeight(), BufferedImage.TYPE_INT_ARGB);
        renderer.markDirty();
        fitCanvas();
        repaint();
    }

    public void setActiveTool(Tool tool) {
        if (activeTool instanceof TextTool) ((TextTool) activeTool).dismissField();
        this.activeTool = tool;
        // Reset shift state when switching tools to avoid stuck modifiers
        shiftDown = false;
        propagateShift(false);
        // For brush-like tools show the cursor ring; for others use the tool's cursor
        showBrushCursor = isBrushLike(tool);
        if (!showBrushCursor) setCursor(tool.getCursor());
        else setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        repaint();
    }

    private boolean isBrushLike(Tool t) {
        return t instanceof PencilTool || t instanceof BrushTool
                || t instanceof EraserTool || t instanceof SprayTool;
    }

    public void setDrawContext(DrawContext ctx) { this.drawContext = ctx; repaint(); }
    public void setStatusCallback(StatusCallback cb) { this.statusCallback = cb; }
    public void setEyedropperTool(EyedropperTool et) { this.eyedropperTool = et; }
    public void setRightClickMenu(JPopupMenu menu) { this.rightClickMenu = menu; }
    public void setCropCallback(Runnable cb) { this.cropCallback = cb; }
    public DrawContext getDrawContext() { return drawContext; }

    // ---- Selection operations (delegated to RectSelectTool if active) ----

    public void copySelection() {
        if (activeTool instanceof RectSelectTool && drawContext != null)
            ((RectSelectTool) activeTool).copy(drawContext);
    }
    public void cutSelection() {
        if (activeTool instanceof RectSelectTool && drawContext != null) {
            ((RectSelectTool) activeTool).cut(drawContext);
            clearOverlay(); repaint();
        }
    }
    public void pasteSelection() {
        if (activeTool instanceof RectSelectTool && drawContext != null) {
            ((RectSelectTool) activeTool).paste(drawContext);
            updateOverlay(); repaint();
        }
    }
    public void deleteSelection() {
        if (activeTool instanceof RectSelectTool && drawContext != null) {
            ((RectSelectTool) activeTool).deleteSelection(drawContext);
            clearOverlay(); repaint();
        }
    }
    public boolean hasSelection() {
        if (activeTool instanceof RectSelectTool) {
            RectSelectTool st = (RectSelectTool) activeTool;
            return st.getSelection() != null && !st.getSelection().isEmpty();
        }
        return false;
    }
    public void setGridEnabled(boolean v) { gridEnabled = v; repaint(); }
    public void setRulersEnabled(boolean v) { rulersEnabled = v; repaint(); }
    public boolean isGridEnabled() { return gridEnabled; }
    public boolean isRulersEnabled() { return rulersEnabled; }
    public double getScale() { return scale; }
    public void notifyShift(boolean down) {
        shiftDown = down;
        propagateShift(down);
    }

    private void propagateShift(boolean down) {
        if (activeTool instanceof LineTool) ((LineTool) activeTool).setShiftDown(down);
        if (activeTool instanceof RectangleTool) ((RectangleTool) activeTool).setShiftDown(down);
        if (activeTool instanceof EllipseTool) ((EllipseTool) activeTool).setShiftDown(down);
    }

    // ---- Zoom ----

    public void zoomIn() { applyZoom(1.2, getWidth() / 2.0, getHeight() / 2.0); }
    public void zoomOut() { applyZoom(1 / 1.2, getWidth() / 2.0, getHeight() / 2.0); }
    public void setZoomLevel(double z) {
        applyZoom(z / scale, getWidth() / 2.0, getHeight() / 2.0);
    }

    public void fitCanvas() {
        int panelW = getWidth() > 0 ? getWidth() : 900;
        int panelH = getHeight() > 0 ? getHeight() : 700;
        double sx = (double)(panelW - 80) / doc.getWidth();
        double sy = (double)(panelH - 80) / doc.getHeight();
        scale = Math.min(sx, sy);
        scale = Math.max(0.1, Math.min(32.0, scale));
        translateX = (panelW - doc.getWidth() * scale) / 2.0;
        translateY = (panelH - doc.getHeight() * scale) / 2.0;
        repaint();
    }

    private void applyZoom(double factor, double pivotX, double pivotY) {
        double newScale = Math.max(0.05, Math.min(32.0, scale * factor));
        double cx = (pivotX - translateX) / scale;
        double cy = (pivotY - translateY) / scale;
        scale = newScale;
        translateX = pivotX - cx * scale;
        translateY = pivotY - cy * scale;
        repaint();
    }

    // ---- Coordinate conversion ----

    private Point screenToCanvas(Point screen) {
        AffineTransform at = buildTransform();
        try {
            Point2D result = at.inverseTransform(new Point2D.Double(screen.x, screen.y), null);
            return new Point((int) result.getX(), (int) result.getY());
        } catch (NoninvertibleTransformException e) {
            return screen;
        }
    }

    private AffineTransform buildTransform() {
        AffineTransform at = new AffineTransform();
        at.translate(translateX, translateY);
        at.scale(scale, scale);
        return at;
    }

    // ---- Paint ----

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                scale >= 2.0 ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                             : RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2.setColor(new Color(0x1e1e1e));
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Canvas shadow / border (drawn first so canvas paints over it)
        Rectangle canvasRect = getCanvasScreenRect();
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRect(canvasRect.x + 5, canvasRect.y + 5, canvasRect.width, canvasRect.height);

        AffineTransform at = buildTransform();
        g2.transform(at);

        // Checkerboard
        g2.setPaint(checkerPaint);
        g2.fillRect(0, 0, doc.getWidth(), doc.getHeight());

        // Composite layers
        BufferedImage composite = renderer.getComposite(doc.getLayerModel(), doc.getWidth(), doc.getHeight());
        g2.drawImage(composite, 0, 0, null);

        // Overlay (tool previews)
        g2.drawImage(overlayImage, 0, 0, null);

        // Grid
        if (gridEnabled) drawGrid(g2);

        g2.dispose();

        // Canvas border
        Graphics2D g2b = (Graphics2D) g.create();
        g2b.setColor(new Color(0x555555));
        g2b.drawRect(canvasRect.x - 1, canvasRect.y - 1, canvasRect.width + 1, canvasRect.height + 1);
        g2b.dispose();

        // Rulers
        if (rulersEnabled) drawRulers(g);

        // Brush cursor ring (in screen space, after all canvas content)
        if (showBrushCursor && cursorScreen != null && drawContext != null) {
            drawBrushCursor(g);
        }
    }

    private void drawBrushCursor(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float brushRadius = drawContext.size / 2f;
        int screenRadius = Math.max(1, (int)(brushRadius * scale));

        int cx = cursorScreen.x, cy = cursorScreen.y;

        // Outer white ring
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(cx - screenRadius, cy - screenRadius, screenRadius * 2, screenRadius * 2);

        // Inner dark ring
        g2.setColor(new Color(0, 0, 0, 150));
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(cx - screenRadius - 1, cy - screenRadius - 1, screenRadius * 2 + 2, screenRadius * 2 + 2);

        // Center crosshair
        g2.setColor(new Color(255, 255, 255, 180));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(cx - 3, cy, cx + 3, cy);
        g2.drawLine(cx, cy - 3, cx, cy + 3);

        g2.dispose();
    }

    private void drawRulers(Graphics g) {
        int rz = RULER_SIZE;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));

        // Top ruler
        g2.setColor(new Color(0x2d2d2d));
        g2.fillRect(0, 0, getWidth(), rz);
        // Left ruler
        g2.fillRect(0, 0, rz, getHeight());
        // Corner square
        g2.setColor(new Color(0x3c3c3c));
        g2.fillRect(0, 0, rz, rz);

        g2.setColor(new Color(0x666666));
        FontMetrics fm = g2.getFontMetrics();

        // Determine tick spacing based on zoom
        int[] tickSteps = {1, 2, 5, 10, 25, 50, 100, 200, 500, 1000};
        int minPixelsBetweenTicks = 40;
        int step = 1;
        for (int s : tickSteps) {
            if (s * scale >= minPixelsBetweenTicks) { step = s; break; }
        }

        // Horizontal ticks
        int startX = (int)(-translateX / scale / step) * step;
        int endX = (int)((getWidth() - translateX) / scale) + step;
        for (int x = startX; x <= endX; x += step) {
            int sx = (int)(translateX + x * scale);
            g2.drawLine(sx, rz - 4, sx, rz);
            String label = String.valueOf(x);
            g2.drawString(label, sx + 2, rz - 4);
        }

        // Vertical ticks
        int startY = (int)(-translateY / scale / step) * step;
        int endY = (int)((getHeight() - translateY) / scale) + step;
        Graphics2D gv = (Graphics2D) g2.create();
        gv.rotate(-Math.PI / 2);
        for (int y = startY; y <= endY; y += step) {
            int sy = (int)(translateY + y * scale);
            g2.drawLine(rz - 4, sy, rz, sy);
            String label = String.valueOf(y);
            gv.drawString(label, -sy + 2, rz - 4);
        }
        gv.dispose();

        // Ruler border lines
        g2.setColor(new Color(0x444444));
        g2.drawLine(0, rz, getWidth(), rz);
        g2.drawLine(rz, 0, rz, getHeight());

        g2.dispose();
    }

    private Rectangle getCanvasScreenRect() {
        int sx = (int) translateX;
        int sy = (int) translateY;
        int sw = (int)(doc.getWidth() * scale);
        int sh = (int)(doc.getHeight() * scale);
        return new Rectangle(sx, sy, sw, sh);
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(new Color(200, 200, 200, 40));
        g2.setStroke(new BasicStroke(0.5f));
        int w = doc.getWidth(), h = doc.getHeight();
        for (int x = 0; x <= w; x += GRID_SIZE) g2.drawLine(x, 0, x, h);
        for (int y = 0; y <= h; y += GRID_SIZE) g2.drawLine(0, y, w, y);
    }

    private void initCheckerboard() {
        int s = 12;
        BufferedImage tile = new BufferedImage(s * 2, s * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tile.createGraphics();
        g.setColor(new Color(0xCCCCCC));
        g.fillRect(0, 0, s * 2, s * 2);
        g.setColor(new Color(0xFFFFFF));
        g.fillRect(0, 0, s, s);
        g.fillRect(s, s, s, s);
        g.dispose();
        checkerPaint = new TexturePaint(tile, new Rectangle(0, 0, s * 2, s * 2));
    }

    // ---- Mouse listeners ----

    private void setupMouseListeners() {
        MouseAdapter ma = new MouseAdapter() {
            private boolean panning = false;

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (e.getButton() == MouseEvent.BUTTON2 || spaceDown) {
                    panning = true;
                    panStart = e.getPoint();
                    panTX = translateX;
                    panTY = translateY;
                    return;
                }
                // Alt+click: eyedropper quick pick
                if (e.getButton() == MouseEvent.BUTTON1 && e.isAltDown() && eyedropperTool != null
                        && drawContext != null) {
                    Point cp = screenToCanvas(e.getPoint());
                    eyedropperTool.onPress(cp, drawContext);
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON3 && rightClickMenu != null) {
                    rightClickMenu.show(CanvasPanel.this, e.getX(), e.getY());
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON1 && activeTool != null && drawContext != null) {
                    Point cp = screenToCanvas(e.getPoint());
                    drawContext.clickCount = e.getClickCount();
                    activeTool.onPress(cp, drawContext);
                    updateOverlay();
                    repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                cursorScreen = e.getPoint();
                if (panning) {
                    translateX = panTX + (e.getX() - panStart.x);
                    translateY = panTY + (e.getY() - panStart.y);
                    repaint();
                    return;
                }
                if (activeTool != null && drawContext != null
                        && (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0
                        && !e.isAltDown()) {
                    Point cp = screenToCanvas(e.getPoint());
                    activeTool.onDrag(cp, drawContext);
                    updateOverlay();
                    repaint();
                }
                updateStatus(e.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (panning) { panning = false; return; }
                if (e.getButton() == MouseEvent.BUTTON1 && activeTool != null && drawContext != null
                        && !e.isAltDown()) {
                    Point cp = screenToCanvas(e.getPoint());
                    activeTool.onRelease(cp, drawContext);
                    updateOverlay();
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                cursorScreen = e.getPoint();
                updateStatus(e.getPoint());
                if (showBrushCursor) repaint(); // redraw for cursor ring
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cursorScreen = null;
                if (showBrushCursor) repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown()) {
                    // Ctrl+wheel = zoom toward cursor
                    double factor = e.getPreciseWheelRotation() < 0 ? 1.12 : 1.0 / 1.12;
                    applyZoom(factor, e.getX(), e.getY());
                } else {
                    // Plain wheel = scroll vertically (shift = horizontal)
                    JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, CanvasPanel.this);
                    if (sp != null) {
                        JScrollBar bar = e.isShiftDown() ? sp.getHorizontalScrollBar() : sp.getVerticalScrollBar();
                        int amount = (int)(e.getPreciseWheelRotation() * bar.getUnitIncrement() * 3);
                        bar.setValue(bar.getValue() + amount);
                    } else {
                        // No scroll pane: fall back to zoom
                        double factor = e.getPreciseWheelRotation() < 0 ? 1.12 : 1.0 / 1.12;
                        applyZoom(factor, e.getX(), e.getY());
                    }
                }
                updateStatus(e.getPoint());
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);
    }

    private void setupKeyListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) spaceDown = true;
                if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                    shiftDown = true;
                    propagateShift(true);
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && activeTool != null && drawContext != null) {
                    activeTool.cancel(drawContext);
                    clearOverlay();
                    repaint();
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER && activeTool instanceof CropTool
                        && cropCallback != null) {
                    cropCallback.run();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) spaceDown = false;
                if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                    shiftDown = false;
                    propagateShift(false);
                }
            }
        });
    }

    private void updateOverlay() {
        if (activeTool != null && drawContext != null && overlayImage != null) {
            activeTool.onPaint(overlayImage, drawContext);
        }
    }

    /** Called externally when overlay should be refreshed (e.g. after Select All). */
    public void updateOverlayPublic() { updateOverlay(); }

    public void clearOverlay() {
        if (overlayImage == null) return;
        Graphics2D g2 = overlayImage.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2.fillRect(0, 0, overlayImage.getWidth(), overlayImage.getHeight());
        g2.dispose();
    }

    private void updateStatus(Point screenPt) {
        if (statusCallback == null) return;
        Point cp = screenToCanvas(screenPt);
        statusCallback.update(scale, cp.x, cp.y);
    }

    // TextTool.TextToolListener implementation
    @Override
    public void showTextField(Point canvasPoint, DrawContext ctx, TextTool tool) {
        AffineTransform at = buildTransform();
        Point2D screenPt = at.transform(new Point2D.Double(canvasPoint.x, canvasPoint.y), null);

        int fontSize = Math.max(8, (int)(ctx.size * 3 * scale));
        JTextField field = new JTextField(20);
        field.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        field.setBackground(new Color(0x3c3c3c));
        field.setForeground(ctx.color);
        field.setCaretColor(ctx.color);
        field.setBorder(BorderFactory.createLineBorder(ctx.color, 1));
        field.setOpaque(true);

        int fw = 200, fh = fontSize + 10;
        field.setBounds((int)screenPt.getX(), (int)screenPt.getY() - fh, fw, fh);
        add(field);
        revalidate();
        field.requestFocusInWindow();
        tool.setTextField(field);

        field.addActionListener(ae -> { tool.commit(ctx); clearOverlay(); repaint(); });
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { tool.commit(ctx); clearOverlay(); repaint(); }
        });
        field.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { tool.cancel(ctx); clearOverlay(); repaint(); }
            }
        });
    }
}
