package com.example.johnwilde.myapplication;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.johnwilde.myapplication.PostResult.Status;
import com.example.johnwilde.myapplication.PostUiModel.State;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.OnErrorNotImplementedException;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.Retrofit.Builder;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private static String TAG = MainActivity.class.getCanonicalName();

    private Disposable mDisposable;
    OkHttpClient client = new OkHttpClient();

    @InjectView(R.id.text1) EditText mEditText;
    @InjectView(R.id.recyclerView) RecyclerView mRecyclerView;
    @InjectView(R.id.text2) TextView mTextView;

    private Map<String, String> mUrlMap = new ConcurrentHashMap<>();
    private EmbedAdapter mAdapter;

    private class NetworkTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String url =  params[0];

            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http")
                    .path("api.embedly.com/1/oembed")
                    .appendQueryParameter("key", BuildConfig.EMBEDLY_API_KEY)
                    .appendQueryParameter("url", url);
            Request request = new Request.Builder()
              .url(builder.build().toString())
              .build();

            Log.d(TAG, "request oembed for url: " + url);
            Response response = null;
            try {
                response = client.newCall(request).execute();
                String result = response.body().string();
                mUrlMap.put(url, result);
                Log.d(TAG, "response " + result);
                return result;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mAdapter.add(s);
            mTextView.append(s);
        }
    }

    private class EditTextChangedListener implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            Linkify.addLinks(mEditText, Linkify.WEB_URLS);
            URLSpan spans[] = mEditText.getUrls();
            for(URLSpan span: spans) {
                String url = span.getURL();
                if (!mUrlMap.containsKey(url)) {
                    mUrlMap.put(url, "in progress");
                    new NetworkTask().execute(url);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        Retrofit retrofit = new Builder()
                .baseUrl("http://api.embedly.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        // Retrofit instance which was created earlier
        EmbedlyApi embedlyApi = retrofit.create(EmbedlyApi.class);

        mAdapter = new EmbedAdapter(this);
        mRecyclerView.setLayoutManager(
                  new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(mAdapter);
        mEditText.addTextChangedListener(new EditTextChangedListener());
        mTextView.setMovementMethod(new ScrollingMovementMethod());

        // Streams of UI events
        Observable<FindLinkEvent> findLinkEvents = RxTextView.afterTextChangeEvents(mEditText)
                .map(text -> new FindLinkEvent(text));
        // todo: real event for link expansion
        Observable<LinkExpandedEvent> linkExpandedEvents = RxTextView.afterTextChangeEvents(mEditText)
                .map(text -> new LinkExpandedEvent(text));
        Observable<PostUiEvent> uiEvents
                = Observable.merge(findLinkEvents, linkExpandedEvents);


        // Transform UI events to single stream of Actions
        ObservableTransformer<FindLinkEvent, FindLinkAction> findLinkActions =
                actions -> actions.map(event -> FindLinkAction.findLink(event.mText));
        ObservableTransformer<LinkExpandedEvent, LinkExpandedAction> expandLinkActions =
                actions -> actions.map(event -> LinkExpandedAction.expandLink(event.mString));
        ObservableTransformer<PostUiEvent, PostAction> postActions
                = events -> events.publish(shared -> Observable.merge(
                        shared.ofType(FindLinkEvent.class).compose(findLinkActions),
                        shared.ofType(LinkExpandedEvent.class).compose(expandLinkActions)
                ));

        // Transform Actions to Results
        ObservableTransformer<FindLinkAction, FindLinkResult> findLink =
                actions -> actions.delay(200, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                        .switchMap(action ->
                                embedlyApi.getUrl(BuildConfig.EMBEDLY_API_KEY, action.getUrl())
                        .map(response -> FindLinkResult.success(response))
                        .onErrorReturn(t -> FindLinkResult.failure())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(FindLinkResult.inFlight()));
        ObservableTransformer<LinkExpandedAction, LinkExpandedResult> expandLink =
                actions -> actions
                            .map(action -> LinkExpandedResult.expand(action))
                            .observeOn(AndroidSchedulers.mainThread())
                            .startWith(LinkExpandedResult.idle());
        ObservableTransformer<PostAction, PostResult> postResults
                = events -> events.publish(shared -> Observable.merge(
                        shared.ofType(FindLinkAction.class).compose(findLink),
                        shared.ofType(LinkExpandedAction.class).compose(expandLink)
                ));


        PostUiModel initialState = PostUiModel.idle();
        Observable<PostUiModel> uiModels = uiEvents
                .observeOn(AndroidSchedulers.mainThread())
                .compose(postActions)
                .compose(postResults)
                .scan(initialState, (state, result) -> {
                    if (result.mStatus == Status.IN_FLIGHT) {
                        return PostUiModel.inProgress(state);
                    }
                    if (result.mStatus == Status.EXPAND_LINK) {
                        return PostUiModel.expandLink(state);
                    }
                    return state;
                });

        // bind models to view
        mDisposable = uiModels.subscribe(model -> {
            if (model.mState == State.EXPAND_LINK) {
                mTextView.append(model.toString());
            }
        }, t -> { throw new OnErrorNotImplementedException(t);});
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
      super.onStop();
      if (!mDisposable.isDisposed()) {
        mDisposable.dispose();
      }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if(mEditText.getSelectionStart() == -1){ // in case of setMovementMethod(LinkMovementMethod.getInstance())
            menu.add(0, 1, 0, "Enable copy");
        }
        else{
            menu.add(0, 2, 0, "Enable links");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case 1:
            mEditText.setMovementMethod(ArrowKeyMovementMethod.getInstance());
            mEditText.setSelection(0, 0);
                  //re-register EditText for context menu:
              unregisterForContextMenu(mEditText);
              registerForContextMenu(mEditText);
              break;
          case 2:
              mEditText.setMovementMethod(LinkMovementMethod.getInstance());
              break;
          }
          return true;
      }
}
