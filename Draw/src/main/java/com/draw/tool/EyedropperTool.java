package com.draw.tool;

import com.draw.util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Picks the color from the composited canvas at the clicked pixel.
 */
public class EyedropperTool implements Tool {
    private Consumer<Color> colorCallback;

    public EyedropperTool(Consumer<Color> colorCallback) {
        this.colorCallback = colorCallback;
    }

    @Override
    public void onPress(Point p, DrawContext ctx) {
        BufferedImage composite = ImageUtil.composite(
                ctx.doc.getLayerModel(), ctx.doc.getWidth(), ctx.doc.getHeight());
        int x = Math.max(0, Math.min(p.x, composite.getWidth() - 1));
        int y = Math.max(0, Math.min(p.y, composite.getHeight() - 1));
        Color picked = new Color(composite.getRGB(x, y), true);
        if (colorCallback != null) colorCallback.accept(picked);
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) { onPress(p, ctx); }

    @Override
    public void onRelease(Point p, DrawContext ctx) {}

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {}

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public String getName() { return "Eyedropper"; }
}
