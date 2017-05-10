package com.example.johnwilde.myapplication;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by johnwilde on 5/9/17.
 */

public interface EmbedlyApi {
    @GET("1/oembed")
    Observable<OembedResponse> getUrl(
            @Query("key") String key,
            @Query("url") String url);
}
