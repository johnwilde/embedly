package com.example.johnwilde.myapplication;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FindLinkAction extends PostAction {
    static Map<String, String> mUrlMap = new ConcurrentHashMap<>();

    String mUrl;
    private FindLinkAction(String url) {
        mUrl = url;
    }
    static FindLinkAction findLink(String url) {
        mUrlMap.put(url, "search");
        return new FindLinkAction(url);
    }
    static boolean foundUrl(String url) {
        return mUrlMap.containsKey(url);
    }
    String getUrl() {
        return mUrl;
    }
}
