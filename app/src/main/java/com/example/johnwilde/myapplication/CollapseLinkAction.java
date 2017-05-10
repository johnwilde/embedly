package com.example.johnwilde.myapplication;

/**
 * Created by johnwilde on 5/9/17.
 */

public class CollapseLinkAction extends PostAction {
    String mUrl;
    private CollapseLinkAction(String url) {
        mUrl = url;
    }
    static CollapseLinkAction expandLink(String url) {
        return new CollapseLinkAction(url);
    }
}

