package com.example.johnwilde.myapplication;

public class CollapseLinkAction extends PostAction {
    String mUrl;
    private CollapseLinkAction(String url) {
        mUrl = url;
    }
    static CollapseLinkAction collapseLink(String url) {
        return new CollapseLinkAction(url);
    }
}

