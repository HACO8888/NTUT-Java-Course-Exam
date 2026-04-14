package com.draw2.tool;

import java.awt.*;
import java.awt.image.BufferedImage;

public interface Tool {
    void onPress(Point p, DrawContext ctx);
    void onDrag(Point p, DrawContext ctx);
    void onRelease(Point p, DrawContext ctx);

    /** Draw a live preview onto the overlay image (transparent ARGB). */
    void onPaint(BufferedImage overlay, DrawContext ctx);

    default void onActivate()   {}
    default void onDeactivate() {}
    default void cancel()       {}

    Cursor getCursor();
    String getName();
}
