package com.draw.tool;

import com.draw.command.DrawStrokeCommand;
import com.draw.model.Layer;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;

/**
 * Rectangle selection tool.
 * - Drag to create selection (marching ants).
 * - Drag inside existing selection to move its content.
 * - Copy/Cut/Paste/Delete operate on the selection region.
 */
public class RectSelectTool implements Tool {

    public enum State { IDLE, SELECTING, SELECTED, MOVING }

    private State state = State.IDLE;
    private Rectangle selection;
    private Point pressPoint;

    // For moving selection content
    private BufferedImage floatingImage;
    private Point floatingOffset;
    private BufferedImage beforeSnapshot;
    private Layer activeLayer;

    // Marching ants animation offset
    private int dashOffset = 0;

    public State getState() { return state; }
    public Rectangle getSelection() { return selection; }

    // ---- Clipboard operations (called from MainWindow) ----

    public void copy(DrawContext ctx) {
        if (selection == null || selection.isEmpty()) return;
        Layer layer = ctx.doc.getLayerModel().getActive();
        if (layer == null) return;
        setClipboard(crop(layer.getImage(), selection));
    }

    public void cut(DrawContext ctx) {
        if (selection == null || selection.isEmpty()) return;
        Layer layer = ctx.doc.getLayerModel().getActive();
        if (layer == null) return;
        BufferedImage before = DrawStrokeCommand.snapshot(layer);
        setClipboard(crop(layer.getImage(), selection));
        clearArea(layer, selection);
        ctx.doc.getHistory().push(new DrawStrokeCommand(layer,
                before, DrawStrokeCommand.snapshot(layer)));
        ctx.doc.getLayerModel().fireLayersChanged();
        ctx.doc.markDirty();
        selection = null;
        state = State.IDLE;
    }

    public void paste(DrawContext ctx) {
        BufferedImage img = getClipboard();
        if (img == null) return;
        Layer layer = ctx.doc.getLayerModel().getActive();
        if (layer == null) return;
        BufferedImage before = DrawStrokeCommand.snapshot(layer);
        Graphics2D g2 = layer.getImage().createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        ctx.doc.getHistory().push(new DrawStrokeCommand(layer,
                before, DrawStrokeCommand.snapshot(layer)));
        ctx.doc.getLayerModel().fireLayersChanged();
        ctx.doc.markDirty();
        selection = new Rectangle(0, 0, img.getWidth(), img.getHeight());
        state = State.SELECTED;
    }

    public void deleteSelection(DrawContext ctx) {
        if (selection == null || selection.isEmpty()) return;
        Layer layer = ctx.doc.getLayerModel().getActive();
        if (layer == null) return;
        BufferedImage before = DrawStrokeCommand.snapshot(layer);
        clearArea(layer, selection);
        ctx.doc.getHistory().push(new DrawStrokeCommand(layer,
                before, DrawStrokeCommand.snapshot(layer)));
        ctx.doc.getLayerModel().fireLayersChanged();
        ctx.doc.markDirty();
        selection = null;
        state = State.IDLE;
    }

    public void deselect() {
        selection = null;
        state = State.IDLE;
        floatingImage = null;
    }

    // ---- Tool interface ----

    @Override
    public void onPress(Point p, DrawContext ctx) {
        if (state == State.SELECTED && selection != null && selection.contains(p)) {
            // Start moving the selection content
            state = State.MOVING;
            activeLayer = ctx.doc.getLayerModel().getActive();
            if (activeLayer == null) { state = State.SELECTED; return; }
            beforeSnapshot = DrawStrokeCommand.snapshot(activeLayer);
            floatingImage = crop(activeLayer.getImage(), selection);
            clearArea(activeLayer, selection);
            floatingOffset = new Point(p.x - selection.x, p.y - selection.y);
            ctx.doc.getLayerModel().fireLayersChanged();
        } else {
            // Start new selection
            stampFloating(ctx); // commit any existing float
            state = State.SELECTING;
            pressPoint = p;
            selection = new Rectangle(p.x, p.y, 0, 0);
        }
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {
        if (state == State.SELECTING && pressPoint != null) {
            int x = Math.min(pressPoint.x, p.x);
            int y = Math.min(pressPoint.y, p.y);
            int w = Math.abs(p.x - pressPoint.x);
            int h = Math.abs(p.y - pressPoint.y);
            selection = new Rectangle(x, y, w, h);
        } else if (state == State.MOVING && floatingImage != null) {
            int nx = p.x - floatingOffset.x;
            int ny = p.y - floatingOffset.y;
            selection = new Rectangle(nx, ny,
                    floatingImage.getWidth(), floatingImage.getHeight());
        }
    }

    @Override
    public void onRelease(Point p, DrawContext ctx) {
        if (state == State.SELECTING) {
            if (selection == null || (selection.width < 2 && selection.height < 2)) {
                selection = null;
                state = State.IDLE;
            } else {
                state = State.SELECTED;
            }
        } else if (state == State.MOVING) {
            stampFloating(ctx);
        }
    }

    @Override
    public void cancel(DrawContext ctx) {
        if (state == State.MOVING && activeLayer != null && beforeSnapshot != null) {
            activeLayer.restoreFrom(new Layer("snap", beforeSnapshot));
            ctx.doc.getLayerModel().fireLayersChanged();
        }
        selection = null;
        floatingImage = null;
        beforeSnapshot = null;
        activeLayer = null;
        state = State.IDLE;
    }

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {
        Graphics2D g2 = overlay.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2.fillRect(0, 0, overlay.getWidth(), overlay.getHeight());
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        if (selection != null && selection.width > 0 && selection.height > 0) {
            // Draw floating content
            if (state == State.MOVING && floatingImage != null) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
                g2.drawImage(floatingImage, selection.x, selection.y, null);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            }

            // Marching ants: two complementary dashed strokes
            dashOffset = (dashOffset + 1) % 20;
            float[] dash = {6f, 4f};
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    1f, dash, dashOffset));
            g2.setColor(Color.WHITE);
            g2.drawRect(selection.x, selection.y, selection.width, selection.height);

            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    1f, dash, dashOffset + 5f));
            g2.setColor(Color.BLACK);
            g2.drawRect(selection.x, selection.y, selection.width, selection.height);
        }
        g2.dispose();
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public String getName() { return "Select"; }

    // ---- Private helpers ----

    private void stampFloating(DrawContext ctx) {
        if (floatingImage != null && activeLayer != null && selection != null) {
            Graphics2D g2 = activeLayer.getImage().createGraphics();
            g2.drawImage(floatingImage, selection.x, selection.y, null);
            g2.dispose();
            if (beforeSnapshot != null) {
                ctx.doc.getHistory().push(new DrawStrokeCommand(activeLayer,
                        beforeSnapshot, DrawStrokeCommand.snapshot(activeLayer)));
            }
            ctx.doc.getLayerModel().fireLayersChanged();
            ctx.doc.markDirty();
        }
        floatingImage = null;
        beforeSnapshot = null;
        activeLayer = null;
        state = State.SELECTED;
    }

    private BufferedImage crop(BufferedImage src, Rectangle r) {
        int x = Math.max(0, r.x);
        int y = Math.max(0, r.y);
        int w = Math.min(r.width,  src.getWidth()  - x);
        int h = Math.min(r.height, src.getHeight() - y);
        if (w <= 0 || h <= 0) return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.drawImage(src.getSubimage(x, y, w, h), 0, 0, null);
        g2.dispose();
        return out;
    }

    private void clearArea(Layer layer, Rectangle r) {
        Graphics2D g2 = layer.getImage().createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2.fillRect(r.x, r.y, r.width, r.height);
        g2.dispose();
    }

    private static void setClipboard(BufferedImage img) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new ImageSelection(img), null);
    }

    private static BufferedImage getClipboard() {
        try {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (cb.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
                Image raw = (Image) cb.getData(DataFlavor.imageFlavor);
                if (raw instanceof BufferedImage) return (BufferedImage) raw;
                int w = raw.getWidth(null), h = raw.getHeight(null);
                if (w <= 0 || h <= 0) return null;
                BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = bi.createGraphics();
                g2.drawImage(raw, 0, 0, null);
                g2.dispose();
                return bi;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static class ImageSelection implements Transferable {
        private final BufferedImage img;
        ImageSelection(BufferedImage img) { this.img = img; }

        @Override public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }
        @Override public boolean isDataFlavorSupported(DataFlavor f) {
            return DataFlavor.imageFlavor.equals(f);
        }
        @Override public Object getTransferData(DataFlavor f) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(f)) throw new UnsupportedFlavorException(f);
            return img;
        }
    }
}
