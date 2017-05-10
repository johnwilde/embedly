package com.example.johnwilde.myapplication;

/**
 * Created by johnwilde on 5/9/17.
 */

public class FindLinkAction extends PostAction {
    String mUrl;
    private FindLinkAction(String url) {
        mUrl = url;
    }
    static FindLinkAction findLink(String url) {
        return  new FindLinkAction(url);
    }
    String getUrl() {
        return mUrl;
    }
}
