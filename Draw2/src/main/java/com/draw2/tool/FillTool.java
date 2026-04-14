package com.draw2.tool;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Queue;

public class FillTool implements Tool {

    private static final int TOLERANCE = 10;

    @Override
    public void onPress(Point p, DrawContext ctx) {
        BufferedImage canvas = ctx.model.getCanvas();
        int x = p.x, y = p.y;
        if (x < 0 || y < 0 || x >= canvas.getWidth() || y >= canvas.getHeight()) return;

        ctx.model.pushUndo();
        int targetRGB = canvas.getRGB(x, y);
        int fillRGB = ctx.foreColor.getRGB();

        if (targetRGB == fillRGB) return;

        bfsFill(canvas, x, y, targetRGB, fillRGB);
        ctx.model.fireCanvasChanged();
    }

    private void bfsFill(BufferedImage img, int startX, int startY, int targetRGB, int fillRGB) {
        int w = img.getWidth(), h = img.getHeight();
        boolean[][] visited = new boolean[w][h];
        Queue<Point> queue = new ArrayDeque<>();
        queue.add(new Point(startX, startY));
        visited[startX][startY] = true;

        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};

        while (!queue.isEmpty()) {
            Point cur = queue.poll();
            img.setRGB(cur.x, cur.y, fillRGB);
            for (int i = 0; i < 4; i++) {
                int nx = cur.x + dx[i];
                int ny = cur.y + dy[i];
                if (nx >= 0 && ny >= 0 && nx < w && ny < h && !visited[nx][ny]) {
                    int rgb = img.getRGB(nx, ny);
                    if (colorMatch(rgb, targetRGB, TOLERANCE)) {
                        visited[nx][ny] = true;
                        queue.add(new Point(nx, ny));
                    }
                }
            }
        }
    }

    private boolean colorMatch(int a, int b, int tolerance) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return Math.abs(ar - br) <= tolerance
            && Math.abs(ag - bg) <= tolerance
            && Math.abs(ab - bb) <= tolerance;
    }

    @Override
    public void onDrag(Point p, DrawContext ctx)   {}
    @Override
    public void onRelease(Point p, DrawContext ctx) {}
    @Override
    public void onPaint(BufferedImage overlay, DrawContext ctx) {}

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    }

    @Override
    public String getName() { return "油漆桶"; }
}
