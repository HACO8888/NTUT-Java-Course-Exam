package com.draw2.tool;

import java.awt.*;
import java.awt.image.BufferedImage;

public class BrushTool implements Tool {

    private Point lastPoint;

    @Override
    public void onPress(Point p, DrawContext ctx) {
        ctx.model.pushUndo();
        lastPoint = p;
        paintDot(p, ctx);
        ctx.model.fireCanvasChanged();
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {
        if (lastPoint == null) return;
        Graphics2D g2 = ctx.model.getCanvas().createGraphics();
        g2.setColor(ctx.foreColor);
        float w = ctx.strokeWidth;
        g2.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
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

    private void paintDot(Point p, DrawContext ctx) {
        int r = ctx.strokeWidth / 2;
        Graphics2D g2 = ctx.model.getCanvas().createGraphics();
        g2.setColor(ctx.foreColor);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.fillOval(p.x - r, p.y - r, ctx.strokeWidth, ctx.strokeWidth);
        g2.dispose();
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public String getName() { return "筆刷"; }
}
