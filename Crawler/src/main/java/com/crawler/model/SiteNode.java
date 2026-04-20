package com.crawler.model;

import java.util.ArrayList;
import java.util.List;

public class SiteNode {
    private final String url;
    private String title;
    private int statusCode;
    private final List<SiteNode> children = new ArrayList<>();
    private SiteNode parent;

    public SiteNode(String url) {
        this.url = url;
    }

    public String getUrl() { return url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public List<SiteNode> getChildren() { return children; }

    public SiteNode getParent() { return parent; }

    public void addChild(SiteNode child) {
        child.parent = this;
        children.add(child);
    }

    public String getDisplayName() {
        try {
            java.net.URL u = new java.net.URL(url);
            String path = u.getPath();
            if (path.isEmpty() || path.equals("/")) return u.getHost();
            return path;
        } catch (Exception e) {
            return url;
        }
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
