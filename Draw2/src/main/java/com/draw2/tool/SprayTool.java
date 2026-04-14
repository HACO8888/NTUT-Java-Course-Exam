package com.draw2.tool;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class SprayTool implements Tool {

    private static final int DOTS_PER_EVENT = 12;
    private final Random rng = new Random();

    @Override
    public void onPress(Point p, DrawContext ctx) {
        ctx.model.pushUndo();
        spray(p, ctx);
        ctx.model.fireCanvasChanged();
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {
        spray(p, ctx);
        ctx.model.fireCanvasChanged();
    }

    @Override
    public void onRelease(Point p, DrawContext ctx) {}

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {}

    private void spray(Point p, DrawContext ctx) {
        int radius = ctx.strokeWidth * 4;
        Graphics2D g2 = ctx.model.getCanvas().createGraphics();
        g2.setColor(ctx.foreColor);
        for (int i = 0; i < DOTS_PER_EVENT; i++) {
            double angle = rng.nextDouble() * 2 * Math.PI;
            double dist  = rng.nextDouble() * radius;
            int dx = (int) (Math.cos(angle) * dist);
            int dy = (int) (Math.sin(angle) * dist);
            g2.fillRect(p.x + dx, p.y + dy, 1, 1);
        }
        g2.dispose();
    }

    @Override
    public Cursor getCursor() { return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); }

    @Override
    public String getName() { return "噴漆"; }
}
