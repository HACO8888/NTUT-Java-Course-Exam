package com.draw2.tool;

import java.awt.*;
import java.awt.image.BufferedImage;

public class EraserTool implements Tool {

    private Point lastPoint;

    @Override
    public void onPress(Point p, DrawContext ctx) {
        ctx.model.pushUndo();
        lastPoint = p;
        erase(p, ctx);
        ctx.model.fireCanvasChanged();
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {
        if (lastPoint == null) return;
        // Draw thick line between points to avoid gaps
        Graphics2D g2 = ctx.model.getCanvas().createGraphics();
        g2.setColor(ctx.backColor);
        int size = ctx.strokeWidth * 2;
        g2.setStroke(new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(lastPoint.x, lastPoint.y, p.x, p.y);
        g2.dispose();
        lastPoint = p;
        ctx.model.fireCanvasChanged();
    }

    @Override
    public void onRelease(Point p, DrawContext ctx) {
        lastPoint = null;
    }

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {}

    private void erase(Point p, DrawContext ctx) {
        int size = ctx.strokeWidth * 2;
        int r = size / 2;
        Graphics2D g2 = ctx.model.getCanvas().createGraphics();
        g2.setColor(ctx.backColor);
        g2.fillOval(p.x - r, p.y - r, size, size);
        g2.dispose();
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public String getName() { return "橡皮擦"; }
}
