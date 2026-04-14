package com.draw2.tool;

import java.awt.*;
import java.awt.image.BufferedImage;

public class PencilTool implements Tool {

    private Point lastPoint;

    @Override
    public void onPress(Point p, DrawContext ctx) {
        ctx.model.pushUndo();
        lastPoint = p;
        Graphics2D g2 = ctx.model.getCanvas().createGraphics();
        g2.setColor(ctx.foreColor);
        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.fillRect(p.x, p.y, 1, 1);
        g2.dispose();
        ctx.model.fireCanvasChanged();
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {
        if (lastPoint == null) return;
        Graphics2D g2 = ctx.model.getCanvas().createGraphics();
        g2.setColor(ctx.foreColor);
        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
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

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public String getName() { return "鉛筆"; }
}
