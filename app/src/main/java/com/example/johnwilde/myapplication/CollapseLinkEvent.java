package com.example.johnwilde.myapplication;

import com.jakewharton.rxbinding2.widget.TextViewAfterTextChangeEvent;

/**
 * Created by johnwilde on 5/9/17.
 */

public class CollapseLinkEvent extends PostUiEvent {
    String mString;
    public CollapseLinkEvent(TextViewAfterTextChangeEvent event) {
        mString = event.editable().toString();
    }
}
