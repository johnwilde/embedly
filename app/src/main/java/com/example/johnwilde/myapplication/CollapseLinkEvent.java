package com.example.johnwilde.myapplication;

import android.view.View;

public class CollapseLinkEvent extends PostUiEvent {
    View mView;
    public CollapseLinkEvent(Object view) {
        mView = (View) view;
    }
}
