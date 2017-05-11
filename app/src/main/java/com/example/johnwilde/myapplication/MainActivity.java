package com.example.johnwilde.myapplication;

import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.example.johnwilde.myapplication.PostResult.Status;
import com.example.johnwilde.myapplication.PostUiModel.State;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxAdapterView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.squareup.okhttp.OkHttpClient;

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

    private Observable<PostUiEvent> getUiEventSequence() {
        // Streams of UI events
        Observable<FindLinkEvent> findLinkEvents = RxTextView.afterTextChangeEvents(mEditText)
                .map(text -> new FindLinkEvent(text));
        Observable<CollapseLinkEvent> collapseLinkEvents = mAdapter.getViewClickedObservable()
                .map(view -> new CollapseLinkEvent(view));
        Observable<PostUiEvent> uiEvents
                = Observable.merge(findLinkEvents, collapseLinkEvents);
        return uiEvents;
    }

    private ObservableTransformer<PostUiEvent, PostAction> getActionTransformer() {
        // Transform UI events to single stream of Actions
       ObservableTransformer<FindLinkEvent, FindLinkAction> findLinkActions =
               actions -> actions
                       .flatMap(event -> Observable.fromArray(event.spans))
                       .filter(span -> !FindLinkAction.foundUrl(span.getURL()))
                       .map(span -> FindLinkAction.findLink(span.getURL()));
       ObservableTransformer<CollapseLinkEvent, CollapseLinkAction> collapseLinkAction =
               actions -> actions.map(event -> CollapseLinkAction.expandLink(event.mString));
       ObservableTransformer<PostUiEvent, PostAction> postActions
               = events -> events.publish(shared -> Observable.merge(
                       shared.ofType(FindLinkEvent.class).compose(findLinkActions),
                       shared.ofType(CollapseLinkEvent.class).compose(collapseLinkAction)
                       ));
        return postActions;
    }

    private ObservableTransformer<PostAction, PostResult> getResultTransformer() {
        Retrofit retrofit = new Builder()
                .baseUrl("http://api.embedly.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        // Retrofit instance which was created earlier
        EmbedlyApi embedlyApi = retrofit.create(EmbedlyApi.class);

        // Transform Actions to Results
       ObservableTransformer<FindLinkAction, FindLinkResult> findLink =
               actions -> actions
                       .delay(200, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                       .observeOn(Schedulers.io())
                       .switchMap(action ->
                           embedlyApi.getUrl(BuildConfig.EMBEDLY_API_KEY, action.getUrl())
                                   .map(response -> Pair.create(response, action.getUrl()))
                       )
                       .map(pair -> FindLinkResult.success(pair.first, pair.second))
                       .onErrorReturn(t -> FindLinkResult.failure())
                       .observeOn(AndroidSchedulers.mainThread());
        ObservableTransformer<FindLinkAction, FindLinkResult> inProgress =
                actions -> actions
                        .delay(200, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                        .observeOn(Schedulers.io())
                        .map(action -> FindLinkResult.inFlight(action.getUrl()))
                        .observeOn(AndroidSchedulers.mainThread());
       ObservableTransformer<CollapseLinkAction, CollapseLinkResult> expandLink =
               actions -> actions
                           .map(action -> CollapseLinkResult.expand(action))
                           .observeOn(AndroidSchedulers.mainThread())
                           .startWith(CollapseLinkResult.idle());

       ObservableTransformer<PostAction, PostResult> postResults
               = events -> events.publish(shared -> Observable.merge(
                       shared.ofType(FindLinkAction.class).compose(findLink),
                       shared.ofType(FindLinkAction.class).compose(inProgress),
                       shared.ofType(CollapseLinkAction.class).compose(expandLink)
               ));
        return postResults;
    }

    Observable<PostUiModel> getUiModelSequence() {

        PostUiModel initialState = PostUiModel.idle();
        Observable<PostUiModel> uiModels =
                getUiEventSequence()
                .observeOn(AndroidSchedulers.mainThread())
                .compose(getActionTransformer())
                .compose(getResultTransformer())
                .scan(initialState, (state, result) -> {
                    if (result.mStatus == Status.IN_FLIGHT) {
                        if (result instanceof FindLinkResult) {
                            FindLinkResult result1 = (FindLinkResult) result;
                            return PostUiModel.inProgress(state, result1.mUrl);
                        }
                    }
                    if (result.mStatus == Status.SUCCESS) {
                        FindLinkResult result1 = (FindLinkResult) result;
                        return PostUiModel.expandLink(state, result1.mUrl, result1.mResponse);
                    }
                    if (result.mStatus == Status.COLLAPSE_LINK) {
                        return PostUiModel.collapseLink(state);
                    }
                    return state;
                });
        return uiModels;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        mAdapter = new EmbedAdapter(this);
        mRecyclerView.setLayoutManager(
                  new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(mAdapter);

        Observable<PostUiModel> uiModels = getUiModelSequence();

        // bind models to view
        mDisposable = uiModels
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(model -> {
            if (model.mState == State.IN_PROGRESS) {
                Log.d(TAG, "IN_PROGRESS " + model.mUrl);
//                mTextView.append(model.mUrlMap.keySet().toString());
            }
            if (model.mState == State.EXPAND_LINK) {
                Log.d(TAG, "EXPAND_LINK " + model.mUrl);
                mAdapter.add(model.getResponse());
            }
            if (model.mState == State.COLLAPSE_LINK) {
                Log.d(TAG, "COLLAPSE_LINK " + model.mUrl);
                mAdapter.remove(model.getResponse());
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
}
