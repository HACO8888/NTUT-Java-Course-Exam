package com.draw2.tool;

import com.draw2.model.CanvasModel;

import java.awt.*;
import java.util.function.Consumer;

public final class DrawContext {

    public final CanvasModel model;
    public final Color foreColor;
    public final Color backColor;
    public final int strokeWidth;
    public final boolean filled;
    public final Consumer<Color> fgPickCallback;
    public final Consumer<Color> bgPickCallback;

    public DrawContext(CanvasModel model,
                       Color foreColor,
                       Color backColor,
                       int strokeWidth,
                       boolean filled,
                       Consumer<Color> fgPickCallback,
                       Consumer<Color> bgPickCallback) {
        this.model = model;
        this.foreColor = foreColor;
        this.backColor = backColor;
        this.strokeWidth = strokeWidth;
        this.filled = filled;
        this.fgPickCallback = fgPickCallback;
        this.bgPickCallback = bgPickCallback;
    }

    /** Returns a copy with fore and back swapped (for right-click drawing). */
    public DrawContext withSwappedColors() {
        return new DrawContext(model, backColor, foreColor, strokeWidth, filled, fgPickCallback, bgPickCallback);
    }
}
