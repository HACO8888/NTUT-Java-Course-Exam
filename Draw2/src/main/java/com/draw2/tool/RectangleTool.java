package com.draw2.tool;

import java.awt.*;
import java.awt.image.BufferedImage;

public class RectangleTool implements Tool {

    private Point start;
    private Point current;
    private boolean shiftDown;

    public void setShiftDown(boolean b) { shiftDown = b; }

    @Override
    public void onPress(Point p, DrawContext ctx) {
        ctx.model.pushUndo();
        start = p;
        current = p;
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {
        current = p;
    }

    @Override
    public void onRelease(Point p, DrawContext ctx) {
        if (start == null) return;
        current = p;
        Rectangle r = makeRect(start, current);
        Graphics2D g2 = ctx.model.getCanvas().createGraphics();
        applyHints(g2, ctx);
        if (ctx.filled) {
            g2.fillRect(r.x, r.y, r.width, r.height);
        } else {
            g2.drawRect(r.x, r.y, r.width, r.height);
        }
        g2.dispose();
        ctx.model.fireCanvasChanged();
        start = null;
        current = null;
    }

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {
        if (start == null || current == null) return;
        Rectangle r = makeRect(start, current);
        Graphics2D g2 = overlay.createGraphics();
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, overlay.getWidth(), overlay.getHeight());
        g2.setComposite(AlphaComposite.SrcOver);
        applyHints(g2, ctx);
        if (ctx.filled) {
            g2.fillRect(r.x, r.y, r.width, r.height);
        } else {
            g2.drawRect(r.x, r.y, r.width, r.height);
        }
        g2.dispose();
    }

    @Override
    public void cancel() { start = null; current = null; }

    private Rectangle makeRect(Point a, Point b) {
        int x = Math.min(a.x, b.x), y = Math.min(a.y, b.y);
        int w = Math.abs(b.x - a.x), h = Math.abs(b.y - a.y);
        if (shiftDown) { int s = Math.max(w, h); w = s; h = s; }
        return new Rectangle(x, y, Math.max(1, w), Math.max(1, h));
    }

    private void applyHints(Graphics2D g2, DrawContext ctx) {
        g2.setColor(ctx.foreColor);
        g2.setStroke(new BasicStroke(ctx.strokeWidth));
    }

    @Override
    public Cursor getCursor() { return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); }

    @Override
    public String getName() { return "矩形"; }
}
