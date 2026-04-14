package com.draw2.tool;

import java.awt.*;
import java.awt.image.BufferedImage;

public class LineTool implements Tool {

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
        current = constrain(start, p);
    }

    @Override
    public void onRelease(Point p, DrawContext ctx) {
        if (start == null) return;
        current = constrain(start, p);
        Graphics2D g2 = ctx.model.getCanvas().createGraphics();
        applyStroke(g2, ctx);
        g2.drawLine(start.x, start.y, current.x, current.y);
        g2.dispose();
        ctx.model.fireCanvasChanged();
        start = null;
        current = null;
    }

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {
        if (start == null || current == null) return;
        Graphics2D g2 = overlay.createGraphics();
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, overlay.getWidth(), overlay.getHeight());
        g2.setComposite(AlphaComposite.SrcOver);
        applyStroke(g2, ctx);
        g2.drawLine(start.x, start.y, current.x, current.y);
        g2.dispose();
    }

    @Override
    public void cancel() {
        start = null;
        current = null;
    }

    private Point constrain(Point s, Point p) {
        if (!shiftDown) return p;
        int dx = p.x - s.x, dy = p.y - s.y;
        if (Math.abs(dx) > Math.abs(dy) * 2) return new Point(p.x, s.y);       // horizontal
        if (Math.abs(dy) > Math.abs(dx) * 2) return new Point(s.x, p.y);       // vertical
        int d = (Math.abs(dx) + Math.abs(dy)) / 2;                              // 45°
        return new Point(s.x + (dx > 0 ? d : -d), s.y + (dy > 0 ? d : -d));
    }

    private void applyStroke(Graphics2D g2, DrawContext ctx) {
        g2.setColor(ctx.foreColor);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(ctx.strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    @Override
    public Cursor getCursor() { return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); }

    @Override
    public String getName() { return "直線"; }
}
