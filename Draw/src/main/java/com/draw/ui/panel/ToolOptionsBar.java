package com.draw.ui.panel;

import com.draw.tool.*;
import com.draw.tool.CropTool;
import com.draw.tool.RectSelectTool;
import com.draw.tool.SmudgeTool;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.function.Consumer;

/**
 * Context-sensitive tool options bar below the menu bar.
 * Shows different controls depending on which tool is active.
 */
public class ToolOptionsBar extends JPanel {
    private static final Color BAR_BG = new Color(0x2d2d2d);
    private static final Color FG     = new Color(0xCCCCCC);

    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);

    // -- Brush / Pencil / Eraser card --
    private final JSpinner brushSizeSpinner = makeSpinner(1, 500, 10);
    private final JSpinner brushOpacitySpinner = makeSpinner(1, 100, 100);
    private final JSlider hardnessSlider = makeSlider(0, 100, 80);

    // -- Shape card --
    private final JCheckBox fillCheck = new JCheckBox("Fill");
    private final JSpinner strokeSpinner = makeSpinner(1, 100, 2);

    // -- Fill card --
    private final JSpinner toleranceSpinner = makeSpinner(0, 255, 30);

    // -- Text card --
    private final JComboBox<String> fontCombo;
    private final JToggleButton boldBtn  = makeToggle("B");
    private final JToggleButton italicBtn = makeToggle("I");
    private final JSpinner fontSizeSpinner = makeSpinner(6, 256, 24);

    // -- Line card --
    private final JSpinner lineWidthSpinner = makeSpinner(1, 100, 2);
    private final JLabel shiftHint = hint("Shift = 45°");

    // -- Spray card --
    private final JSpinner sprayRadiusSpinner = makeSpinner(5, 200, 40);
    private final JSpinner sprayDensitySpinner = makeSpinner(1, 50, 12);

    // -- Callbacks --
    private Consumer<float[]> brushCallback;   // [size, opacity, hardness]
    private Consumer<Boolean> fillCallback;
    private Consumer<Float> strokeCallback;
    private Consumer<Integer> toleranceCallback;
    private Consumer<String[]> textFontCallback; // [name, bold, italic, size]
    private Consumer<Float> lineWidthCallback;
    private Runnable cropApplyCallback;

    public ToolOptionsBar() {
        setBackground(BAR_BG);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x1a1a1a)));
        setPreferredSize(new Dimension(Integer.MAX_VALUE, 38));

        cardPanel.setBackground(BAR_BG);

        // All available fonts (subset for performance)
        String[] fonts = {
            "SansSerif", "Serif", "Monospaced",
            "Arial", "Helvetica", "Times New Roman", "Courier New",
            "Impact", "Comic Sans MS", "Georgia", "Verdana"
        };
        fontCombo = new JComboBox<>(fonts);
        style(fontCombo);

        // Build cards
        cardPanel.add(buildBrushCard(),    "brush");
        cardPanel.add(buildShapeCard(),    "shape");
        cardPanel.add(buildFillCard(),     "fill");
        cardPanel.add(buildTextCard(),     "text");
        cardPanel.add(buildLineCard(),     "line");
        cardPanel.add(buildSprayCard(),    "spray");
        cardPanel.add(buildSelectCard(),   "select");
        cardPanel.add(buildGradientCard(), "gradient");
        cardPanel.add(buildCropCard(),     "crop");
        cardPanel.add(buildSmudgeCard(),   "smudge");
        cardPanel.add(buildDefaultCard(),  "default");

        add(cardPanel, BorderLayout.CENTER);

        // Wire listeners
        ChangeListener brushCL = e -> fireBrush();
        ((JSpinner) brushSizeSpinner).addChangeListener(brushCL);
        ((JSpinner) brushOpacitySpinner).addChangeListener(brushCL);
        hardnessSlider.addChangeListener(e -> fireBrush());

        fillCheck.addItemListener(e -> {
            if (fillCallback != null) fillCallback.accept(fillCheck.isSelected());
        });
        strokeSpinner.addChangeListener(e -> {
            if (strokeCallback != null)
                strokeCallback.accept(((Number) strokeSpinner.getValue()).floatValue());
        });
        toleranceSpinner.addChangeListener(e -> {
            if (toleranceCallback != null)
                toleranceCallback.accept(((Number) toleranceSpinner.getValue()).intValue());
        });
        fontCombo.addActionListener(e -> fireText());
        boldBtn.addActionListener(e -> fireText());
        italicBtn.addActionListener(e -> fireText());
        fontSizeSpinner.addChangeListener(e -> fireText());
        lineWidthSpinner.addChangeListener(e -> {
            if (lineWidthCallback != null)
                lineWidthCallback.accept(((Number) lineWidthSpinner.getValue()).floatValue());
        });
    }

    // ---- Show card based on active tool ----

    public void showFor(Tool tool) {
        if (tool instanceof PencilTool || tool instanceof BrushTool || tool instanceof EraserTool) {
            cards.show(cardPanel, "brush");
        } else if (tool instanceof RectangleTool || tool instanceof EllipseTool || tool instanceof PolygonTool) {
            cards.show(cardPanel, "shape");
        } else if (tool instanceof FillTool) {
            cards.show(cardPanel, "fill");
        } else if (tool instanceof TextTool) {
            cards.show(cardPanel, "text");
        } else if (tool instanceof LineTool) {
            cards.show(cardPanel, "line");
        } else if (tool instanceof SprayTool) {
            cards.show(cardPanel, "spray");
        } else if (tool instanceof SmudgeTool) {
            cards.show(cardPanel, "smudge");
        } else if (tool instanceof RectSelectTool) {
            cards.show(cardPanel, "select");
        } else if (tool instanceof GradientTool) {
            cards.show(cardPanel, "gradient");
        } else if (tool instanceof CropTool) {
            cards.show(cardPanel, "crop");
        } else {
            cards.show(cardPanel, "default");
        }
    }

    // ---- Setters for callbacks ----

    public void setBrushCallback(Consumer<float[]> cb) { this.brushCallback = cb; }
    public void setFillCallback(Consumer<Boolean> cb) { this.fillCallback = cb; }
    public void setStrokeCallback(Consumer<Float> cb) { this.strokeCallback = cb; }
    public void setToleranceCallback(Consumer<Integer> cb) { this.toleranceCallback = cb; }
    public void setTextFontCallback(Consumer<String[]> cb) { this.textFontCallback = cb; }
    public void setLineWidthCallback(Consumer<Float> cb) { this.lineWidthCallback = cb; }
    public void setCropApplyCallback(Runnable cb) { this.cropApplyCallback = cb; }

    // ---- Getters for current values ----

    public float getBrushSize()    { return ((Number) brushSizeSpinner.getValue()).floatValue(); }
    public float getBrushOpacity() { return ((Number) brushOpacitySpinner.getValue()).intValue() / 100f; }
    public float getBrushHardness() { return hardnessSlider.getValue() / 100f; }

    // Sync spinner when brush panel slider changes
    public void setBrushSize(int v) { brushSizeSpinner.setValue(v); }
    public void setBrushOpacity(int pct) { brushOpacitySpinner.setValue(pct); }

    /** Increase/decrease brush size via [ ] keyboard shortcuts. */
    public void adjustBrushSize(int delta) {
        int cur = ((Number) brushSizeSpinner.getValue()).intValue();
        int next = Math.max(1, Math.min(500, cur + delta));
        brushSizeSpinner.setValue(next);
        fireBrush();
    }

    // ---- Private fire methods ----

    private void fireBrush() {
        if (brushCallback != null) {
            brushCallback.accept(new float[]{getBrushSize(), getBrushOpacity(), getBrushHardness()});
        }
    }

    private void fireText() {
        if (textFontCallback != null) {
            String name = (String) fontCombo.getSelectedItem();
            String bold = boldBtn.isSelected() ? "bold" : "";
            String italic = italicBtn.isSelected() ? "italic" : "";
            String size = String.valueOf(((Number) fontSizeSpinner.getValue()).intValue());
            textFontCallback.accept(new String[]{name, bold, italic, size});
        }
    }

    // ---- Card builders ----

    private JPanel buildBrushCard() {
        JPanel p = row();
        p.add(label("Size")); p.add(brushSizeSpinner);
        p.add(sep());
        p.add(label("Opacity %")); p.add(brushOpacitySpinner);
        p.add(sep());
        p.add(label("Hardness")); p.add(hardnessSlider);
        return p;
    }

    private JPanel buildShapeCard() {
        JPanel p = row();
        style(fillCheck);
        fillCheck.setForeground(FG);
        p.add(fillCheck);
        p.add(sep());
        p.add(label("Stroke")); p.add(strokeSpinner);
        p.add(hint("Shift = square/circle"));
        return p;
    }

    private JPanel buildFillCard() {
        JPanel p = row();
        p.add(label("Tolerance")); p.add(toleranceSpinner);
        p.add(hint("0 = exact match"));
        return p;
    }

    private JPanel buildTextCard() {
        JPanel p = row();
        p.add(label("Font")); p.add(fontCombo);
        p.add(boldBtn); p.add(italicBtn);
        p.add(sep());
        p.add(label("Size")); p.add(fontSizeSpinner);
        return p;
    }

    private JPanel buildLineCard() {
        JPanel p = row();
        p.add(label("Width")); p.add(lineWidthSpinner);
        p.add(sep());
        p.add(shiftHint);
        return p;
    }

    private JPanel buildSprayCard() {
        JPanel p = row();
        p.add(label("Radius")); p.add(sprayRadiusSpinner);
        p.add(sep());
        p.add(label("Density")); p.add(sprayDensitySpinner);
        return p;
    }

    private JPanel buildSelectCard() {
        JPanel p = row();
        p.add(hint("Drag to select  •  Drag inside selection to move  •  Ctrl+C/X/V  •  Delete"));
        return p;
    }

    private JPanel buildGradientCard() {
        JPanel p = row();
        p.add(hint("Drag to set gradient direction  •  Uses foreground → secondary color"));
        return p;
    }

    private JPanel buildCropCard() {
        JPanel p = row();
        p.add(hint("Drag to define crop area"));
        p.add(sep());
        JButton applyBtn = new JButton("Apply Crop  ↵");
        applyBtn.setFocusPainted(false);
        applyBtn.setBackground(new Color(0x0e7acc));
        applyBtn.setForeground(Color.WHITE);
        applyBtn.setFont(applyBtn.getFont().deriveFont(11f));
        applyBtn.setPreferredSize(new Dimension(110, 24));
        applyBtn.addActionListener(e -> { if (cropApplyCallback != null) cropApplyCallback.run(); });
        p.add(applyBtn);
        p.add(sep());
        p.add(hint("Escape to cancel"));
        return p;
    }

    private JPanel buildSmudgeCard() {
        JPanel p = row();
        p.add(label("Size")); p.add(brushSizeSpinner);
        p.add(sep());
        p.add(label("Strength %")); p.add(brushOpacitySpinner);
        return p;
    }

    private JPanel buildDefaultCard() {
        JPanel p = row();
        p.add(hint("Select a tool to see options"));
        return p;
    }

    // ---- Helpers ----

    private JPanel row() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        p.setBackground(BAR_BG);
        return p;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(FG);
        l.setFont(l.getFont().deriveFont(11f));
        return l;
    }

    private JLabel hint(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(0x888888));
        l.setFont(l.getFont().deriveFont(Font.ITALIC, 11f));
        return l;
    }

    private JSeparator sep() {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setPreferredSize(new Dimension(1, 20));
        s.setForeground(new Color(0x444444));
        return s;
    }

    private static JSpinner makeSpinner(int min, int max, int val) {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(val, min, max, 1));
        sp.setPreferredSize(new Dimension(60, 24));
        sp.setMaximumSize(new Dimension(60, 24));
        JComponent editor = sp.getEditor();
        editor.setBackground(new Color(0x3c3c3c));
        if (editor instanceof JSpinner.DefaultEditor) {
            JSpinner.DefaultEditor de = (JSpinner.DefaultEditor) editor;
            de.getTextField().setBackground(new Color(0x3c3c3c));
            de.getTextField().setForeground(new Color(0xCCCCCC));
            de.getTextField().setCaretColor(new Color(0xCCCCCC));
        }
        return sp;
    }

    private static JSlider makeSlider(int min, int max, int val) {
        JSlider s = new JSlider(min, max, val);
        s.setBackground(new Color(0x2d2d2d));
        s.setFocusable(false);
        s.setPreferredSize(new Dimension(90, 22));
        return s;
    }

    private static JToggleButton makeToggle(String text) {
        JToggleButton b = new JToggleButton(text);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(28, 24));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 12f));
        return b;
    }

    private static void style(JComponent c) {
        c.setBackground(new Color(0x3c3c3c));
        c.setForeground(new Color(0xCCCCCC));
    }
}
