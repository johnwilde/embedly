package com.example.johnwilde.myapplication;

/**
 * Created by johnwilde on 5/9/17.
 */

public class LinkExpandedAction extends PostAction {
    String mUrl;
    private LinkExpandedAction(String url) {
        mUrl = url;
    }
    static LinkExpandedAction expandLink(String url) {
        return new LinkExpandedAction(url);
    }
}

