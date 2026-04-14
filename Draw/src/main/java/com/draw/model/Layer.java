package com.draw.model;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.UUID;

public class Layer {
    /** Blend mode names matching our custom compositing. */
    public enum BlendMode {
        NORMAL("Normal"),
        MULTIPLY("Multiply"),
        SCREEN("Screen"),
        OVERLAY("Overlay"),
        HARD_LIGHT("Hard Light"),
        SOFT_LIGHT("Soft Light"),
        DARKEN("Darken"),
        LIGHTEN("Lighten"),
        DIFFERENCE("Difference"),
        EXCLUSION("Exclusion"),
        COLOR_DODGE("Color Dodge"),
        COLOR_BURN("Color Burn");

        public final String display;
        BlendMode(String d) { this.display = d; }

        @Override public String toString() { return display; }
    }

    private final UUID id;
    private String name;
    private BufferedImage image;
    private boolean visible;
    private float opacity;
    private BlendMode blendMode;

    public Layer(String name, int width, int height) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.visible = true;
        this.opacity = 1.0f;
        this.blendMode = BlendMode.NORMAL;
    }

    public Layer(String name, BufferedImage image) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.image = image;
        this.visible = true;
        this.opacity = 1.0f;
        this.blendMode = BlendMode.NORMAL;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BufferedImage getImage() { return image; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public float getOpacity() { return opacity; }
    public void setOpacity(float opacity) { this.opacity = Math.max(0f, Math.min(1f, opacity)); }
    public BlendMode getBlendMode() { return blendMode; }
    public void setBlendMode(BlendMode m) { this.blendMode = m; }

    public void resize(int width, int height) {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = newImage.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        this.image = newImage;
    }

    /** Crop the layer to the given rectangle (offset into the existing image). */
    public void crop(int x, int y, int w, int h) {
        int srcX = Math.max(0, x);
        int srcY = Math.max(0, y);
        int srcW = Math.min(w, image.getWidth()  - srcX);
        int srcH = Math.min(h, image.getHeight() - srcY);
        BufferedImage cropped = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        if (srcW > 0 && srcH > 0) {
            Graphics2D g2 = cropped.createGraphics();
            g2.drawImage(image.getSubimage(srcX, srcY, srcW, srcH),
                    srcX - x, srcY - y, null);
            g2.dispose();
        }
        this.image = cropped;
    }

    public Layer snapshot() {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = copy.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        Layer snap = new Layer(name, copy);
        snap.visible = this.visible;
        snap.opacity = this.opacity;
        snap.blendMode = this.blendMode;
        return snap;
    }

    public void restoreFrom(Layer snapshot) {
        Graphics2D g2 = image.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        g2.drawImage(snapshot.getImage(), 0, 0, null);
        g2.dispose();
    }
}
