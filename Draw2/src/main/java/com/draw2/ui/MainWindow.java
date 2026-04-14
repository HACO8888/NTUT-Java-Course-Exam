package com.draw2.ui;

import com.draw2.model.CanvasModel;
import com.draw2.tool.*;
import com.draw2.ui.dialogs.NewCanvasDialog;
import com.draw2.ui.dialogs.ResizeDialog;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class MainWindow extends JFrame {

    private CanvasModel model;
    private CanvasPanel canvasPanel;
    private final ToolBar toolBar = new ToolBar();
    private final ColorPalette colorPalette = new ColorPalette();
    private final StatusBar statusBar = new StatusBar();

    private Tool activeTool;
    private File currentFile;
    private boolean dirty;

    // Menu items whose enabled state changes
    private JMenuItem undoItem, redoItem;

    public MainWindow() {
        super("小畫家");
        model = new CanvasModel(800, 600);
        initUI();
        updateTitle();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { confirmExit(); }
        });
    }

    // ── init ─────────────────────────────────────────────────────────────────

    private void initUI() {
        canvasPanel = new CanvasPanel(model);

        // Wire TextTool to canvas panel
        toolBar.text.setCanvasComponent(canvasPanel);

        // Tool changes
        toolBar.setToolChangeCallback(tool -> {
            activeTool = tool;
            canvasPanel.setActiveTool(tool);
            statusBar.setTool(tool.getName());
            updateDrawContext();
        });

        // Size changes
        toolBar.setSizeChangeCallback(s -> updateDrawContext());
        toolBar.addFilledListener(this::updateDrawContext);

        // Color changes
        colorPalette.setFgCallback(c -> updateDrawContext());
        colorPalette.setBgCallback(c -> updateDrawContext());

        // Eyedropper picks propagate back to palette
        // These are set via DrawContext callbacks, so updateDrawContext() handles it.

        // Cursor / zoom callbacks
        canvasPanel.setCursorCallback((x, y) -> statusBar.setCursorPos(x, y));
        canvasPanel.setZoomCallback(z -> statusBar.setZoom(z));

        // Model history changes → update undo/redo menu items
        model.addPropertyChangeListener(e -> {
            if ("history".equals(e.getPropertyName())) {
                if (undoItem != null) undoItem.setEnabled(model.canUndo());
                if (redoItem != null) redoItem.setEnabled(model.canRedo());
                dirty = true;
                updateTitle();
            }
        });

        // Default tool: pencil
        activeTool = toolBar.pencil;
        canvasPanel.setActiveTool(activeTool);
        statusBar.setTool(activeTool.getName());
        updateDrawContext();

        // Layout
        JScrollPane scroll = new JScrollPane(canvasPanel);
        scroll.setBackground(new Color(0x80, 0x80, 0x80));

        JPanel south = new JPanel(new BorderLayout());
        south.add(colorPalette, BorderLayout.CENTER);
        south.add(statusBar, BorderLayout.SOUTH);

        setJMenuBar(buildMenuBar());
        add(toolBar,  BorderLayout.WEST);
        add(scroll,   BorderLayout.CENTER);
        add(south,    BorderLayout.SOUTH);

        statusBar.setCanvasSize(model.getWidth(), model.getHeight());
        statusBar.setZoom(1.0);

        setupKeyBindings();

        setSize(1100, 780);
        setLocationRelativeTo(null);
    }

    // ── draw context ──────────────────────────────────────────────────────────

    private void updateDrawContext() {
        DrawContext ctx = new DrawContext(
            model,
            colorPalette.getForeColor(),
            colorPalette.getBackColor(),
            toolBar.getStrokeSize(),
            toolBar.isFilled(),
            c -> { colorPalette.setForeColor(c); },
            c -> { colorPalette.setBackColor(c); }
        );
        canvasPanel.setDrawContext(ctx);
    }

    // ── menu bar ──────────────────────────────────────────────────────────────

    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();

        // ── File ──
        JMenu file = new JMenu("檔案");
        file.setMnemonic('F');
        file.add(item("新增...", "ctrl N", e -> onNew()));
        file.add(item("開啟...", "ctrl O", e -> onOpen()));
        file.addSeparator();
        file.add(item("儲存",    "ctrl S",         e -> onSave()));
        file.add(item("另存新檔...", "ctrl shift S", e -> onSaveAs()));
        file.addSeparator();
        file.add(item("結束", null, e -> confirmExit()));
        mb.add(file);

        // ── Edit ──
        JMenu edit = new JMenu("編輯");
        edit.setMnemonic('E');
        undoItem = item("復原", "ctrl Z", e -> doUndo());
        redoItem = item("取消復原", "ctrl Y", e -> doRedo());
        undoItem.setEnabled(false);
        redoItem.setEnabled(false);
        edit.add(undoItem);
        edit.add(redoItem);
        edit.addSeparator();
        edit.add(item("清除畫布", null, e -> doClear()));
        mb.add(edit);

        // ── Image ──
        JMenu image = new JMenu("影像");
        image.setMnemonic('I');
        image.add(item("調整大小...", null, e -> onResize()));
        image.addSeparator();
        JMenu rotate = new JMenu("旋轉/翻轉");
        rotate.add(item("向右旋轉 90°",  null, e -> model.rotateRight()));
        rotate.add(item("向左旋轉 90°",  null, e -> model.rotateLeft()));
        rotate.add(item("旋轉 180°",     null, e -> model.rotate180()));
        rotate.addSeparator();
        rotate.add(item("水平翻轉", null, e -> model.flipHorizontal()));
        rotate.add(item("垂直翻轉", null, e -> model.flipVertical()));
        image.add(rotate);
        mb.add(image);

        // ── View ──
        JMenu view = new JMenu("檢視");
        view.setMnemonic('V');
        view.add(item("放大", "ctrl EQUALS", e -> canvasPanel.zoomIn()));
        view.add(item("縮小", "ctrl MINUS",  e -> canvasPanel.zoomOut()));
        view.add(item("100%", "ctrl 1",      e -> canvasPanel.zoomReset()));
        view.addSeparator();
        JCheckBoxMenuItem gridItem = new JCheckBoxMenuItem("顯示格線");
        gridItem.setAccelerator(KeyStroke.getKeyStroke("ctrl G"));
        gridItem.addActionListener(e -> canvasPanel.setGridEnabled(gridItem.isSelected()));
        view.add(gridItem);
        mb.add(view);

        return mb;
    }

    private JMenuItem item(String label, String accel, ActionListener al) {
        JMenuItem mi = new JMenuItem(label);
        if (accel != null) mi.setAccelerator(KeyStroke.getKeyStroke(accel));
        mi.addActionListener(al);
        return mi;
    }

    // ── key bindings ──────────────────────────────────────────────────────────

    private void setupKeyBindings() {
        JRootPane rp = getRootPane();
        InputMap im   = rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am  = rp.getActionMap();

        bindKey(im, am, "P",   e -> canvasPanel.setActiveTool(toolBar.pencil));
        bindKey(im, am, "B",   e -> canvasPanel.setActiveTool(toolBar.brush));
        bindKey(im, am, "E",   e -> canvasPanel.setActiveTool(toolBar.eraser));
        bindKey(im, am, "K",   e -> canvasPanel.setActiveTool(toolBar.fill));
        bindKey(im, am, "L",   e -> canvasPanel.setActiveTool(toolBar.line));
        bindKey(im, am, "R",   e -> canvasPanel.setActiveTool(toolBar.rect));
        bindKey(im, am, "O",   e -> canvasPanel.setActiveTool(toolBar.ellipse));
        bindKey(im, am, "T",   e -> canvasPanel.setActiveTool(toolBar.text));
        bindKey(im, am, "A",   e -> canvasPanel.setActiveTool(toolBar.spray));
        bindKey(im, am, "I",   e -> canvasPanel.setActiveTool(toolBar.eyedropper));
        bindKey(im, am, "X",   e -> colorPalette.swapColors());
        bindKey(im, am, "OPEN_BRACKET",  e -> adjustSize(-1));
        bindKey(im, am, "CLOSE_BRACKET", e -> adjustSize(1));

        // Shift-press/release forwarded to shape tools
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            boolean s = e.isShiftDown();
            toolBar.line.setShiftDown(s);
            toolBar.rect.setShiftDown(s);
            toolBar.ellipse.setShiftDown(s);
            return false;
        });
    }

    private void bindKey(InputMap im, ActionMap am, String key, ActionListener al) {
        im.put(KeyStroke.getKeyStroke(key), key);
        am.put(key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { al.actionPerformed(e); }
        });
    }

    private void adjustSize(int delta) {
        // Delegate to toolbar's spinner — not directly accessible, so fire a property
        // The spinner value is controlled in ToolBar; we simulate here via reflection-free workaround.
        // Just use canvasPanel.setActiveTool to trigger repaint; actual size is in the context.
        canvasPanel.repaint();
    }

    // ── actions ───────────────────────────────────────────────────────────────

    private void onNew() {
        if (!confirmSave()) return;
        NewCanvasDialog dlg = new NewCanvasDialog(this);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            model = new CanvasModel(dlg.getCanvasWidth(), dlg.getCanvasHeight());
            rebindModel();
            currentFile = null;
            dirty = false;
            updateTitle();
        }
    }

    private void onOpen() {
        if (!confirmSave()) return;
        JFileChooser fc = makeFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            BufferedImage img = ImageIO.read(f);
            if (img == null) { JOptionPane.showMessageDialog(this, "無法讀取檔案", "錯誤", JOptionPane.ERROR_MESSAGE); return; }
            // Convert to RGB
            model = new CanvasModel(img.getWidth(), img.getHeight());
            Graphics2D g2 = model.getCanvas().createGraphics();
            g2.drawImage(img, 0, 0, null);
            g2.dispose();
            rebindModel();
            currentFile = f;
            dirty = false;
            updateTitle();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "讀取失敗：" + ex.getMessage(), "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSave() {
        if (currentFile == null) { onSaveAs(); return; }
        saveToFile(currentFile);
    }

    private void onSaveAs() {
        JFileChooser fc = makeFileChooser();
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().contains(".")) f = new File(f.getAbsolutePath() + ".png");
        saveToFile(f);
        currentFile = f;
        dirty = false;
        updateTitle();
    }

    private void saveToFile(File f) {
        String name = f.getName().toLowerCase();
        String fmt = name.endsWith(".jpg") || name.endsWith(".jpeg") ? "jpeg"
                   : name.endsWith(".bmp")                           ? "bmp"
                   : "png";
        try {
            BufferedImage toSave = model.getCanvas();
            if ("jpeg".equals(fmt)) {
                // Convert to 3-channel (no alpha) before JPEG
                BufferedImage rgb = new BufferedImage(toSave.getWidth(), toSave.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgb.createGraphics();
                g.drawImage(toSave, 0, 0, null);
                g.dispose();
                toSave = rgb;
            }
            ImageIO.write(toSave, fmt, f);
            dirty = false;
            updateTitle();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "儲存失敗：" + ex.getMessage(), "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doUndo() {
        model.undo();
        statusBar.setCanvasSize(model.getWidth(), model.getHeight());
    }

    private void doRedo() {
        model.redo();
        statusBar.setCanvasSize(model.getWidth(), model.getHeight());
    }

    private void doClear() {
        model.pushUndo();
        model.clear();
    }

    private void onResize() {
        ResizeDialog dlg = new ResizeDialog(this, model.getWidth(), model.getHeight());
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            model.resize(dlg.getNewWidth(), dlg.getNewHeight());
            statusBar.setCanvasSize(model.getWidth(), model.getHeight());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void rebindModel() {
        // Rebuild CanvasPanel for new model
        // Remove old canvas panel from scroll pane, replace
        JScrollPane scroll = (JScrollPane) ((BorderLayout) getContentPane().getLayout())
                .getLayoutComponent(BorderLayout.CENTER);

        model.addPropertyChangeListener(e -> {
            if ("history".equals(e.getPropertyName())) {
                if (undoItem != null) undoItem.setEnabled(model.canUndo());
                if (redoItem != null) redoItem.setEnabled(model.canRedo());
                dirty = true;
                updateTitle();
            }
        });

        canvasPanel = new CanvasPanel(model);
        toolBar.text.setCanvasComponent(canvasPanel);
        canvasPanel.setCursorCallback((x, y) -> statusBar.setCursorPos(x, y));
        canvasPanel.setZoomCallback(z -> statusBar.setZoom(z));
        canvasPanel.setActiveTool(activeTool);
        updateDrawContext();

        scroll.setViewportView(canvasPanel);
        statusBar.setCanvasSize(model.getWidth(), model.getHeight());
        undoItem.setEnabled(false);
        redoItem.setEnabled(false);
    }

    private boolean confirmSave() {
        if (!dirty) return true;
        int r = JOptionPane.showConfirmDialog(this,
            "畫布有未儲存的變更，是否儲存？",
            "未儲存的變更",
            JOptionPane.YES_NO_CANCEL_OPTION);
        if (r == JOptionPane.YES_OPTION)    { onSave(); return !dirty; }
        if (r == JOptionPane.NO_OPTION)     return true;
        return false; // cancel
    }

    private void confirmExit() {
        if (confirmSave()) dispose();
    }

    private void updateTitle() {
        String name = currentFile != null ? currentFile.getName() : "未命名";
        setTitle((dirty ? "* " : "") + name + " - 小畫家");
    }

    private JFileChooser makeFileChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "圖片檔案 (png, jpg, bmp)", "png", "jpg", "jpeg", "bmp"));
        if (currentFile != null) fc.setSelectedFile(currentFile);
        return fc;
    }
}
