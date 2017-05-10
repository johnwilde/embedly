package com.example.johnwilde.myapplication;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.johnwilde.myapplication.PostUiModel.State.EXPAND_LINK;
import static com.example.johnwilde.myapplication.PostUiModel.State.IDLE;
import static com.example.johnwilde.myapplication.PostUiModel.State.IN_PROGRESS;

/**
 * Created by johnwilde on 5/8/17.
 */

public final class PostUiModel {
    private Map<String, String> mUrlMap = new ConcurrentHashMap<>();
    enum State {IDLE, IN_PROGRESS, EXPAND_LINK}
    State mState = IDLE;

    public static PostUiModel idle() {
        return new PostUiModel();
    }
    public static PostUiModel inProgress(PostUiModel start) {
        start.mState = IN_PROGRESS;
        return start;
    }
    public static PostUiModel expandLink(PostUiModel start) {
        start.mState = EXPAND_LINK;
        return start;
    }

}
