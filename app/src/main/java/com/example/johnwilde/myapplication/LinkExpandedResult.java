package com.example.johnwilde.myapplication;

public class LinkExpandedResult extends PostResult {
    LinkExpandedAction mAction;
    private LinkExpandedResult(Status status) {
        mStatus = status;
    }
    private LinkExpandedResult(Status status, LinkExpandedAction action) {
        mStatus = status;
        mAction = action;
    }

    static LinkExpandedResult expand(LinkExpandedAction action) {
        return new LinkExpandedResult(Status.EXPAND_LINK, action);
    }
    static LinkExpandedResult idle() {
        return new LinkExpandedResult(Status.IDLE);
    }

}
