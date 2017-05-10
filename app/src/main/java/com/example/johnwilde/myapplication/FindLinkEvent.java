package com.example.johnwilde.myapplication;

import android.text.style.URLSpan;
import android.text.util.Linkify;

import com.jakewharton.rxbinding2.widget.TextViewAfterTextChangeEvent;

public class FindLinkEvent extends PostUiEvent {
    URLSpan spans[];
    public FindLinkEvent(TextViewAfterTextChangeEvent event) {
        Linkify.addLinks(event.view(), Linkify.WEB_URLS);
        spans = event.view().getUrls();
    }
}
