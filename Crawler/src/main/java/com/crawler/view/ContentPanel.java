package com.crawler.view;

import com.crawler.model.CrawlResult;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

public class ContentPanel extends JPanel {
    private final JFXPanel jfxPanel;
    private final JTextArea sourceArea;
    private final JTextArea textArea;
    private final JLabel infoLabel;
    private final DefaultListModel<String> linksModel;
    private final DefaultListModel<String> imagesModel;
    private WebEngine webEngine;
    private final JLabel webStatusLabel;
    private final JProgressBar webProgress;
    private final JLabel imagePreviewLabel;
    private final JLabel imageInfoLabel;
    private final ConcurrentHashMap<String, ImageIcon> imageCache = new ConcurrentHashMap<>();

    public ContentPanel() {
        setLayout(new BorderLayout());

        infoLabel = new JLabel("選擇一個結果以檢視內容");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        infoLabel.setForeground(new Color(0x7f848e));

        webStatusLabel = new JLabel(" ");
        webStatusLabel.setForeground(new Color(0x7f848e));
        webStatusLabel.setFont(webStatusLabel.getFont().deriveFont(11f));
        webProgress = new JProgressBar();
        webProgress.setPreferredSize(new Dimension(0, 3));
        webProgress.setVisible(false);

        jfxPanel = new JFXPanel();
        Platform.runLater(() -> {
            WebView webView = new WebView();
            webEngine = webView.getEngine();
            webEngine.setJavaScriptEnabled(true);
            webEngine.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                SwingUtilities.invokeLater(() -> {
                    if (newState == Worker.State.RUNNING) {
                        webStatusLabel.setText("載入中...");
                        webProgress.setIndeterminate(true);
                        webProgress.setVisible(true);
                    } else if (newState == Worker.State.SUCCEEDED) {
                        webStatusLabel.setText("載入完成");
                        webProgress.setVisible(false);
                    } else if (newState == Worker.State.FAILED) {
                        webStatusLabel.setText("載入失敗");
                        webProgress.setVisible(false);
                    } else {
                        webProgress.setVisible(false);
                    }
                });
            });

            Scene scene = new Scene(webView);
            jfxPanel.setScene(scene);
        });

        sourceArea = new JTextArea();
        sourceArea.setEditable(false);
        sourceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        sourceArea.setTabSize(2);

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        linksModel = new DefaultListModel<>();
        JList<String> linksList = new JList<>(linksModel);
        linksList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        linksList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String url = linksList.getSelectedValue();
                    if (url != null) {
                        try { Desktop.getDesktop().browse(new java.net.URI(url)); }
                        catch (Exception ignored) {}
                    }
                }
            }
        });
        JPanel linksPanel = new JPanel(new BorderLayout());
        JLabel linksHeader = new JLabel(" 雙擊可在瀏覽器中開啟");
        linksHeader.setForeground(new Color(0x7f848e));
        linksPanel.add(linksHeader, BorderLayout.NORTH);
        linksPanel.add(new JScrollPane(linksList), BorderLayout.CENTER);

        imagesModel = new DefaultListModel<>();
        JList<String> imagesList = new JList<>(imagesModel);
        imagesList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        imagesList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String url = imagesList.getSelectedValue();
                    if (url != null) {
                        try { Desktop.getDesktop().browse(new java.net.URI(url)); }
                        catch (Exception ignored) {}
                    }
                }
            }
        });
        imagesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = imagesList.getSelectedValue();
                if (selected != null) {
                    loadImagePreview(selected);
                }
            }
        });

        imagePreviewLabel = new JLabel("選擇圖片以預覽", SwingConstants.CENTER);
        imagePreviewLabel.setForeground(new Color(0x7f848e));
        imagePreviewLabel.setVerticalAlignment(SwingConstants.CENTER);

        imageInfoLabel = new JLabel(" ");
        imageInfoLabel.setForeground(new Color(0x7f848e));
        imageInfoLabel.setFont(imageInfoLabel.getFont().deriveFont(11f));
        imageInfoLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JPanel imagePreviewPanel = new JPanel(new BorderLayout());
        imagePreviewPanel.add(new JScrollPane(imagePreviewLabel), BorderLayout.CENTER);
        imagePreviewPanel.add(imageInfoLabel, BorderLayout.SOUTH);

        JPanel imagesListPanel = new JPanel(new BorderLayout());
        JLabel imagesHeader = new JLabel(" 雙擊可在瀏覽器開啟 | 點擊預覽");
        imagesHeader.setForeground(new Color(0x7f848e));
        imagesListPanel.add(imagesHeader, BorderLayout.NORTH);
        imagesListPanel.add(new JScrollPane(imagesList), BorderLayout.CENTER);

        JSplitPane imagesSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                imagesListPanel, imagePreviewPanel);
        imagesSplit.setDividerLocation(400);
        imagesSplit.setResizeWeight(0.4);

        JPanel imagesPanel = new JPanel(new BorderLayout());
        imagesPanel.add(imagesSplit, BorderLayout.CENTER);

        JPanel webPanel = new JPanel(new BorderLayout());
        JPanel webBottomBar = new JPanel(new BorderLayout());
        webBottomBar.add(webStatusLabel, BorderLayout.WEST);
        webBottomBar.add(webProgress, BorderLayout.CENTER);
        webPanel.add(jfxPanel, BorderLayout.CENTER);
        webPanel.add(webBottomBar, BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("渲染預覽", webPanel);
        tabs.addTab("HTML 原始碼", new JScrollPane(sourceArea));
        tabs.addTab("純文字", new JScrollPane(textArea));
        tabs.addTab("連結列表", linksPanel);
        tabs.addTab("圖片列表", imagesPanel);

        add(infoLabel, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    public void showContent(CrawlResult result) {
        if (result == null) {
            clear();
            return;
        }

        infoLabel.setText(String.format("URL: %s | 狀態: %d | 標題: %s | 大小: %s",
                result.getUrl(), result.getStatusCode(),
                result.getTitle() != null ? result.getTitle() : "",
                formatSize(result.getContentLength())));

        Platform.runLater(() -> {
            if (webEngine != null) {
                webEngine.load(result.getUrl());
            }
        });

        String html = result.getHtmlContent();
        sourceArea.setText(html != null ? html : "");
        sourceArea.setCaretPosition(0);

        textArea.setText(result.getTextContent() != null ? result.getTextContent() : "");
        textArea.setCaretPosition(0);

        linksModel.clear();
        for (String link : result.getLinks()) {
            linksModel.addElement(link);
        }

        imagesModel.clear();
        for (String img : result.getImages()) {
            imagesModel.addElement(img);
        }
    }

    public void clear() {
        infoLabel.setText("選擇一個結果以檢視內容");
        Platform.runLater(() -> {
            if (webEngine != null) {
                webEngine.loadContent("");
            }
        });
        sourceArea.setText("");
        textArea.setText("");
        linksModel.clear();
        imagesModel.clear();
        imagePreviewLabel.setIcon(null);
        imagePreviewLabel.setText("選擇圖片以預覽");
        imageInfoLabel.setText(" ");
        imageCache.clear();
    }

    private void loadImagePreview(String imgUrl) {
        imagePreviewLabel.setText("載入中...");
        imagePreviewLabel.setIcon(null);
        imageInfoLabel.setText(imgUrl);

        ImageIcon cached = imageCache.get(imgUrl);
        if (cached != null) {
            showScaledImage(cached, imgUrl);
            return;
        }

        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() {
                try {
                    BufferedImage img = ImageIO.read(new URL(imgUrl));
                    if (img != null) {
                        return new ImageIcon(img);
                    }
                } catch (Exception ignored) {
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        imageCache.put(imgUrl, icon);
                        showScaledImage(icon, imgUrl);
                    } else {
                        imagePreviewLabel.setText("無法載入圖片");
                        imagePreviewLabel.setIcon(null);
                    }
                } catch (Exception e) {
                    imagePreviewLabel.setText("載入失敗");
                    imagePreviewLabel.setIcon(null);
                }
            }
        }.execute();
    }

    private void showScaledImage(ImageIcon icon, String imgUrl) {
        int origW = icon.getIconWidth();
        int origH = icon.getIconHeight();

        JScrollPane parent = (JScrollPane) imagePreviewLabel.getParent().getParent();
        int maxW = Math.max(200, parent.getViewport().getWidth() - 20);
        int maxH = Math.max(200, parent.getViewport().getHeight() - 20);

        int w = origW;
        int h = origH;
        if (w > maxW || h > maxH) {
            double scale = Math.min((double) maxW / w, (double) maxH / h);
            w = (int) (w * scale);
            h = (int) (h * scale);
        }

        Image scaled = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        imagePreviewLabel.setIcon(new ImageIcon(scaled));
        imagePreviewLabel.setText(null);
        imageInfoLabel.setText(String.format("%s  |  %d x %d px", imgUrl, origW, origH));
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
