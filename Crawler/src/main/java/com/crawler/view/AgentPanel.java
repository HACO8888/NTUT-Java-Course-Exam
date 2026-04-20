package com.crawler.view;

import com.crawler.engine.AgentService;
import com.crawler.engine.AgentService.Model;
import com.crawler.engine.ApiKeyStore;
import com.crawler.model.CrawlResult;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgentPanel extends JPanel {
    private final AgentService agentService;
    private final JTextPane chatPane;
    private final StyledDocument chatDoc;
    private final JTextField inputField;
    private final JButton sendBtn;
    private final JButton clearBtn;
    private final JButton apiKeyBtn;
    private Supplier<List<CrawlResult>> resultsSupplier;
    private Consumer<String> onNavigateToUrl;

    private final SimpleAttributeSet userStyle;
    private final SimpleAttributeSet agentStyle;
    private final SimpleAttributeSet systemStyle;
    private final SimpleAttributeSet labelStyle;
    private final SimpleAttributeSet linkStyle;
    private final SimpleAttributeSet boldStyle;
    private final SimpleAttributeSet italicStyle;
    private final SimpleAttributeSet boldItalicStyle;
    private final SimpleAttributeSet codeStyle;
    private final SimpleAttributeSet codeBlockStyle;
    private final SimpleAttributeSet h1Style;
    private final SimpleAttributeSet h2Style;
    private final SimpleAttributeSet h3Style;
    private final SimpleAttributeSet bulletStyle;
    private final SimpleAttributeSet hrStyle;

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)");

    public AgentPanel(AgentService agentService) {
        this.agentService = agentService;
        setLayout(new BorderLayout());

        userStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(userStyle, new Color(0x61AFEF));
        StyleConstants.setFontSize(userStyle, 14);

        agentStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(agentStyle, new Color(0xABB2BF));
        StyleConstants.setFontSize(agentStyle, 14);

        systemStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(systemStyle, new Color(0x5C6370));
        StyleConstants.setItalic(systemStyle, true);
        StyleConstants.setFontSize(systemStyle, 12);

        labelStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(labelStyle, new Color(0x98C379));
        StyleConstants.setBold(labelStyle, true);
        StyleConstants.setFontSize(labelStyle, 14);

        linkStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(linkStyle, new Color(0x61AFEF));
        StyleConstants.setUnderline(linkStyle, true);
        StyleConstants.setFontSize(linkStyle, 14);

        boldStyle = new SimpleAttributeSet(agentStyle);
        StyleConstants.setBold(boldStyle, true);

        italicStyle = new SimpleAttributeSet(agentStyle);
        StyleConstants.setItalic(italicStyle, true);

        boldItalicStyle = new SimpleAttributeSet(agentStyle);
        StyleConstants.setBold(boldItalicStyle, true);
        StyleConstants.setItalic(boldItalicStyle, true);

        codeStyle = new SimpleAttributeSet();
        StyleConstants.setFontFamily(codeStyle, Font.MONOSPACED);
        StyleConstants.setForeground(codeStyle, new Color(0xE5C07B));
        StyleConstants.setBackground(codeStyle, new Color(0x2C313A));
        StyleConstants.setFontSize(codeStyle, 13);

        codeBlockStyle = new SimpleAttributeSet();
        StyleConstants.setFontFamily(codeBlockStyle, Font.MONOSPACED);
        StyleConstants.setForeground(codeBlockStyle, new Color(0x98C379));
        StyleConstants.setBackground(codeBlockStyle, new Color(0x2C313A));
        StyleConstants.setFontSize(codeBlockStyle, 13);

        h1Style = new SimpleAttributeSet();
        StyleConstants.setForeground(h1Style, new Color(0xE06C75));
        StyleConstants.setBold(h1Style, true);
        StyleConstants.setFontSize(h1Style, 20);

        h2Style = new SimpleAttributeSet();
        StyleConstants.setForeground(h2Style, new Color(0xE06C75));
        StyleConstants.setBold(h2Style, true);
        StyleConstants.setFontSize(h2Style, 17);

        h3Style = new SimpleAttributeSet();
        StyleConstants.setForeground(h3Style, new Color(0xE06C75));
        StyleConstants.setBold(h3Style, true);
        StyleConstants.setFontSize(h3Style, 15);

        bulletStyle = new SimpleAttributeSet(agentStyle);
        StyleConstants.setForeground(bulletStyle, new Color(0xC678DD));

        hrStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(hrStyle, new Color(0x5C6370));
        StyleConstants.setFontSize(hrStyle, 8);

        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("  \u2728 AI 助理 — 詢問爬取結果相關問題");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setForeground(new Color(0x61AFEF));

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        apiKeyBtn = new JButton("\u2699 API Key");
        apiKeyBtn.setToolTipText("設定 OpenAI API Key");
        apiKeyBtn.addActionListener(e -> promptApiKey());
        JComboBox<Model> modelCombo = new JComboBox<>(Model.values());
        modelCombo.setSelectedItem(agentService.getModel());
        modelCombo.setPreferredSize(new Dimension(160, 28));
        modelCombo.addActionListener(e -> {
            agentService.setModel((Model) modelCombo.getSelectedItem());
            appendSystem("模型已切換為: " + modelCombo.getSelectedItem());
        });

        clearBtn = new JButton("清除對話");
        clearBtn.addActionListener(e -> clearChat());
        headerButtons.add(new JLabel("模型: "));
        headerButtons.add(modelCombo);
        headerButtons.add(apiKeyBtn);
        headerButtons.add(clearBtn);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(headerButtons, BorderLayout.EAST);

        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(new Color(0x1e1e1e));
        chatPane.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        chatDoc = chatPane.getStyledDocument();

        chatPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String url = getUrlAtPoint(e.getPoint());
                if (url != null) {
                    try {
                        Desktop.getDesktop().browse(new java.net.URI(url));
                    } catch (Exception ignored) {
                    }
                }
            }
        });

        chatPane.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                String url = getUrlAtPoint(e.getPoint());
                chatPane.setCursor(url != null
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
            }
        });

        JScrollPane chatScroll = new JScrollPane(chatPane);

        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        inputField = new JTextField();
        inputField.setFont(inputField.getFont().deriveFont(14f));
        inputField.putClientProperty("JTextField.placeholderText",
                "輸入問題，例如：哪個頁面有最多連結？找出關於招生的頁面...");
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && sendBtn.isEnabled()) {
                    sendMessage();
                }
            }
        });

        sendBtn = new JButton("發送");
        sendBtn.setBackground(new Color(0x0078D4));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFont(sendBtn.getFont().deriveFont(Font.BOLD, 14f));
        sendBtn.setPreferredSize(new Dimension(80, 32));
        sendBtn.addActionListener(e -> sendMessage());

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(chatScroll, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        loadApiKey();

        appendSystem("歡迎使用 AI 助理！\n"
                + "爬取完成後，你可以詢問任何關於爬取結果的問題，例如：\n"
                + "  • 哪個頁面有最多連結？\n"
                + "  • 幫我找出所有包含「招生」的頁面\n"
                + "  • 這個網站的結構是什麼？\n"
                + "  • 有哪些頁面回傳錯誤？\n"
                + "  • 幫我總結這個網站的內容\n");
    }

    public void setResultsSupplier(Supplier<List<CrawlResult>> supplier) {
        this.resultsSupplier = supplier;
    }

    public void setOnNavigateToUrl(Consumer<String> callback) {
        this.onNavigateToUrl = callback;
    }

    private void sendMessage() {
        String question = inputField.getText().trim();
        if (question.isEmpty()) return;

        inputField.setText("");
        appendUser(question);
        setInputEnabled(false);

        List<CrawlResult> results = resultsSupplier != null ? resultsSupplier.get() : Collections.emptyList();

        agentService.askAsync(question, results,
                response -> SwingUtilities.invokeLater(() -> {
                    appendAgent(response);
                    setInputEnabled(true);
                    inputField.requestFocusInWindow();
                }),
                error -> SwingUtilities.invokeLater(() -> {
                    appendSystem("錯誤: " + error);
                    setInputEnabled(true);
                    inputField.requestFocusInWindow();
                })
        );
    }

    private void appendUser(String text) {
        try {
            chatDoc.insertString(chatDoc.getLength(), "\n你: ", labelStyle);
            insertWithLinks(text, userStyle);
            chatDoc.insertString(chatDoc.getLength(), "\n", userStyle);
            scrollToBottom();
        } catch (BadLocationException ignored) {
        }
    }

    private void appendAgent(String text) {
        try {
            chatDoc.insertString(chatDoc.getLength(), "\nAI: ", labelStyle);
            insertMarkdown(text);
            chatDoc.insertString(chatDoc.getLength(), "\n", agentStyle);
            scrollToBottom();
        } catch (BadLocationException ignored) {
        }
    }

    private void appendSystem(String text) {
        try {
            chatDoc.insertString(chatDoc.getLength(), text + "\n", systemStyle);
            scrollToBottom();
        } catch (BadLocationException ignored) {
        }
    }

    private void insertWithLinks(String text, SimpleAttributeSet baseStyle) throws BadLocationException {
        Matcher matcher = URL_PATTERN.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                chatDoc.insertString(chatDoc.getLength(),
                        text.substring(lastEnd, matcher.start()), baseStyle);
            }

            String url = matcher.group(1);
            SimpleAttributeSet urlAttr = new SimpleAttributeSet(linkStyle);
            urlAttr.addAttribute("url", url);
            chatDoc.insertString(chatDoc.getLength(), url, urlAttr);

            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            chatDoc.insertString(chatDoc.getLength(),
                    text.substring(lastEnd), baseStyle);
        }
    }

    private void insertMarkdown(String text) throws BadLocationException {
        String[] lines = text.split("\n", -1);
        boolean inCodeBlock = false;
        StringBuilder codeBlockBuf = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (line.startsWith("```")) {
                if (!inCodeBlock) {
                    inCodeBlock = true;
                    codeBlockBuf.setLength(0);
                } else {
                    inCodeBlock = false;
                    chatDoc.insertString(chatDoc.getLength(), codeBlockBuf.toString(), codeBlockStyle);
                }
                continue;
            }

            if (inCodeBlock) {
                if (codeBlockBuf.length() > 0) codeBlockBuf.append("\n");
                codeBlockBuf.append(line);
                continue;
            }

            if (i > 0) {
                chatDoc.insertString(chatDoc.getLength(), "\n", agentStyle);
            }

            if (line.matches("^---+$") || line.matches("^\\*\\*\\*+$") || line.matches("^___+$")) {
                chatDoc.insertString(chatDoc.getLength(), "────────────────────────────────", hrStyle);
                continue;
            }

            if (line.startsWith("### ")) {
                insertInlineMarkdown(line.substring(4), h3Style);
                continue;
            }
            if (line.startsWith("## ")) {
                insertInlineMarkdown(line.substring(3), h2Style);
                continue;
            }
            if (line.startsWith("# ")) {
                insertInlineMarkdown(line.substring(2), h1Style);
                continue;
            }

            if (line.matches("^\\s*[-*+] .*")) {
                String content = line.replaceFirst("^(\\s*)[-*+] ", "");
                String indent = line.replaceFirst("[-*+] .*", "");
                chatDoc.insertString(chatDoc.getLength(), indent + "  • ", bulletStyle);
                insertInlineMarkdown(content, agentStyle);
                continue;
            }

            if (line.matches("^\\s*\\d+\\. .*")) {
                String numPart = line.replaceFirst("^(\\s*\\d+\\.) .*", "$1");
                String content = line.replaceFirst("^\\s*\\d+\\. ", "");
                chatDoc.insertString(chatDoc.getLength(), "  " + numPart + " ", bulletStyle);
                insertInlineMarkdown(content, agentStyle);
                continue;
            }

            insertInlineMarkdown(line, agentStyle);
        }

        if (inCodeBlock && codeBlockBuf.length() > 0) {
            chatDoc.insertString(chatDoc.getLength(), codeBlockBuf.toString(), codeBlockStyle);
        }
    }

    private void insertInlineMarkdown(String text, SimpleAttributeSet baseStyle) throws BadLocationException {
        Pattern inlinePattern = Pattern.compile(
                "(`[^`]+`)" +
                "|(\\*\\*\\*[^*]+\\*\\*\\*)" +
                "|(\\*\\*[^*]+\\*\\*)" +
                "|(\\*[^*]+\\*)" +
                "|(___[^_]+___)" +
                "|(__[^_]+__)" +
                "|(_[^_]+_)" +
                "|(\\[[^\\]]+\\]\\(https?://[^)]+\\))" +
                "|(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)"
        );

        Matcher matcher = inlinePattern.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                chatDoc.insertString(chatDoc.getLength(),
                        text.substring(lastEnd, matcher.start()), baseStyle);
            }

            String match = matcher.group();

            if (match.startsWith("`")) {
                chatDoc.insertString(chatDoc.getLength(),
                        match.substring(1, match.length() - 1), codeStyle);
            } else if (match.startsWith("***") || match.startsWith("___")) {
                chatDoc.insertString(chatDoc.getLength(),
                        match.substring(3, match.length() - 3), boldItalicStyle);
            } else if (match.startsWith("**") || match.startsWith("__")) {
                chatDoc.insertString(chatDoc.getLength(),
                        match.substring(2, match.length() - 2), boldStyle);
            } else if (match.startsWith("*") || match.startsWith("_")) {
                chatDoc.insertString(chatDoc.getLength(),
                        match.substring(1, match.length() - 1), italicStyle);
            } else if (match.startsWith("[")) {
                int closeBracket = match.indexOf("](");
                String linkText = match.substring(1, closeBracket);
                String url = match.substring(closeBracket + 2, match.length() - 1);
                SimpleAttributeSet urlAttr = new SimpleAttributeSet(linkStyle);
                urlAttr.addAttribute("url", url);
                chatDoc.insertString(chatDoc.getLength(), linkText, urlAttr);
            } else if (match.startsWith("http")) {
                SimpleAttributeSet urlAttr = new SimpleAttributeSet(linkStyle);
                urlAttr.addAttribute("url", match);
                chatDoc.insertString(chatDoc.getLength(), match, urlAttr);
            }

            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            chatDoc.insertString(chatDoc.getLength(),
                    text.substring(lastEnd), baseStyle);
        }
    }

    private String getUrlAtPoint(Point pt) {
        int pos = chatPane.viewToModel2D(pt);
        if (pos < 0) return null;

        Element elem = chatDoc.getCharacterElement(pos);
        AttributeSet attrs = elem.getAttributes();
        Object url = attrs.getAttribute("url");
        return url instanceof String ? (String) url : null;
    }

    private void scrollToBottom() {
        chatPane.setCaretPosition(chatDoc.getLength());
    }

    private void setInputEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
        sendBtn.setEnabled(enabled);
        if (!enabled) {
            sendBtn.setText("思考中...");
        } else {
            sendBtn.setText("發送");
        }
    }

    private void loadApiKey() {
        String saved = ApiKeyStore.load();
        if (!saved.isEmpty()) {
            agentService.setApiKey(saved);
        }
    }

    private void promptApiKey() {
        String current = agentService.getApiKey();

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("請輸入 OpenAI API Key:"), BorderLayout.NORTH);
        JPasswordField keyField = new JPasswordField(40);
        if (!current.isEmpty()) {
            keyField.setText(current);
        }
        panel.add(keyField, BorderLayout.CENTER);
        JLabel hint = new JLabel("<html><small>API Key 以 sk- 開頭，可從 platform.openai.com 取得<br/>Key 會加密儲存在 ~/.crawler/apikey.dat</small></html>");
        hint.setForeground(new Color(0x7f848e));
        panel.add(hint, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(this, panel, "設定 API Key",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String key = new String(keyField.getPassword()).trim();
            if (!key.isEmpty()) {
                agentService.setApiKey(key);
                ApiKeyStore.save(key);
                appendSystem("API Key 已設定並加密儲存。");
            }
        }
    }

    private void clearChat() {
        try {
            chatDoc.remove(0, chatDoc.getLength());
            agentService.clearHistory();
            appendSystem("對話已清除。");
        } catch (BadLocationException ignored) {
        }
    }
}
