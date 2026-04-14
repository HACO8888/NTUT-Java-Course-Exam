package com.draw.tool;

import com.draw.model.CanvasDocument;

import java.awt.*;

/**
 * Immutable value object passed from the UI to every tool on each mouse event.
 */
public class DrawContext {
    public final CanvasDocument doc;
    public final Color color;
    public final float size;
    public final float opacity;
    public final float hardness;
    /** Click count from the mouse event (1 = single, 2 = double). */
    public int clickCount = 1;

    /** Secondary (background) color — used as gradient endpoint, etc. */
    public Color secondaryColor = Color.WHITE;

    public DrawContext(CanvasDocument doc, Color color, float size, float opacity, float hardness) {
        this.doc = doc;
        this.color = color;
        this.size = size;
        this.opacity = opacity;
        this.hardness = hardness;
    }
}
