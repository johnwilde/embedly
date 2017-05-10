package com.example.johnwilde.myapplication;

public final class FindLinkResult extends PostResult {
    OembedResponse mResponse;
    String mUrl;
    private FindLinkResult(Status status) {
        mStatus = status;
    }
    private FindLinkResult(Status status, OembedResponse response) {
        mStatus = status;
        mResponse = response;
    }
    static FindLinkResult inFlight(String url) {
        FindLinkResult result = new FindLinkResult(Status.IN_FLIGHT);
        result.mUrl = url;
        return result;
    }
    static FindLinkResult success(OembedResponse response) {
        return new FindLinkResult(Status.SUCCESS, response);
    }
    static FindLinkResult failure() {
        return new FindLinkResult(Status.INVALID);
    }

}
