package com.example.johnwilde.myapplication;

/**
 * Created by johnwilde on 5/9/17.
 */

public class PostResult {
    enum Status {IDLE, COLLAPSE_LINK, EMBED_REQUEST_IN_FLIGHT, ERROR_EXPANDING_LINK, EXPAND_LINK}
    Status mStatus;

}
