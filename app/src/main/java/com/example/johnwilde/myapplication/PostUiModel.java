package com.example.johnwilde.myapplication;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.johnwilde.myapplication.PostUiModel.State.COLLAPSE_LINK;
import static com.example.johnwilde.myapplication.PostUiModel.State.ERROR_EXPANDING_LINK;
import static com.example.johnwilde.myapplication.PostUiModel.State.EXPAND_LINK;
import static com.example.johnwilde.myapplication.PostUiModel.State.IDLE;
import static com.example.johnwilde.myapplication.PostUiModel.State.IN_PROGRESS;

/**
 * Created by johnwilde on 5/8/17.
 */

public final class PostUiModel {
    Map<String, OembedResponse> mUrlMap = new ConcurrentHashMap<>();
    enum State {IDLE, IN_PROGRESS, EXPAND_LINK, COLLAPSE_LINK, ERROR_EXPANDING_LINK}
    State mState = IDLE;
    String mUrl;

    public static PostUiModel idle() {
        return new PostUiModel();
    }
    public static PostUiModel inProgress(PostUiModel start, String url) {
        start.mState = IN_PROGRESS;
        start.mUrl = url;
        start.mUrlMap.put(url, new OembedResponse());
        return start;
    }
    public static PostUiModel expandLink(PostUiModel start, String url, OembedResponse response) {
        start.mState = EXPAND_LINK;
        start.mUrl = url;
        response.requestUrl = url;
        start.mUrlMap.put(url, response);
        return start;
    }

    public static PostUiModel collapseLink(PostUiModel start, CollapseLinkResult result) {
        start.mState = COLLAPSE_LINK;
        start.mUrl = result.mAction.mUrl;
        return start;
    }

    public static PostUiModel errorExpandingLink(PostUiModel start) {
        start.mState = ERROR_EXPANDING_LINK;
        return start;
    }

    OembedResponse getResponse() {
        return mUrlMap.get(mUrl);
    }

}
