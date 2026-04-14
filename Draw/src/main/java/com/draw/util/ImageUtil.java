package com.draw.util;

import com.draw.canvas.CanvasRenderer;
import com.draw.model.CanvasDocument;
import com.draw.model.Layer;
import com.draw.model.LayerModel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageUtil {

    /** Composites all visible layers and saves to file. */
    public static void saveAs(CanvasDocument doc, File file) throws IOException {
        BufferedImage composite = composite(doc.getLayerModel(), doc.getWidth(), doc.getHeight());
        String name = file.getName().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            // Flatten alpha to white background for JPG
            BufferedImage rgb = new BufferedImage(doc.getWidth(), doc.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = rgb.createGraphics();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, doc.getWidth(), doc.getHeight());
            g2.drawImage(composite, 0, 0, null);
            g2.dispose();
            ImageIO.write(rgb, "jpg", file);
        } else {
            ImageIO.write(composite, "png", file);
        }
    }

    /** Opens a file chooser for saving. Returns the chosen file or null if cancelled. */
    public static File showSaveDialog(Component parent) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save As");
        fc.addChoosableFileFilter(new FileNameExtensionFilter("PNG Image", "png"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("JPEG Image", "jpg", "jpeg"));
        fc.setFileFilter(fc.getChoosableFileFilters()[1]);
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
        File f = fc.getSelectedFile();
        // Auto-add extension if missing
        String path = f.getAbsolutePath();
        if (!path.toLowerCase().endsWith(".png") && !path.toLowerCase().endsWith(".jpg")
                && !path.toLowerCase().endsWith(".jpeg")) {
            f = new File(path + ".png");
        }
        return f;
    }

    /** Opens a file chooser for loading. Returns loaded image or null if cancelled/failed. */
    public static BufferedImage showOpenDialog(Component parent) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Open Image");
        fc.addChoosableFileFilter(
                new FileNameExtensionFilter("Image files", "png", "jpg", "jpeg", "bmp", "gif"));
        fc.setFileFilter(fc.getChoosableFileFilters()[1]);
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
        try {
            return ImageIO.read(fc.getSelectedFile());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent, "Failed to open image: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public static BufferedImage composite(LayerModel model, int w, int h) {
        // Delegate to CanvasRenderer which handles blend modes correctly
        CanvasRenderer renderer = new CanvasRenderer();
        return renderer.getComposite(model, w, h);
    }

    /** Scales an image to fit within maxW x maxH while preserving aspect ratio. */
    public static BufferedImage scaledThumbnail(BufferedImage src, int maxW, int maxH) {
        if (src == null) return new BufferedImage(maxW, maxH, BufferedImage.TYPE_INT_ARGB);
        double sx = (double) maxW / src.getWidth();
        double sy = (double) maxH / src.getHeight();
        double scale = Math.min(sx, sy);
        int w = Math.max(1, (int)(src.getWidth() * scale));
        int h = Math.max(1, (int)(src.getHeight() * scale));
        BufferedImage thumb = new BufferedImage(maxW, maxH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = thumb.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int ox = (maxW - w) / 2;
        int oy = (maxH - h) / 2;
        g2.drawImage(src, ox, oy, w, h, null);
        g2.dispose();
        return thumb;
    }
}
