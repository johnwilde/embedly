package com.example.johnwilde.myapplication;

public class CollapseLinkResult extends PostResult {
    CollapseLinkAction mAction;
    private CollapseLinkResult(Status status) {
        mStatus = status;
    }
    private CollapseLinkResult(Status status, CollapseLinkAction action) {
        mStatus = status;
        mAction = action;
    }

    static CollapseLinkResult expand(CollapseLinkAction action) {
        return new CollapseLinkResult(Status.COLLAPSE_LINK, action);
    }
    static CollapseLinkResult idle() {
        return new CollapseLinkResult(Status.IDLE);
    }

}
