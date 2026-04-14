package com.draw.tool;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Contract for all drawing tools.
 */
public interface Tool {
    /** Called when mouse button is pressed on the canvas (canvas coordinates). */
    void onPress(Point p, DrawContext ctx);

    /** Called while mouse is dragged (canvas coordinates). */
    void onDrag(Point p, DrawContext ctx);

    /** Called when mouse button is released (canvas coordinates). */
    void onRelease(Point p, DrawContext ctx);

    /**
     * Called every repaint to draw live preview onto the overlay image.
     * Implementations should clear the overlay themselves if needed.
     */
    void onPaint(BufferedImage overlay, DrawContext ctx);

    /** Cancel any in-progress operation (e.g. ESC pressed). */
    default void cancel(DrawContext ctx) {}

    Cursor getCursor();
    String getName();
}
