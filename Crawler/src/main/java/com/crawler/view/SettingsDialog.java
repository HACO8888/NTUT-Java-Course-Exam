package com.crawler.view;

import com.crawler.model.CrawlConfig;
import com.crawler.model.CrawlConfig.TraversalStrategy;

import javax.swing.*;
import java.awt.*;

public class SettingsDialog extends JDialog {
    private final JTextField seedUrlField;
    private final JSpinner maxDepthSpinner;
    private final JSpinner maxPagesSpinner;
    private final JSpinner threadCountSpinner;
    private final JSpinner requestDelaySpinner;
    private final JSpinner timeoutSpinner;
    private final JTextField userAgentField;
    private final JCheckBox respectRobotsCheck;
    private final JComboBox<TraversalStrategy> strategyCombo;
    private final JTextField includePatternField;
    private final JTextField excludePatternField;
    private final JTextField extractionSelectorField;
    private final JCheckBox downloadResourcesCheck;
    private final JCheckBox sameDomainCheck;
    private boolean confirmed = false;

    public SettingsDialog(JFrame parent) {
        super(parent, "爬蟲設定", true);
        setSize(520, 580);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        seedUrlField = new JTextField("https://www.ntut.edu.tw/", 30);
        addField(formPanel, gbc, row++, "起始 URL:", seedUrlField);

        maxDepthSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 20, 1));
        addField(formPanel, gbc, row++, "最大深度:", maxDepthSpinner);

        maxPagesSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 10000, 10));
        addField(formPanel, gbc, row++, "最大頁數:", maxPagesSpinner);

        threadCountSpinner = new JSpinner(new SpinnerNumberModel(8, 1, 32, 1));
        addField(formPanel, gbc, row++, "執行緒數:", threadCountSpinner);

        requestDelaySpinner = new JSpinner(new SpinnerNumberModel(200, 0, 5000, 50));
        addField(formPanel, gbc, row++, "請求延遲 (ms):", requestDelaySpinner);

        timeoutSpinner = new JSpinner(new SpinnerNumberModel(10000, 1000, 60000, 1000));
        addField(formPanel, gbc, row++, "逾時 (ms):", timeoutSpinner);

        userAgentField = new JTextField("JavaCrawler/1.0", 30);
        addField(formPanel, gbc, row++, "User-Agent:", userAgentField);

        strategyCombo = new JComboBox<>(TraversalStrategy.values());
        addField(formPanel, gbc, row++, "遍歷策略:", strategyCombo);

        respectRobotsCheck = new JCheckBox("", true);
        addField(formPanel, gbc, row++, "遵循 robots.txt:", respectRobotsCheck);

        sameDomainCheck = new JCheckBox("", true);
        addField(formPanel, gbc, row++, "僅爬取同網域:", sameDomainCheck);

        includePatternField = new JTextField("", 30);
        addField(formPanel, gbc, row++, "包含 URL 模式 (regex):", includePatternField);

        excludePatternField = new JTextField("", 30);
        addField(formPanel, gbc, row++, "排除 URL 模式 (regex):", excludePatternField);

        extractionSelectorField = new JTextField("", 30);
        addField(formPanel, gbc, row++, "CSS 選擇器:", extractionSelectorField);

        downloadResourcesCheck = new JCheckBox("", false);
        addField(formPanel, gbc, row++, "下載資源:", downloadResourcesCheck);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton okBtn = new JButton("確定");
        JButton cancelBtn = new JButton("取消");
        okBtn.addActionListener(e -> {
            confirmed = true;
            setVisible(false);
        });
        cancelBtn.addActionListener(e -> {
            confirmed = false;
            setVisible(false);
        });
        buttonPanel.add(okBtn);
        buttonPanel.add(cancelBtn);

        setLayout(new BorderLayout());
        add(new JScrollPane(formPanel), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int row,
                           String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    public CrawlConfig getConfig() {
        CrawlConfig config = new CrawlConfig();
        config.setSeedUrl(seedUrlField.getText().trim());
        config.setMaxDepth((int) maxDepthSpinner.getValue());
        config.setMaxPages((int) maxPagesSpinner.getValue());
        config.setThreadCount((int) threadCountSpinner.getValue());
        config.setRequestDelayMs((int) requestDelaySpinner.getValue());
        config.setTimeoutMs((int) timeoutSpinner.getValue());
        config.setUserAgent(userAgentField.getText().trim());
        config.setRespectRobotsTxt(respectRobotsCheck.isSelected());
        config.setTraversalStrategy((TraversalStrategy) strategyCombo.getSelectedItem());
        config.setIncludePattern(includePatternField.getText().trim());
        config.setExcludePattern(excludePatternField.getText().trim());
        config.setExtractionSelector(extractionSelectorField.getText().trim());
        config.setDownloadResources(downloadResourcesCheck.isSelected());
        config.setSameDomainOnly(sameDomainCheck.isSelected());
        return config;
    }

    public void setConfig(CrawlConfig config) {
        seedUrlField.setText(config.getSeedUrl());
        maxDepthSpinner.setValue(config.getMaxDepth());
        maxPagesSpinner.setValue(config.getMaxPages());
        threadCountSpinner.setValue(config.getThreadCount());
        requestDelaySpinner.setValue((int) config.getRequestDelayMs());
        timeoutSpinner.setValue(config.getTimeoutMs());
        userAgentField.setText(config.getUserAgent());
        respectRobotsCheck.setSelected(config.isRespectRobotsTxt());
        strategyCombo.setSelectedItem(config.getTraversalStrategy());
        includePatternField.setText(config.getIncludePattern());
        excludePatternField.setText(config.getExcludePattern());
        extractionSelectorField.setText(config.getExtractionSelector());
        downloadResourcesCheck.setSelected(config.isDownloadResources());
        sameDomainCheck.setSelected(config.isSameDomainOnly());
    }

    public boolean isConfirmed() { return confirmed; }
}
