package com.draw.canvas;

import com.draw.model.Layer;
import com.draw.model.LayerModel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Composites all visible layers into a single display image, respecting blend modes.
 */
public class CanvasRenderer {
    private BufferedImage cached;
    private boolean dirty = true;

    public void markDirty() { dirty = true; }

    public BufferedImage getComposite(LayerModel model, int w, int h) {
        if (dirty || cached == null || cached.getWidth() != w || cached.getHeight() != h) {
            cached = composite(model, w, h);
            dirty = false;
        }
        return cached;
    }

    private BufferedImage composite(LayerModel model, int w, int h) {
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        // Start with transparent
        Graphics2D g2 = result.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2.fillRect(0, 0, w, h);
        g2.dispose();

        List<Layer> layers = model.getLayers();
        for (Layer layer : layers) {
            if (!layer.isVisible()) continue;
            if (layer.getBlendMode() == Layer.BlendMode.NORMAL) {
                // Fast path: use Java's built-in alpha compositing
                Graphics2D lg = result.createGraphics();
                lg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, layer.getOpacity()));
                lg.drawImage(layer.getImage(), 0, 0, null);
                lg.dispose();
            } else {
                // Software blend mode compositing
                blendLayer(result, layer.getImage(), layer.getBlendMode(), layer.getOpacity());
            }
        }
        return result;
    }

    /**
     * Applies a blend mode pixel-by-pixel.
     * dst = result (modified in-place), src = layer image.
     */
    private void blendLayer(BufferedImage dst, BufferedImage src, Layer.BlendMode mode, float opacity) {
        int w = Math.min(dst.getWidth(), src.getWidth());
        int h = Math.min(dst.getHeight(), src.getHeight());

        int[] dstPixels = new int[w * h];
        int[] srcPixels = new int[w * h];
        dst.getRGB(0, 0, w, h, dstPixels, 0, w);
        src.getRGB(0, 0, w, h, srcPixels, 0, w);

        for (int i = 0; i < dstPixels.length; i++) {
            int sp = srcPixels[i];
            int dp = dstPixels[i];

            int sa = (sp >>> 24) & 0xFF;
            if (sa == 0) continue;

            float srcA = (sa / 255f) * opacity;
            float dstA = ((dp >>> 24) & 0xFF) / 255f;

            float sr = ((sp >> 16) & 0xFF) / 255f;
            float sg = ((sp >> 8)  & 0xFF) / 255f;
            float sb = ( sp        & 0xFF) / 255f;

            float dr = ((dp >> 16) & 0xFF) / 255f;
            float dg = ((dp >> 8)  & 0xFF) / 255f;
            float db = ( dp        & 0xFF) / 255f;

            float br = blend(sr, dr, mode);
            float bg = blend(sg, dg, mode);
            float bb = blend(sb, db, mode);

            // SRC_OVER alpha compositing after blend
            float outA = srcA + dstA * (1 - srcA);
            if (outA == 0) { dstPixels[i] = 0; continue; }

            float outR = (br * srcA + dr * dstA * (1 - srcA)) / outA;
            float outG = (bg * srcA + dg * dstA * (1 - srcA)) / outA;
            float outB = (bb * srcA + db * dstA * (1 - srcA)) / outA;

            dstPixels[i] = ((int)(outA * 255) << 24)
                    | ((int)(clamp(outR) * 255) << 16)
                    | ((int)(clamp(outG) * 255) << 8)
                    |  (int)(clamp(outB) * 255);
        }
        dst.setRGB(0, 0, w, h, dstPixels, 0, w);
    }

    private float blend(float src, float dst, Layer.BlendMode mode) {
        switch (mode) {
            case MULTIPLY:    return src * dst;
            case SCREEN:      return 1 - (1 - src) * (1 - dst);
            case OVERLAY:     return dst < 0.5f ? 2 * src * dst : 1 - 2 * (1 - src) * (1 - dst);
            case HARD_LIGHT:  return src < 0.5f ? 2 * src * dst : 1 - 2 * (1 - src) * (1 - dst);
            case SOFT_LIGHT:
                if (src <= 0.5f) return dst - (1 - 2*src) * dst * (1 - dst);
                float d = dst <= 0.25f ? ((16*dst - 12) * dst + 4) * dst : (float)Math.sqrt(dst);
                return dst + (2*src - 1) * (d - dst);
            case DARKEN:      return Math.min(src, dst);
            case LIGHTEN:     return Math.max(src, dst);
            case DIFFERENCE:  return Math.abs(src - dst);
            case EXCLUSION:   return src + dst - 2 * src * dst;
            case COLOR_DODGE: return dst == 0 ? 0 : Math.min(1f, dst / (1 - src + 0.001f));
            case COLOR_BURN:  return dst == 1 ? 1 : Math.max(0f, 1 - (1 - dst) / (src + 0.001f));
            default:          return src; // NORMAL
        }
    }

    private float clamp(float v) { return Math.max(0, Math.min(1, v)); }
}
