package com.draw.tool;

import com.draw.command.DrawStrokeCommand;
import com.draw.model.Layer;

import java.awt.*;
import java.awt.image.BufferedImage;

public class LineTool implements Tool {
    private Point startPoint;
    private Point currentPoint;
    private BufferedImage beforeSnapshot;
    private boolean shiftDown = false;

    public void setShiftDown(boolean v) { this.shiftDown = v; }

    /** Snap endpoint to nearest 45° angle from startPoint. */
    private Point constrain(Point end) {
        if (startPoint == null) return end;
        int dx = end.x - startPoint.x;
        int dy = end.y - startPoint.y;
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        double len = Math.sqrt(dx * dx + dy * dy);
        // Snap to nearest 45°
        double snapped = Math.round(angle / 45.0) * 45.0;
        double rad = Math.toRadians(snapped);
        return new Point(startPoint.x + (int)(Math.cos(rad) * len),
                         startPoint.y + (int)(Math.sin(rad) * len));
    }

    @Override
    public void onPress(Point p, DrawContext ctx) {
        startPoint = p;
        currentPoint = p;
        Layer layer = ctx.doc.getLayerModel().getActive();
        if (layer != null) beforeSnapshot = DrawStrokeCommand.snapshot(layer);
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {
        currentPoint = shiftDown ? constrain(p) : p;
    }

    @Override
    public void onRelease(Point p, DrawContext ctx) {
        Layer layer = ctx.doc.getLayerModel().getActive();
        if (layer == null || startPoint == null) return;

        Point end = shiftDown ? constrain(p) : p;
        Graphics2D g2 = layer.getImage().createGraphics();
        setHints(g2);
        g2.setColor(ctx.color);
        g2.setStroke(new BasicStroke(ctx.size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ctx.opacity));
        g2.drawLine(startPoint.x, startPoint.y, end.x, end.y);
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
        g2.setColor(ctx.color);
        g2.setStroke(new BasicStroke(ctx.size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ctx.opacity * 0.8f));
        g2.drawLine(startPoint.x, startPoint.y, currentPoint.x, currentPoint.y);
        g2.dispose();
    }

    @Override
    public void cancel(DrawContext ctx) {
        startPoint = null; currentPoint = null; beforeSnapshot = null;
    }

    @Override
    public Cursor getCursor() { return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); }

    @Override
    public String getName() { return "Line"; }

    private void setHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private void clearOverlay(Graphics2D g2, BufferedImage overlay) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2.fillRect(0, 0, overlay.getWidth(), overlay.getHeight());
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
    }
}
