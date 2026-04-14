package com.draw.tool;

import com.draw.command.DrawStrokeCommand;
import com.draw.model.Layer;

import java.awt.*;
import java.awt.image.BufferedImage;

public class EllipseTool implements Tool {
    private Point startPoint;
    private Point currentPoint;
    private BufferedImage beforeSnapshot;
    private boolean filled = false;
    private boolean shiftDown = false;

    public void setFilled(boolean filled) { this.filled = filled; }
    public void setShiftDown(boolean v) { this.shiftDown = v; }

    /** If shift is held, constrain to a circle. */
    private Rectangle makeRect(Point a, Point b) {
        int dx = b.x - a.x;
        int dy = b.y - a.y;
        if (shiftDown) {
            int side = Math.min(Math.abs(dx), Math.abs(dy));
            dx = (dx < 0) ? -side : side;
            dy = (dy < 0) ? -side : side;
        }
        return new Rectangle(
            Math.min(a.x, a.x + dx), Math.min(a.y, a.y + dy),
            Math.abs(dx), Math.abs(dy));
    }

    @Override
    public void onPress(Point p, DrawContext ctx) {
        startPoint = p; currentPoint = p;
        Layer layer = ctx.doc.getLayerModel().getActive();
        if (layer != null) beforeSnapshot = DrawStrokeCommand.snapshot(layer);
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) { currentPoint = p; }

    @Override
    public void onRelease(Point p, DrawContext ctx) {
        Layer layer = ctx.doc.getLayerModel().getActive();
        if (layer == null || startPoint == null) return;

        Rectangle rect = makeRect(startPoint, p);
        if (rect.width < 1 || rect.height < 1) { startPoint = null; return; }

        Graphics2D g2 = layer.getImage().createGraphics();
        setHints(g2);
        g2.setColor(ctx.color);
        g2.setStroke(new BasicStroke(ctx.size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ctx.opacity));
        if (filled) g2.fillOval(rect.x, rect.y, rect.width, rect.height);
        else g2.drawOval(rect.x, rect.y, rect.width, rect.height);
        g2.dispose();

        BufferedImage after = DrawStrokeCommand.snapshot(layer);
        ctx.doc.getHistory().push(new DrawStrokeCommand(layer, beforeSnapshot, after));
        ctx.doc.getLayerModel().fireLayersChanged();
        ctx.doc.markDirty();
        startPoint = null; currentPoint = null; beforeSnapshot = null;
    }

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {
        if (startPoint == null || currentPoint == null) return;
        Graphics2D g2 = overlay.createGraphics();
        clearOverlay(g2, overlay);
        setHints(g2);
        Rectangle rect = makeRect(startPoint, currentPoint);
        g2.setColor(ctx.color);
        g2.setStroke(new BasicStroke(ctx.size));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ctx.opacity * 0.8f));
        if (rect.width > 0 && rect.height > 0) {
            if (filled) g2.fillOval(rect.x, rect.y, rect.width, rect.height);
            else g2.drawOval(rect.x, rect.y, rect.width, rect.height);
        }
        // Dimension label
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.drawString(rect.width + " × " + rect.height, currentPoint.x + 6, currentPoint.y - 4);
        g2.dispose();
    }

    @Override
    public void cancel(DrawContext ctx) { startPoint = null; currentPoint = null; }

    @Override
    public Cursor getCursor() { return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); }

    @Override
    public String getName() { return "Ellipse"; }

    private void setHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private void clearOverlay(Graphics2D g2, BufferedImage overlay) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2.fillRect(0, 0, overlay.getWidth(), overlay.getHeight());
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
    }
}
