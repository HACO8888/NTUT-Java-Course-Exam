package com.draw.command;

import com.draw.model.Layer;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Undo/redo for any stroke-based paint operation.
 * Snapshots the affected region before and after drawing.
 */
public class DrawStrokeCommand implements Command {
    private final Layer layer;
    private final BufferedImage beforeSnapshot;
    private final BufferedImage afterSnapshot;

    public DrawStrokeCommand(Layer layer, BufferedImage before, BufferedImage after) {
        this.layer = layer;
        this.beforeSnapshot = before;
        this.afterSnapshot = after;
    }

    @Override
    public void execute() {
        blit(afterSnapshot);
    }

    @Override
    public void undo() {
        blit(beforeSnapshot);
    }

    private void blit(BufferedImage src) {
        Graphics2D g2 = layer.getImage().createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2.fillRect(0, 0, layer.getImage().getWidth(), layer.getImage().getHeight());
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
    }

    /** Convenience: snapshot the entire layer image. */
    public static BufferedImage snapshot(Layer layer) {
        BufferedImage src = layer.getImage();
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = copy.createGraphics();
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return copy;
    }
}
