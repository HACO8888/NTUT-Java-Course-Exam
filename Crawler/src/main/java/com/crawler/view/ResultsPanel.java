package com.crawler.view;

import com.crawler.model.CrawlResult;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ResultsPanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JTextField filterField;
    private final JComboBox<String> statusFilter;
    private final List<CrawlResult> results = new ArrayList<>();
    private Consumer<CrawlResult> onResultSelected;
    private Consumer<CrawlResult> onResultDoubleClicked;

    private static final String[] COLUMNS = {
            "URL", "狀態碼", "標題", "大小", "回應時間(ms)", "深度", "連結數", "圖片數"
    };

    public ResultsPanel() {
        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 1: case 4: case 5: case 6: case 7: return Integer.class;
                    case 3: return Long.class;
                    default: return String.class;
                }
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);
        table.getTableHeader().setReorderingAllowed(false);

        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        table.getColumnModel().getColumn(0).setPreferredWidth(350);
        table.getColumnModel().getColumn(1).setPreferredWidth(60);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(50);
        table.getColumnModel().getColumn(6).setPreferredWidth(60);
        table.getColumnModel().getColumn(7).setPreferredWidth(60);

        table.getColumnModel().getColumn(1).setCellRenderer(new StatusCodeRenderer());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && onResultSelected != null) {
                int viewRow = table.getSelectedRow();
                if (viewRow >= 0) {
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    if (modelRow < results.size()) {
                        onResultSelected.accept(results.get(modelRow));
                    }
                }
            }
        });

        JPopupMenu popup = new JPopupMenu();
        JMenuItem copyUrl = new JMenuItem("複製 URL");
        copyUrl.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int modelRow = table.convertRowIndexToModel(row);
                String url = (String) tableModel.getValueAt(modelRow, 0);
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(url), null);
            }
        });
        JMenuItem openBrowser = new JMenuItem("在瀏覽器開啟");
        openBrowser.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int modelRow = table.convertRowIndexToModel(row);
                String url = (String) tableModel.getValueAt(modelRow, 0);
                try {
                    Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ignored) {
                }
            }
        });
        popup.add(copyUrl);
        popup.add(openBrowser);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { showPopup(e); }
            @Override
            public void mouseReleased(MouseEvent e) { showPopup(e); }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && onResultDoubleClicked != null) {
                    int viewRow = table.getSelectedRow();
                    if (viewRow >= 0) {
                        int modelRow = table.convertRowIndexToModel(viewRow);
                        if (modelRow < results.size()) {
                            onResultDoubleClicked.accept(results.get(modelRow));
                        }
                    }
                }
            }
            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) table.setRowSelectionInterval(row, row);
                    popup.show(table, e.getX(), e.getY());
                }
            }
        });

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterPanel.add(new JLabel("篩選:"));
        filterField = new JTextField(25);
        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });
        filterPanel.add(filterField);

        filterPanel.add(new JLabel("狀態:"));
        statusFilter = new JComboBox<>(new String[]{"全部", "2xx", "3xx", "4xx", "5xx", "錯誤"});
        statusFilter.addActionListener(e -> applyFilter());
        filterPanel.add(statusFilter);

        JLabel countLabel = new JLabel("共 0 筆");
        filterPanel.add(Box.createHorizontalStrut(12));
        filterPanel.add(countLabel);

        add(filterPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        tableModel.addTableModelListener(e ->
                countLabel.setText("共 " + tableModel.getRowCount() + " 筆"));
    }

    public void addResult(CrawlResult result) {
        results.add(result);
        tableModel.addRow(new Object[]{
                result.getUrl(),
                result.getStatusCode(),
                result.getTitle(),
                result.getContentLength(),
                (int) result.getResponseTimeMs(),
                result.getDepth(),
                result.getLinkCount(),
                result.getImageCount()
        });
    }

    public void clear() {
        results.clear();
        tableModel.setRowCount(0);
    }

    public void setOnResultSelected(Consumer<CrawlResult> listener) {
        this.onResultSelected = listener;
    }

    public void setOnResultDoubleClicked(Consumer<CrawlResult> listener) {
        this.onResultDoubleClicked = listener;
    }

    public List<CrawlResult> getAllResults() {
        return new ArrayList<>(results);
    }

    private void applyFilter() {
        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        String text = filterField.getText().trim();
        if (!text.isEmpty()) {
            try {
                filters.add(RowFilter.regexFilter("(?i)" + text, 0, 2));
            } catch (Exception ignored) {
            }
        }

        String status = (String) statusFilter.getSelectedItem();
        if (status != null && !status.equals("全部")) {
            switch (status) {
                case "2xx": filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, 199, 1));
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, 300, 1)); break;
                case "3xx": filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, 299, 1));
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, 400, 1)); break;
                case "4xx": filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, 399, 1));
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, 500, 1)); break;
                case "5xx": filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, 499, 1));
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, 600, 1)); break;
                case "錯誤": filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, 0, 1)); break;
            }
        }

        if (filters.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.andFilter(filters));
        }
    }

    private static class StatusCodeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected && value instanceof Integer) {
                int code = (Integer) value;
                if (code >= 200 && code < 300) {
                    c.setForeground(new Color(0x98C379));
                } else if (code >= 300 && code < 400) {
                    c.setForeground(new Color(0xE5C07B));
                } else if (code >= 400) {
                    c.setForeground(new Color(0xE06C75));
                } else {
                    c.setForeground(new Color(0xE06C75));
                }
            }
            setHorizontalAlignment(SwingConstants.CENTER);
            return c;
        }
    }
}
