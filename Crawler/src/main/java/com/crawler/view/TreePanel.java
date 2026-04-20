package com.crawler.view;

import com.crawler.model.SiteNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TreePanel extends JPanel {
    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode root;
    private final Map<String, DefaultMutableTreeNode> nodeMap = new HashMap<>();
    private final JTextField searchField;
    private Consumer<SiteNode> onNodeSelected;

    public TreePanel() {
        setLayout(new BorderLayout());

        root = new DefaultMutableTreeNode(new SiteNode("(未開始)"));
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setRowHeight(24);
        tree.setCellRenderer(new SiteNodeRenderer());

        tree.addTreeSelectionListener(e -> {
            if (onNodeSelected != null) {
                DefaultMutableTreeNode selected =
                        (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (selected != null && selected.getUserObject() instanceof SiteNode) {
                    onNodeSelected.accept((SiteNode) selected.getUserObject());
                }
            }
        });

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton expandAll = new JButton("全部展開");
        JButton collapseAll = new JButton("全部收合");
        expandAll.addActionListener(e -> expandAllNodes(true));
        collapseAll.addActionListener(e -> expandAllNodes(false));
        toolbar.add(expandAll);
        toolbar.add(collapseAll);

        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(new JLabel("搜尋:"));
        searchField = new JTextField(20);
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { searchTree(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { searchTree(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { searchTree(); }
        });
        toolbar.add(searchField);

        JLabel countLabel = new JLabel("節點: 0");
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(countLabel);

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(tree), BorderLayout.CENTER);

        treeModel.addTreeModelListener(new javax.swing.event.TreeModelListener() {
            public void treeNodesChanged(javax.swing.event.TreeModelEvent e) {}
            public void treeNodesInserted(javax.swing.event.TreeModelEvent e) {
                countLabel.setText("節點: " + nodeMap.size());
            }
            public void treeNodesRemoved(javax.swing.event.TreeModelEvent e) {}
            public void treeStructureChanged(javax.swing.event.TreeModelEvent e) {
                countLabel.setText("節點: " + nodeMap.size());
            }
        });
    }

    public void setRootNode(SiteNode node) {
        root.setUserObject(node);
        root.removeAllChildren();
        nodeMap.clear();
        nodeMap.put(node.getUrl(), root);
        treeModel.reload();
    }

    public void addNode(SiteNode node, String parentUrl) {
        DefaultMutableTreeNode parentTreeNode = null;
        if (parentUrl != null) {
            parentTreeNode = nodeMap.get(parentUrl);
        }
        if (parentTreeNode == null) {
            parentTreeNode = root;
        }

        DefaultMutableTreeNode child = new DefaultMutableTreeNode(node);
        nodeMap.put(node.getUrl(), child);
        parentTreeNode.add(child);
        treeModel.nodesWereInserted(parentTreeNode, new int[]{parentTreeNode.getChildCount() - 1});
    }

    public void addNode(SiteNode node) {
        addNode(node, null);
    }

    public void clear() {
        root.setUserObject(new SiteNode("(未開始)"));
        root.removeAllChildren();
        nodeMap.clear();
        treeModel.reload();
    }

    public void setOnNodeSelected(Consumer<SiteNode> listener) {
        this.onNodeSelected = listener;
    }

    private void searchTree() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) return;

        for (int i = 0; i < tree.getRowCount(); i++) {
            javax.swing.tree.TreePath path = tree.getPathForRow(i);
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (n.getUserObject() instanceof SiteNode) {
                SiteNode sn = (SiteNode) n.getUserObject();
                String text = sn.getUrl().toLowerCase();
                String title = sn.getTitle() != null ? sn.getTitle().toLowerCase() : "";
                if (text.contains(query) || title.contains(query)) {
                    tree.setSelectionPath(path);
                    tree.scrollPathToVisible(path);
                    return;
                }
            }
        }
    }

    private void expandAllNodes(boolean expand) {
        int rowCount = tree.getRowCount();
        if (expand) {
            for (int i = 0; i < rowCount; i++) {
                tree.expandRow(i);
                rowCount = tree.getRowCount();
            }
        } else {
            for (int i = rowCount - 1; i >= 1; i--) {
                tree.collapseRow(i);
            }
        }
    }

    private static class SiteNodeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode) {
                Object obj = ((DefaultMutableTreeNode) value).getUserObject();
                if (obj instanceof SiteNode) {
                    SiteNode node = (SiteNode) obj;
                    int code = node.getStatusCode();
                    if (!sel) {
                        if (code >= 200 && code < 300) {
                            setForeground(new Color(0x98C379));
                        } else if (code >= 400) {
                            setForeground(new Color(0xE06C75));
                        }
                    }
                    String title = node.getTitle();
                    if (title != null && !title.isEmpty()) {
                        setText(node.getDisplayName() + " - " + title);
                    }
                }
            }
            return c;
        }
    }
}
