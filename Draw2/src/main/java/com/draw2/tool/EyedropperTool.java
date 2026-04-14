package com.draw2.tool;

import java.awt.*;
import java.awt.image.BufferedImage;

public class EyedropperTool implements Tool {

    private boolean rightClick;

    /** Called by CanvasPanel to tell the tool which button was pressed. */
    public void setRightClick(boolean b) { rightClick = b; }

    @Override
    public void onPress(Point p, DrawContext ctx) {
        pickColor(p, ctx);
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {
        pickColor(p, ctx);
    }

    @Override
    public void onRelease(Point p, DrawContext ctx) {}

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {}

    private void pickColor(Point p, DrawContext ctx) {
        BufferedImage canvas = ctx.model.getCanvas();
        int x = p.x, y = p.y;
        if (x < 0 || y < 0 || x >= canvas.getWidth() || y >= canvas.getHeight()) return;
        Color picked = new Color(canvas.getRGB(x, y));
        if (rightClick) {
            if (ctx.bgPickCallback != null) ctx.bgPickCallback.accept(picked);
        } else {
            if (ctx.fgPickCallback != null) ctx.fgPickCallback.accept(picked);
        }
    }

    @Override
    public Cursor getCursor() { return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR); }

    @Override
    public String getName() { return "滴管"; }
}
