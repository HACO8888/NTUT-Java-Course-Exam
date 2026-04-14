package com.draw.tool;

import com.draw.command.FillCommand;
import com.draw.command.DrawStrokeCommand;
import com.draw.model.Layer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Flood-fill (paint bucket) tool. Supports adjustable tolerance.
 */
public class FillTool implements Tool {
    /** 0–255: maximum per-channel colour distance to consider "same colour". */
    private int tolerance = 30;

    public void setTolerance(int t) { this.tolerance = Math.max(0, Math.min(255, t)); }
    public int getTolerance() { return tolerance; }

    @Override
    public void onPress(Point p, DrawContext ctx) {
        Layer layer = ctx.doc.getLayerModel().getActive();
        if (layer == null) return;

        BufferedImage img = layer.getImage();
        int w = img.getWidth(), h = img.getHeight();
        if (p.x < 0 || p.x >= w || p.y < 0 || p.y >= h) return;

        BufferedImage before = DrawStrokeCommand.snapshot(layer);

        int targetArgb = img.getRGB(p.x, p.y);
        Color fc = ctx.color;
        int fa = (int)(ctx.opacity * 255);
        int fillArgb = (fa << 24) | (fc.getRed() << 16) | (fc.getGreen() << 8) | fc.getBlue();

        if (targetArgb == fillArgb) return;

        // BFS flood fill with tolerance
        boolean[][] visited = new boolean[w][h];
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{p.x, p.y});
        visited[p.x][p.y] = true;

        while (!queue.isEmpty()) {
            int[] pt = queue.poll();
            int x = pt[0], y = pt[1];
            img.setRGB(x, y, fillArgb);

            int[][] neighbors = {{x + 1, y}, {x - 1, y}, {x, y + 1}, {x, y - 1}};
            for (int[] n : neighbors) {
                int nx = n[0], ny = n[1];
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                if (visited[nx][ny]) continue;
                if (!colorMatch(img.getRGB(nx, ny), targetArgb, tolerance)) continue;
                visited[nx][ny] = true;
                queue.add(new int[]{nx, ny});
            }
        }

        BufferedImage after = DrawStrokeCommand.snapshot(layer);
        ctx.doc.getHistory().push(new FillCommand(layer, before, after));
        ctx.doc.getLayerModel().fireLayersChanged();
        ctx.doc.markDirty();
    }

    private boolean colorMatch(int a, int b, int tol) {
        int dr = Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF));
        int dg = Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF));
        int db = Math.abs((a & 0xFF) - (b & 0xFF));
        int da = Math.abs(((a >> 24) & 0xFF) - ((b >> 24) & 0xFF));
        return dr <= tol && dg <= tol && db <= tol && da <= tol;
    }

    @Override
    public void onDrag(Point p, DrawContext ctx) {}

    @Override
    public void onRelease(Point p, DrawContext ctx) {}

    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {}

    @Override
    public Cursor getCursor() { return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR); }

    @Override
    public String getName() { return "Fill"; }
}
