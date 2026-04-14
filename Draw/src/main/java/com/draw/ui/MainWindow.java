package com.draw.ui;

import com.draw.canvas.CanvasPanel;
import com.draw.model.CanvasDocument;
import com.draw.model.Layer;
import com.draw.tool.*;
import com.draw.tool.CropTool;
import com.draw.tool.RectSelectTool;
import com.draw.ui.dialog.CanvasSizeDialog;
import com.draw.ui.dialog.NewDocumentDialog;
import com.draw.ui.panel.*;
import com.draw.ui.toolbar.ToolBar;
import com.draw.util.DrawIcons;
import com.draw.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Main application window — wires together all UI components.
 */
public class MainWindow extends JFrame {

    private CanvasDocument doc;
    private CanvasPanel canvasPanel;
    private final ToolBar toolBar = new ToolBar();
    private final ColorPanel colorPanel = new ColorPanel();
    private final BrushSettingsPanel brushPanel = new BrushSettingsPanel();
    private LayerPanel layerPanel;
    private StatusBar statusBar;
    private ToolOptionsBar toolOptionsBar;

    private Color currentColor = Color.BLACK;
    private Color secondaryColor = Color.WHITE;
    private File currentFile = null;

    // Recent files (max 5)
    private final Deque<File> recentFiles = new ArrayDeque<>();
    private JMenu recentFilesMenu;

    // Quick-access UndoRedo labels
    private JMenuItem undoItem, redoItem;

    public MainWindow() {
        doc = new CanvasDocument(800, 600);
        initUI();
        setupShortcuts();
        fillDefaultBackground();
        updateTitle();
    }

    private void fillDefaultBackground() {
        Layer bg = doc.getLayerModel().getActive();
        if (bg != null) {
            Graphics2D g2 = bg.getImage().createGraphics();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, doc.getWidth(), doc.getHeight());
            g2.dispose();
            doc.getLayerModel().fireLayersChanged();
        }
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1400, 860);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { confirmAndExit(); }
        });

        // Canvas
        canvasPanel = new CanvasPanel(doc);
        toolBar.getTextTool().setCanvasComponent(canvasPanel);

        // Eyedropper wired to color panel
        EyedropperTool eyedropperTool = new EyedropperTool(c -> {
            currentColor = c;
            colorPanel.setColorFromExternal(c);
            updateDrawContext();
        });
        canvasPanel.setEyedropperTool(eyedropperTool);
        toolBar.setEyedropperTool(eyedropperTool);

        // Tool options bar
        toolOptionsBar = new ToolOptionsBar();
        toolOptionsBar.setBrushCallback(vals -> {
            // vals = [size, opacity, hardness]
            updateDrawContext();
        });
        toolOptionsBar.setFillCallback(filled -> {
            if (toolBar.getActiveTool() instanceof RectangleTool)
                ((RectangleTool) toolBar.getActiveTool()).setFilled(filled);
            if (toolBar.getActiveTool() instanceof EllipseTool)
                ((EllipseTool) toolBar.getActiveTool()).setFilled(filled);
            if (toolBar.getActiveTool() instanceof PolygonTool)
                ((PolygonTool) toolBar.getActiveTool()).setFilled(filled);
        });
        toolOptionsBar.setToleranceCallback(tol -> {
            if (toolBar.getActiveTool() instanceof FillTool)
                ((FillTool) toolBar.getActiveTool()).setTolerance(tol);
        });
        toolOptionsBar.setTextFontCallback(parts -> {
            TextTool tt = toolBar.getTextTool();
            tt.setFontName(parts[0]);
            int style = Font.PLAIN;
            if (!parts[1].isEmpty()) style |= Font.BOLD;
            if (!parts[2].isEmpty()) style |= Font.ITALIC;
            tt.setFontStyle(style);
        });

        // Wire tool bar
        toolBar.setToolChangeCallback(tool -> {
            canvasPanel.setActiveTool(tool);
            toolOptionsBar.showFor(tool);
            if (statusBar != null) statusBar.setTool(tool.getName());
        });
        canvasPanel.setActiveTool(toolBar.getDefaultTool());
        toolOptionsBar.showFor(toolBar.getDefaultTool());

        // Wire color callbacks (FG and BG)
        colorPanel.setFgColorCallback(c -> { currentColor = c; updateDrawContext(); });
        colorPanel.setBgColorCallback(c -> { secondaryColor = c; updateDrawContext(); });

        // Wire brush callback
        brushPanel.setChangeCallback(vals -> updateDrawContext());

        updateDrawContext();

        // Status bar
        statusBar = new StatusBar();
        statusBar.setCanvasSize(doc.getWidth(), doc.getHeight());
        statusBar.setZoomCallback(z -> canvasPanel.setZoomLevel(z));
        canvasPanel.setStatusCallback((zoom, cx, cy) -> {
            statusBar.setZoom(zoom);
            statusBar.setCursorPos(cx, cy);
        });

        // Keep layer name in status bar in sync
        doc.getLayerModel().addPropertyChangeListener(e -> {
            Layer active = doc.getLayerModel().getActive();
            if (active != null && statusBar != null) statusBar.setLayerName(active.getName());
        });
        Layer initialLayer = doc.getLayerModel().getActive();
        if (initialLayer != null) statusBar.setLayerName(initialLayer.getName());

        // Layer panel
        layerPanel = new LayerPanel(doc);

        // ---- Right panel as accordion ----
        JPanel rightPanel = buildRightPanel();

        // ---- Canvas scroll ----
        JScrollPane scrollPane = new JScrollPane(canvasPanel);
        scrollPane.setBackground(new Color(0x1e1e1e));
        scrollPane.getViewport().setBackground(new Color(0x1e1e1e));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);

        // Crop wiring: both Enter key and toolbar button trigger the same action
        Runnable applyCrop = () -> {
            CropTool ct = toolBar.getCropTool();
            java.awt.Rectangle r = ct.getCropRect();
            if (r != null && r.width > 4 && r.height > 4) {
                doc.crop(r);
                ct.clearCrop();
                canvasPanel.clearOverlay();
                canvasPanel.setDoc(doc);
                rebuildLayers();
                updateDrawContext();
                statusBar.setCanvasSize(doc.getWidth(), doc.getHeight());
                doc.markDirty();
                updateTitle();
            }
        };
        canvasPanel.setCropCallback(applyCrop);
        toolOptionsBar.setCropApplyCallback(applyCrop);

        // Right-click context menu on canvas
        canvasPanel.setRightClickMenu(buildCanvasContextMenu());

        setJMenuBar(buildMenuBar2());

        // ---- Layout ----
        setLayout(new BorderLayout());
        add(toolOptionsBar, BorderLayout.NORTH);
        add(toolBar, BorderLayout.WEST);
        add(scrollPane, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);
    }

    // ---- Right panel (accordion) ----

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(0x252526));
        panel.setPreferredSize(new Dimension(270, 600));
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(0x3c3c3c)));

        panel.add(buildSection("Color", colorPanel, true));
        panel.add(buildSection("Brush", brushPanel, true));
        panel.add(buildSection("Layers", layerPanel, true));

        return panel;
    }

    private JPanel buildSection(String title, JComponent content, boolean expanded) {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(new Color(0x252526));

        // Header button
        JButton header = new JButton((expanded ? "▼ " : "▶ ") + title);
        header.setHorizontalAlignment(SwingConstants.LEFT);
        header.setBackground(new Color(0x2d2d2d));
        header.setForeground(new Color(0xCCCCCC));
        header.setBorderPainted(false);
        header.setFocusPainted(false);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));
        header.setPreferredSize(new Dimension(270, 28));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(0x3c3c3c)),
            BorderFactory.createEmptyBorder(0, 8, 0, 0)));

        content.setVisible(expanded);

        header.addActionListener(e -> {
            boolean nowVisible = !content.isVisible();
            content.setVisible(nowVisible);
            header.setText((nowVisible ? "▼ " : "▶ ") + title);
            section.revalidate();
            section.repaint();
        });

        section.add(header, BorderLayout.NORTH);
        section.add(content, BorderLayout.CENTER);
        return section;
    }

    // ---- Draw context ----

    private void updateDrawContext() {
        DrawContext ctx = new DrawContext(doc, currentColor,
                toolOptionsBar.getBrushSize(),
                toolOptionsBar.getBrushOpacity(),
                toolOptionsBar.getBrushHardness());
        ctx.secondaryColor = secondaryColor;
        canvasPanel.setDrawContext(ctx);
    }

    // ---- Menu ----

    private JMenuBar buildMenuBar2() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(new Color(0x2d2d2d));

        // File
        JMenu file = new JMenu("File");
        file.add(menuItem("New", "ctrl N", this::onNew));
        file.add(menuItem("Open...", "ctrl O", this::onOpen));
        file.addSeparator();
        file.add(menuItem("Save", "ctrl S", this::onSave));
        file.add(menuItem("Save As...", "ctrl shift S", this::onSaveAs));
        file.addSeparator();
        recentFilesMenu = new JMenu("Recent Files");
        recentFilesMenu.setEnabled(false);
        file.add(recentFilesMenu);
        file.addSeparator();
        file.add(menuItem("Exit", null, this::confirmAndExit));
        bar.add(file);

        // Edit
        JMenu edit = new JMenu("Edit");
        undoItem = menuItem("Undo", "ctrl Z", this::doUndo);
        redoItem = menuItem("Redo", "ctrl Y", this::doRedo);
        edit.add(undoItem);
        edit.add(redoItem);
        edit.addSeparator();
        edit.add(menuItem("Copy",  "ctrl C", () -> canvasPanel.copySelection()));
        edit.add(menuItem("Cut",   "ctrl X", () -> canvasPanel.cutSelection()));
        edit.add(menuItem("Paste", "ctrl V", () -> { toolBar.selectTool("Select"); canvasPanel.pasteSelection(); }));
        edit.addSeparator();
        edit.add(menuItem("Clear Layer", null, this::clearCanvas));
        bar.add(edit);

        // Image
        JMenu image = new JMenu("Image");
        image.add(menuItem("Canvas Size...", null, this::onResizeCanvas));
        image.add(menuItem("Flatten Layers", null, this::flattenLayers));
        image.addSeparator();
        image.add(menuItem("Rotate 90° CW", null, () -> rotateCanvas(true)));
        image.add(menuItem("Rotate 90° CCW", null, () -> rotateCanvas(false)));
        image.add(menuItem("Flip Horizontal", null, () -> flipCanvas(true)));
        image.add(menuItem("Flip Vertical", null, () -> flipCanvas(false)));
        bar.add(image);

        // Select
        JMenu select = new JMenu("Select");
        select.add(menuItem("Select All", "ctrl A", this::selectAll));
        select.add(menuItem("Deselect",   "ctrl D", this::deselect));
        bar.add(select);

        // View
        JMenu view = new JMenu("View");
        view.add(menuItem("Zoom In", "ctrl EQUALS", () -> canvasPanel.zoomIn()));
        view.add(menuItem("Zoom Out", "ctrl MINUS", () -> canvasPanel.zoomOut()));
        view.add(menuItem("Fit Canvas", "ctrl 0", () -> canvasPanel.fitCanvas()));
        view.add(menuItem("Zoom 100%", "ctrl 1", () -> canvasPanel.setZoomLevel(1.0)));
        view.addSeparator();
        JCheckBoxMenuItem gridItem = new JCheckBoxMenuItem("Grid");
        gridItem.setAccelerator(KeyStroke.getKeyStroke("ctrl QUOTE"));
        gridItem.addActionListener(e -> canvasPanel.setGridEnabled(gridItem.isSelected()));
        view.add(gridItem);
        JCheckBoxMenuItem rulerItem = new JCheckBoxMenuItem("Rulers", true);
        rulerItem.setAccelerator(KeyStroke.getKeyStroke("ctrl R"));
        rulerItem.addActionListener(e -> canvasPanel.setRulersEnabled(rulerItem.isSelected()));
        view.add(rulerItem);
        bar.add(view);

        // Filter
        JMenu filter = new JMenu("Filter");
        filter.add(menuItem("Gaussian Blur...",    null, this::filterBlur));
        filter.add(menuItem("Sharpen",             null, this::filterSharpen));
        filter.addSeparator();
        filter.add(menuItem("Invert Colors",       null, this::filterInvert));
        filter.add(menuItem("Grayscale",           null, this::filterGrayscale));
        filter.addSeparator();
        filter.add(menuItem("Brightness / Contrast...", null, this::filterBrightnessContrast));
        filter.add(menuItem("Hue / Saturation...", null, this::filterHueSaturation));
        bar.add(filter);

        return bar;
    }

    private JMenuItem menuItem(String text, String accel, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        if (accel != null) item.setAccelerator(KeyStroke.getKeyStroke(accel));
        item.addActionListener(e -> action.run());
        return item;
    }

    // ---- Keyboard shortcuts ----

    private void setupShortcuts() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        // Undo / Redo
        bindKey(im, am, "ctrl Z", "undo", this::doUndo);
        bindKey(im, am, "ctrl Y", "redo", this::doRedo);
        bindKey(im, am, "ctrl shift Z", "redo2", this::doRedo);

        // File
        bindKey(im, am, "ctrl S", "save", this::onSave);
        bindKey(im, am, "ctrl O", "open", this::onOpen);
        bindKey(im, am, "ctrl N", "new", this::onNew);

        // Zoom
        bindKey(im, am, "ctrl EQUALS", "zoomIn", () -> canvasPanel.zoomIn());
        bindKey(im, am, "ctrl MINUS", "zoomOut", () -> canvasPanel.zoomOut());
        bindKey(im, am, "ctrl 0", "fit", () -> canvasPanel.fitCanvas());
        bindKey(im, am, "ctrl 1", "zoom100", () -> canvasPanel.setZoomLevel(1.0));

        // Tool shortcuts
        bindKey(im, am, "V", "toolSelect",  () -> toolBar.selectTool("Select"));
        bindKey(im, am, "B", "toolBrush",   () -> toolBar.selectTool("Brush"));
        bindKey(im, am, "P", "toolPencil",  () -> toolBar.selectTool("Pencil"));
        bindKey(im, am, "E", "toolEraser",  () -> toolBar.selectTool("Eraser"));
        bindKey(im, am, "G", "toolFill",    () -> toolBar.selectTool("Fill"));
        bindKey(im, am, "L", "toolLine",    () -> toolBar.selectTool("Line"));
        bindKey(im, am, "R", "toolRect",    () -> toolBar.selectTool("Rectangle"));
        bindKey(im, am, "O", "toolEllipse", () -> toolBar.selectTool("Ellipse"));
        bindKey(im, am, "W", "toolPoly",    () -> toolBar.selectTool("Polygon"));
        bindKey(im, am, "U", "toolGrad",    () -> toolBar.selectTool("Gradient"));
        bindKey(im, am, "T", "toolText",    () -> toolBar.selectTool("Text"));
        bindKey(im, am, "I", "toolEye",     () -> toolBar.selectTool("Eyedropper"));
        bindKey(im, am, "S", "toolSpray",   () -> toolBar.selectTool("Spray"));
        bindKey(im, am, "M", "toolSmudge",  () -> toolBar.selectTool("Smudge"));
        bindKey(im, am, "C", "toolCrop",    () -> toolBar.selectTool("Crop"));

        // Bracket shortcuts for brush size
        bindKey(im, am, "OPEN_BRACKET",  "brushSizeDown", () -> toolOptionsBar.adjustBrushSize(-5));
        bindKey(im, am, "CLOSE_BRACKET", "brushSizeUp",   () -> toolOptionsBar.adjustBrushSize(+5));

        // X = swap FG/BG colors
        bindKey(im, am, "X", "swapColors", () -> colorPanel.swapColors());

        // Select All / Deselect
        bindKey(im, am, "ctrl A", "selectAll", this::selectAll);
        bindKey(im, am, "ctrl D", "deselect",  this::deselect);

        // Selection clipboard (only active when Select tool is in use)
        bindKey(im, am, "ctrl C", "copy",   () -> canvasPanel.copySelection());
        bindKey(im, am, "ctrl X", "cut",    () -> canvasPanel.cutSelection());
        bindKey(im, am, "ctrl V", "paste",  () -> {
            toolBar.selectTool("Select");
            canvasPanel.pasteSelection();
        });
        bindKey(im, am, "DELETE", "delete", () -> canvasPanel.deleteSelection());

        // Shift for shape constraint (tracked in CanvasPanel via key listener)
    }

    // ---- Canvas right-click context menu ----

    private JPopupMenu buildCanvasContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(menuItem("Undo", null, this::doUndo));
        menu.add(menuItem("Redo", null, this::doRedo));
        menu.addSeparator();
        JMenuItem copyItem = menuItem("Copy Selection", null, () -> canvasPanel.copySelection());
        JMenuItem cutItem  = menuItem("Cut Selection",  null, () -> canvasPanel.cutSelection());
        JMenuItem pasteItem = menuItem("Paste", null, () -> {
            toolBar.selectTool("Select");
            canvasPanel.pasteSelection();
        });
        JMenuItem deleteItem = menuItem("Delete Selection", null, () -> canvasPanel.deleteSelection());
        menu.add(copyItem);
        menu.add(cutItem);
        menu.add(pasteItem);
        menu.add(deleteItem);
        menu.addSeparator();
        menu.add(menuItem("Clear Layer",     null, this::clearCanvas));
        menu.add(menuItem("Flatten Layers",  null, this::flattenLayers));

        // Dynamically enable/disable selection items on popup
        menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                boolean hasSel = canvasPanel.hasSelection();
                copyItem.setEnabled(hasSel);
                cutItem.setEnabled(hasSel);
                deleteItem.setEnabled(hasSel);
            }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });
        return menu;
    }

    private void bindKey(InputMap im, ActionMap am, String key, String name, Runnable action) {
        im.put(KeyStroke.getKeyStroke(key), name);
        am.put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    // ---- Undo / Redo ----

    private void doUndo() {
        doc.getHistory().undo();
        doc.getLayerModel().fireLayersChanged();
        layerPanel.rebuild();
    }

    private void doRedo() {
        doc.getHistory().redo();
        doc.getLayerModel().fireLayersChanged();
        layerPanel.rebuild();
    }

    // ---- File actions ----

    private void onNew() {
        NewDocumentDialog dlg = new NewDocumentDialog(this);
        dlg.setVisible(true);
        if (!dlg.isConfirmed()) return;

        doc = new CanvasDocument(dlg.getCanvasWidth(), dlg.getCanvasHeight());
        canvasPanel.setDoc(doc);
        rebuildLayers();
        updateDrawContext();
        fillDefaultBackground();
        currentFile = null;
        updateTitle();
        statusBar.setCanvasSize(doc.getWidth(), doc.getHeight());
    }

    private void onOpen() {
        BufferedImage img = ImageUtil.showOpenDialog(this);
        if (img == null) return;

        doc = new CanvasDocument(img.getWidth(), img.getHeight());
        Layer layer = doc.getLayerModel().getActive();
        Graphics2D g2 = layer.getImage().createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        canvasPanel.setDoc(doc);
        rebuildLayers();
        updateDrawContext();
        statusBar.setCanvasSize(doc.getWidth(), doc.getHeight());
        doc.markClean();
    }

    private void onSave() {
        if (currentFile == null) { onSaveAs(); return; }
        try {
            ImageUtil.saveAs(doc, currentFile);
            doc.markClean();
            updateTitle();
        } catch (IOException ex) {
            showError("Save failed: " + ex.getMessage());
        }
    }

    private void onSaveAs() {
        File f = ImageUtil.showSaveDialog(this);
        if (f == null) return;
        try {
            ImageUtil.saveAs(doc, f);
            currentFile = f;
            addRecentFile(f);
            doc.markClean();
            updateTitle();
        } catch (IOException ex) {
            showError("Save failed: " + ex.getMessage());
        }
    }

    private void onResizeCanvas() {
        CanvasSizeDialog dlg = new CanvasSizeDialog(this, doc.getWidth(), doc.getHeight());
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            doc.resize(dlg.getCanvasWidth(), dlg.getCanvasHeight());
            canvasPanel.setDoc(doc);
            statusBar.setCanvasSize(doc.getWidth(), doc.getHeight());
        }
    }

    private void flattenLayers() {
        BufferedImage composite = ImageUtil.composite(doc.getLayerModel(), doc.getWidth(), doc.getHeight());
        doc = new CanvasDocument(doc.getWidth(), doc.getHeight());
        Graphics2D g2 = doc.getLayerModel().getActive().getImage().createGraphics();
        g2.drawImage(composite, 0, 0, null);
        g2.dispose();
        canvasPanel.setDoc(doc);
        rebuildLayers();
        updateDrawContext();
    }

    private void rotateCanvas(boolean clockwise) {
        Layer active = doc.getLayerModel().getActive();
        if (active == null) return;
        BufferedImage src = active.getImage();
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage dst = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();
        if (clockwise) {
            g2.translate(h, 0);
            g2.rotate(Math.PI / 2);
        } else {
            g2.translate(0, w);
            g2.rotate(-Math.PI / 2);
        }
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        Graphics2D lg = active.getImage().createGraphics();
        lg.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        lg.fillRect(0, 0, w, h);
        lg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        // If dimensions changed we'd need to resize — for now just redraw if same size
        lg.drawImage(dst, 0, 0, w, h, null);
        lg.dispose();
        doc.getLayerModel().fireLayersChanged();
    }

    private void flipCanvas(boolean horizontal) {
        Layer active = doc.getLayerModel().getActive();
        if (active == null) return;
        BufferedImage img = active.getImage();
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dst.createGraphics();
        if (horizontal) {
            g2.translate(w, 0);
            g2.scale(-1, 1);
        } else {
            g2.translate(0, h);
            g2.scale(1, -1);
        }
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        Graphics2D lg = img.createGraphics();
        lg.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        lg.fillRect(0, 0, w, h);
        lg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        lg.drawImage(dst, 0, 0, null);
        lg.dispose();
        doc.getLayerModel().fireLayersChanged();
    }

    private void clearCanvas() {
        Layer active = doc.getLayerModel().getActive();
        if (active == null) return;
        int choice = JOptionPane.showConfirmDialog(this,
                "Clear active layer contents?", "Clear Layer", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        Graphics2D g2 = active.getImage().createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2.fillRect(0, 0, active.getImage().getWidth(), active.getImage().getHeight());
        g2.dispose();
        doc.getLayerModel().fireLayersChanged();
        doc.markDirty();
    }

    // ---- Select ----

    private void selectAll() {
        toolBar.selectTool("Select");
        RectSelectTool st = toolBar.getSelectTool();
        st.setFullSelection(doc.getWidth(), doc.getHeight());
        canvasPanel.updateOverlayPublic();
        canvasPanel.repaint();
    }

    private void deselect() {
        if (toolBar.getActiveTool() instanceof RectSelectTool) {
            toolBar.getSelectTool().deselect();
            canvasPanel.clearOverlay();
            canvasPanel.repaint();
        }
    }

    // ---- Filters ----

    private void filterBlur() {
        Layer active = doc.getLayerModel().getActive();
        if (active == null) return;
        String input = JOptionPane.showInputDialog(this, "Blur radius (1-20):", "Gaussian Blur", JOptionPane.PLAIN_MESSAGE);
        if (input == null) return;
        int radius;
        try { radius = Math.max(1, Math.min(20, Integer.parseInt(input.trim()))); }
        catch (NumberFormatException ex) { return; }

        BufferedImage before = com.draw.command.DrawStrokeCommand.snapshot(active);
        applyGaussianBlur(active.getImage(), radius);
        doc.getHistory().push(new com.draw.command.DrawStrokeCommand(active,
                before, com.draw.command.DrawStrokeCommand.snapshot(active)));
        doc.getLayerModel().fireLayersChanged();
        doc.markDirty();
    }

    private void applyGaussianBlur(BufferedImage img, int radius) {
        int size = radius * 2 + 1;
        float[] data = new float[size * size];
        float sigma = radius / 3f + 0.5f;
        float sum = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - radius, dy = y - radius;
                data[y * size + x] = (float)Math.exp(-(dx*dx + dy*dy) / (2 * sigma * sigma));
                sum += data[y * size + x];
            }
        }
        for (int i = 0; i < data.length; i++) data[i] /= sum;

        Kernel kernel = new Kernel(size, size, data);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        BufferedImage tmp = op.filter(img, null);
        Graphics2D g2 = img.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2.fillRect(0, 0, img.getWidth(), img.getHeight());
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        g2.drawImage(tmp, 0, 0, null);
        g2.dispose();
    }

    private void filterSharpen() {
        Layer active = doc.getLayerModel().getActive();
        if (active == null) return;
        BufferedImage before = com.draw.command.DrawStrokeCommand.snapshot(active);

        float[] sharpenData = {
            0f,  -1f,  0f,
           -1f,   5f, -1f,
            0f,  -1f,  0f
        };
        Kernel kernel = new Kernel(3, 3, sharpenData);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        BufferedImage img = active.getImage();
        BufferedImage tmp = op.filter(img, null);
        Graphics2D g2 = img.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2.fillRect(0, 0, img.getWidth(), img.getHeight());
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        g2.drawImage(tmp, 0, 0, null);
        g2.dispose();

        doc.getHistory().push(new com.draw.command.DrawStrokeCommand(active,
                before, com.draw.command.DrawStrokeCommand.snapshot(active)));
        doc.getLayerModel().fireLayersChanged();
        doc.markDirty();
    }

    private void filterInvert() {
        Layer active = doc.getLayerModel().getActive();
        if (active == null) return;
        BufferedImage before = com.draw.command.DrawStrokeCommand.snapshot(active);
        BufferedImage img = active.getImage();
        int w = img.getWidth(), h = img.getHeight();
        int[] pixels = new int[w * h];
        img.getRGB(0, 0, w, h, pixels, 0, w);
        for (int i = 0; i < pixels.length; i++) {
            int a = pixels[i] & 0xFF000000;
            pixels[i] = a | (~pixels[i] & 0x00FFFFFF);
        }
        img.setRGB(0, 0, w, h, pixels, 0, w);
        doc.getHistory().push(new com.draw.command.DrawStrokeCommand(active,
                before, com.draw.command.DrawStrokeCommand.snapshot(active)));
        doc.getLayerModel().fireLayersChanged();
        doc.markDirty();
    }

    private void filterGrayscale() {
        Layer active = doc.getLayerModel().getActive();
        if (active == null) return;
        BufferedImage before = com.draw.command.DrawStrokeCommand.snapshot(active);
        BufferedImage img = active.getImage();
        int w = img.getWidth(), h = img.getHeight();
        int[] pixels = new int[w * h];
        img.getRGB(0, 0, w, h, pixels, 0, w);
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int a = (p >>> 24) & 0xFF;
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8)  & 0xFF;
            int b =  p        & 0xFF;
            // Luminance weights: 0.299R + 0.587G + 0.114B
            int gray = (r * 77 + g * 150 + b * 29) >> 8;
            pixels[i] = (a << 24) | (gray << 16) | (gray << 8) | gray;
        }
        img.setRGB(0, 0, w, h, pixels, 0, w);
        doc.getHistory().push(new com.draw.command.DrawStrokeCommand(active,
                before, com.draw.command.DrawStrokeCommand.snapshot(active)));
        doc.getLayerModel().fireLayersChanged();
        doc.markDirty();
    }

    private void filterBrightnessContrast() {
        Layer active = doc.getLayerModel().getActive();
        if (active == null) return;

        JSlider brightnessSlider = new JSlider(-150, 150, 0);
        JSlider contrastSlider   = new JSlider(-100, 100, 0);
        brightnessSlider.setMajorTickSpacing(50);
        contrastSlider.setMajorTickSpacing(50);
        brightnessSlider.setPaintTicks(true); brightnessSlider.setPaintLabels(true);
        contrastSlider.setPaintTicks(true);   contrastSlider.setPaintLabels(true);

        JPanel panel = new JPanel(new GridLayout(4, 1, 4, 4));
        panel.add(new JLabel("Brightness:"));
        panel.add(brightnessSlider);
        panel.add(new JLabel("Contrast:"));
        panel.add(contrastSlider);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Brightness / Contrast", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        int bright = brightnessSlider.getValue();
        int contrast = contrastSlider.getValue();
        BufferedImage before = com.draw.command.DrawStrokeCommand.snapshot(active);
        applyBrightnessContrast(active.getImage(), bright, contrast);
        doc.getHistory().push(new com.draw.command.DrawStrokeCommand(active,
                before, com.draw.command.DrawStrokeCommand.snapshot(active)));
        doc.getLayerModel().fireLayersChanged();
        doc.markDirty();
    }

    private void applyBrightnessContrast(BufferedImage img, int brightness, int contrast) {
        int w = img.getWidth(), h = img.getHeight();
        int[] pixels = new int[w * h];
        img.getRGB(0, 0, w, h, pixels, 0, w);
        // Contrast factor
        double factor = (259.0 * (contrast + 255)) / (255.0 * (259 - contrast));
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int a = (p >>> 24) & 0xFF;
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8)  & 0xFF;
            int b =  p        & 0xFF;
            // Apply contrast then brightness
            r = clampChannel((int)(factor * (r - 128) + 128) + brightness);
            g = clampChannel((int)(factor * (g - 128) + 128) + brightness);
            b = clampChannel((int)(factor * (b - 128) + 128) + brightness);
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        img.setRGB(0, 0, w, h, pixels, 0, w);
    }

    private void filterHueSaturation() {
        Layer active = doc.getLayerModel().getActive();
        if (active == null) return;

        JSlider hueSlider  = new JSlider(-180, 180, 0);
        JSlider satSlider  = new JSlider(-100, 100, 0);
        JSlider lightSlider = new JSlider(-100, 100, 0);
        hueSlider.setMajorTickSpacing(60);   hueSlider.setPaintTicks(true); hueSlider.setPaintLabels(true);
        satSlider.setMajorTickSpacing(50);   satSlider.setPaintTicks(true); satSlider.setPaintLabels(true);
        lightSlider.setMajorTickSpacing(50); lightSlider.setPaintTicks(true); lightSlider.setPaintLabels(true);

        JPanel panel = new JPanel(new GridLayout(6, 1, 4, 4));
        panel.add(new JLabel("Hue shift:"));       panel.add(hueSlider);
        panel.add(new JLabel("Saturation:"));       panel.add(satSlider);
        panel.add(new JLabel("Lightness:"));        panel.add(lightSlider);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Hue / Saturation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        BufferedImage before = com.draw.command.DrawStrokeCommand.snapshot(active);
        applyHueSaturation(active.getImage(),
                hueSlider.getValue(), satSlider.getValue() / 100f, lightSlider.getValue() / 100f);
        doc.getHistory().push(new com.draw.command.DrawStrokeCommand(active,
                before, com.draw.command.DrawStrokeCommand.snapshot(active)));
        doc.getLayerModel().fireLayersChanged();
        doc.markDirty();
    }

    private void applyHueSaturation(BufferedImage img, int hueShift, float satDelta, float lightDelta) {
        int w = img.getWidth(), h = img.getHeight();
        int[] pixels = new int[w * h];
        img.getRGB(0, 0, w, h, pixels, 0, w);
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int a = (p >>> 24) & 0xFF;
            if (a == 0) continue;
            float r = ((p >> 16) & 0xFF) / 255f;
            float g = ((p >> 8)  & 0xFF) / 255f;
            float b = ( p        & 0xFF) / 255f;
            float[] hsb = Color.RGBtoHSB((int)(r*255), (int)(g*255), (int)(b*255), null);
            hsb[0] = ((hsb[0] + hueShift / 360f) % 1f + 1f) % 1f;
            hsb[1] = Math.max(0, Math.min(1, hsb[1] + satDelta));
            hsb[2] = Math.max(0, Math.min(1, hsb[2] + lightDelta));
            int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
            pixels[i] = (a << 24) | (rgb & 0x00FFFFFF);
        }
        img.setRGB(0, 0, w, h, pixels, 0, w);
    }

    private static int clampChannel(int v) { return v < 0 ? 0 : v > 255 ? 255 : v; }

    // ---- Helpers ----

    private void confirmAndExit() {
        if (doc.isDirty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Unsaved changes. Exit anyway?", "Confirm Exit", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;
        }
        System.exit(0);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void updateTitle() {
        String name = currentFile != null ? currentFile.getName() : "Untitled";
        setTitle("Draw — " + name + (doc.isDirty() ? " •" : ""));
    }

    private void rebuildLayers() {
        for (Component c : getContentPane().getComponents()) {
            if (c instanceof JPanel) {
                JPanel east = (JPanel) c;
                if (east.getPreferredSize().width == 270) {
                    // This is the right panel — find and replace Layers section
                    rebuildLayerPanelInside(east);
                    break;
                }
            }
        }
    }

    private void rebuildLayerPanelInside(JPanel rightPanel) {
        for (Component c : rightPanel.getComponents()) {
            if (c instanceof JPanel) {
                JPanel section = (JPanel) c;
                for (Component inner : section.getComponents()) {
                    if (inner instanceof LayerPanel) {
                        LayerPanel newLp = new LayerPanel(doc);
                        section.remove(inner);
                        section.add(newLp, BorderLayout.CENTER);
                        layerPanel = newLp;
                        section.revalidate();
                        section.repaint();
                        return;
                    }
                }
            }
        }
    }

    private void addRecentFile(File f) {
        recentFiles.remove(f);
        recentFiles.addFirst(f);
        while (recentFiles.size() > 5) recentFiles.removeLast();
        rebuildRecentMenu();
    }

    private void rebuildRecentMenu() {
        recentFilesMenu.removeAll();
        recentFilesMenu.setEnabled(!recentFiles.isEmpty());
        for (File f : recentFiles) {
            JMenuItem item = new JMenuItem(f.getName());
            item.setToolTipText(f.getAbsolutePath());
            item.addActionListener(e -> {
                try {
                    BufferedImage img = javax.imageio.ImageIO.read(f);
                    if (img != null) {
                        doc = new CanvasDocument(img.getWidth(), img.getHeight());
                        Graphics2D g2 = doc.getLayerModel().getActive().getImage().createGraphics();
                        g2.drawImage(img, 0, 0, null);
                        g2.dispose();
                        currentFile = f;
                        canvasPanel.setDoc(doc);
                        rebuildLayers();
                        updateDrawContext();
                        updateTitle();
                    }
                } catch (IOException ex) { showError("Cannot open: " + ex.getMessage()); }
            });
            recentFilesMenu.add(item);
        }
    }
}
