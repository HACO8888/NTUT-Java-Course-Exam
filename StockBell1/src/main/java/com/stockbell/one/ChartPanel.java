package com.stockbell.one;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public final class ChartPanel extends JPanel {
    public enum Mode { INTRADAY_LINE, DAILY_K, WEEKLY_K }

    private static final Color BG = new Color(28, 28, 32);
    private static final Color GRID = new Color(60, 60, 70);
    private static final Color AXIS_FG = new Color(180, 180, 190);
    private static final Color UP_COLOR = new Color(220, 60, 60);
    private static final Color DOWN_COLOR = new Color(40, 160, 80);
    private static final Color LINE_COLOR = new Color(255, 200, 60);
    private static final Color MA5_COLOR = new Color(80, 160, 240);
    private static final Color MA20_COLOR = new Color(255, 140, 40);

    private final PricePane pricePane = new PricePane();
    private final VolumePane volumePane = new VolumePane();
    private final JLabel titleLabel = new JLabel("（請選取 watchlist 中一檔股票）");
    private final JToggleButton intradayBtn = new JToggleButton("分時");
    private final JToggleButton dailyBtn = new JToggleButton("日K");
    private final JToggleButton weeklyBtn = new JToggleButton("週K");

    private KlineSeries series = new KlineSeries(List.of());
    private double[] ma5 = new double[0];
    private double[] ma20 = new double[0];
    private Mode mode = Mode.INTRADAY_LINE;
    private String symbolTitle = "";
    private Runnable onModeChange = () -> {};

    public ChartPanel() {
        super(new BorderLayout());
        setBackground(BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        titleLabel.setForeground(AXIS_FG);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        header.add(titleLabel, BorderLayout.WEST);

        ButtonGroup grp = new ButtonGroup();
        grp.add(intradayBtn); grp.add(dailyBtn); grp.add(weeklyBtn);
        intradayBtn.setSelected(true);
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        modePanel.setBackground(BG);
        modePanel.add(intradayBtn); modePanel.add(dailyBtn); modePanel.add(weeklyBtn);
        header.add(modePanel, BorderLayout.EAST);

        intradayBtn.addActionListener(e -> { mode = Mode.INTRADAY_LINE; onModeChange.run(); });
        dailyBtn.addActionListener(e -> { mode = Mode.DAILY_K; onModeChange.run(); });
        weeklyBtn.addActionListener(e -> { mode = Mode.WEEKLY_K; onModeChange.run(); });

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pricePane, volumePane);
        split.setResizeWeight(0.75);
        split.setDividerSize(2);
        split.setBorder(null);

        add(header, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    public Mode mode() { return mode; }

    public void setOnModeChange(Runnable r) { this.onModeChange = r == null ? () -> {} : r; }

    public void setData(String symbol, String name, KlineSeries series) {
        this.symbolTitle = symbol + " " + name;
        this.series = series == null ? new KlineSeries(List.of()) : series;
        this.ma5 = this.series.size() >= 5 ? this.series.movingAverage(5) : new double[0];
        this.ma20 = this.series.size() >= 20 ? this.series.movingAverage(20) : new double[0];
        titleLabel.setText(symbolTitle + "    " + modeLabel());
        repaint();
    }

    private String modeLabel() {
        return switch (mode) {
            case INTRADAY_LINE -> "[分時走勢]";
            case DAILY_K -> "[日 K 線 + MA5/MA20]";
            case WEEKLY_K -> "[週 K 線 + MA5/MA20]";
        };
    }

    private final class PricePane extends JPanel {
        PricePane() { setBackground(BG); }

        @Override
        protected void paintComponent(Graphics gg) {
            super.paintComponent(gg);
            Graphics2D g = (Graphics2D) gg.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int leftM = 60, rightM = 12, topM = 8, botM = 22;
            int plotW = w - leftM - rightM;
            int plotH = h - topM - botM;
            if (plotW <= 0 || plotH <= 0) { g.dispose(); return; }

            List<Kline> bars = series.bars();
            if (bars.isEmpty()) {
                g.setColor(AXIS_FG);
                g.drawString("無資料", w / 2 - 20, h / 2);
                g.dispose();
                return;
            }

            double pMin = series.minLow();
            double pMax = series.maxHigh();
            if (pMax <= pMin) { pMax = pMin + 1; }
            double pad = (pMax - pMin) * 0.05;
            pMin -= pad; pMax += pad;
            double pRange = pMax - pMin;

            // grid + y labels
            g.setColor(GRID);
            g.setFont(g.getFont().deriveFont(11f));
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i <= 4; i++) {
                int y = topM + i * plotH / 4;
                g.setColor(GRID);
                g.drawLine(leftM, y, leftM + plotW, y);
                double price = pMax - i * pRange / 4;
                g.setColor(AXIS_FG);
                String s = String.format("%.2f", price);
                g.drawString(s, leftM - 6 - fm.stringWidth(s), y + 4);
            }

            int n = bars.size();
            double dx = (double) plotW / Math.max(1, n - 1);

            if (mode == Mode.INTRADAY_LINE) {
                GeneralPath path = new GeneralPath();
                boolean started = false;
                for (int i = 0; i < n; i++) {
                    double c = bars.get(i).close();
                    if (Double.isNaN(c)) continue;
                    float x = (float) (leftM + i * dx);
                    float y = (float) (topM + (pMax - c) / pRange * plotH);
                    if (!started) { path.moveTo(x, y); started = true; }
                    else path.lineTo(x, y);
                }
                g.setColor(LINE_COLOR);
                g.setStroke(new BasicStroke(1.6f));
                g.draw(path);
            } else {
                double barWidth = Math.max(1.5, (plotW / (double) n) * 0.7);
                for (int i = 0; i < n; i++) {
                    Kline k = bars.get(i);
                    double xc = leftM + i * dx;
                    double yHigh = topM + (pMax - k.high()) / pRange * plotH;
                    double yLow = topM + (pMax - k.low()) / pRange * plotH;
                    double yOpen = topM + (pMax - k.open()) / pRange * plotH;
                    double yClose = topM + (pMax - k.close()) / pRange * plotH;
                    Color color = k.close() >= k.open() ? UP_COLOR : DOWN_COLOR;
                    g.setColor(color);
                    g.setStroke(new BasicStroke(1f));
                    g.drawLine((int) xc, (int) yHigh, (int) xc, (int) yLow);
                    int bx = (int) (xc - barWidth / 2);
                    int by = (int) Math.min(yOpen, yClose);
                    int bh = Math.max(1, (int) Math.abs(yOpen - yClose));
                    g.fillRect(bx, by, (int) barWidth, bh);
                }
                drawMA(g, ma5, MA5_COLOR, leftM, topM, plotH, pMax, pRange, dx);
                drawMA(g, ma20, MA20_COLOR, leftM, topM, plotH, pMax, pRange, dx);

                int lx = leftM + 4, ly = topM + 14;
                g.setColor(MA5_COLOR); g.drawString("MA5", lx, ly);
                g.setColor(MA20_COLOR); g.drawString("MA20", lx + 36, ly);
            }

            // x labels
            g.setColor(AXIS_FG);
            SimpleDateFormat sdf = mode == Mode.INTRADAY_LINE
                    ? new SimpleDateFormat("HH:mm")
                    : new SimpleDateFormat("MM/dd");
            int step = Math.max(1, n / 6);
            for (int i = 0; i < n; i += step) {
                int x = (int) (leftM + i * dx);
                String s = sdf.format(new Date(bars.get(i).timestamp() * 1000L));
                g.drawString(s, x - fm.stringWidth(s) / 2, topM + plotH + 14);
            }

            g.dispose();
        }

        private void drawMA(Graphics2D g, double[] ma, Color color,
                            int leftM, int topM, int plotH, double pMax, double pRange, double dx) {
            if (ma.length == 0) return;
            g.setColor(color);
            g.setStroke(new BasicStroke(1.4f));
            GeneralPath path = new GeneralPath();
            boolean started = false;
            for (int i = 0; i < ma.length; i++) {
                if (Double.isNaN(ma[i])) { started = false; continue; }
                float x = (float) (leftM + i * dx);
                float y = (float) (topM + (pMax - ma[i]) / pRange * plotH);
                if (!started) { path.moveTo(x, y); started = true; }
                else path.lineTo(x, y);
            }
            g.draw(path);
        }
    }

    private final class VolumePane extends JPanel {
        VolumePane() { setBackground(BG); }

        @Override
        protected void paintComponent(Graphics gg) {
            super.paintComponent(gg);
            Graphics2D g = (Graphics2D) gg.create();
            int w = getWidth(), h = getHeight();
            int leftM = 60, rightM = 12, topM = 4, botM = 4;
            int plotW = w - leftM - rightM;
            int plotH = h - topM - botM;
            if (plotW <= 0 || plotH <= 0) { g.dispose(); return; }

            List<Kline> bars = series.bars();
            if (bars.isEmpty()) { g.dispose(); return; }

            long maxVol = series.maxVolume();
            if (maxVol <= 0) maxVol = 1;
            int n = bars.size();
            double dx = (double) plotW / Math.max(1, n);
            double barW = Math.max(1, dx * 0.7);

            for (int i = 0; i < n; i++) {
                Kline k = bars.get(i);
                int bh = (int) ((double) k.volume() / maxVol * plotH);
                int x = (int) (leftM + i * dx);
                int y = topM + plotH - bh;
                Color color = k.close() >= k.open() ? UP_COLOR : DOWN_COLOR;
                g.setColor(color);
                g.fillRect(x, y, (int) barW, bh);
            }

            g.setColor(AXIS_FG);
            g.setFont(g.getFont().deriveFont(10f));
            g.drawString("成交量", leftM - 50, topM + 12);
            g.dispose();
        }
    }
}
