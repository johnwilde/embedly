package com.example.johnwilde.myapplication;

import com.jakewharton.rxbinding2.widget.TextViewAfterTextChangeEvent;

/**
 * Created by johnwilde on 5/8/17.
 */

public class FindLinkEvent extends PostUiEvent {
    String mText;
    public FindLinkEvent(TextViewAfterTextChangeEvent event) {
    }
}
