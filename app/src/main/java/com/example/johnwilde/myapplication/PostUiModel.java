package com.example.johnwilde.myapplication;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by johnwilde on 5/8/17.
 */

public final class PostUiModel {
    private Map<String, String> mUrlMap = new ConcurrentHashMap<>();

    public static PostUiModel idle() {
        return new PostUiModel();
    }
}
