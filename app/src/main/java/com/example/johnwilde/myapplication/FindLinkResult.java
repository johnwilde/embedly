package com.example.johnwilde.myapplication;

import com.squareup.okhttp.Response;

/**
 * Created by johnwilde on 5/8/17.
 */

public final class FindLinkResult extends PostResult {
    OembedResponse mResponse;
    private FindLinkResult(Status status) {
        mStatus = status;
    }
    private FindLinkResult(Status status, OembedResponse response) {
        mStatus = status;
        mResponse = response;
    }
    static FindLinkResult inFlight() {
        return new FindLinkResult(Status.IN_FLIGHT);
    }
    static FindLinkResult success(OembedResponse response) {
        return new FindLinkResult(Status.SUCCESS, response);
    }
    static FindLinkResult failure() {
        return new FindLinkResult(Status.INVALID);
    }

}
