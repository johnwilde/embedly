package com.example.johnwilde.myapplication;

import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.example.johnwilde.myapplication.EmbedAdapter.EmbedViewHolder;
import com.example.johnwilde.myapplication.PostResult.Status;
import com.example.johnwilde.myapplication.PostUiModel.State;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.squareup.okhttp.OkHttpClient;

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

    private EmbedAdapter mAdapter;

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
            }
            if (model.mState == State.EXPAND_LINK) {
                Log.d(TAG, "EXPAND_LINK " + model.mUrl);
                mAdapter.add(model.getResponse());
            }
            if (model.mState == State.COLLAPSE_LINK) {
                Log.d(TAG, "COLLAPSE_LINK " + model.mUrl);
                mAdapter.remove(model.mUrl);
            }
            if (model.mState == State.ERROR_EXPANDING_LINK) {
                Log.d(TAG, "ERROR_EXPANDING_LINK " + model.mUrl);
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


    private Observable<PostUiModel> getUiModelSequence() {

        PostUiModel initialState = PostUiModel.idle();
        Observable<PostUiModel> uiModels = getUiEventSequence()
            .observeOn(AndroidSchedulers.mainThread())
            .compose(getActionTransformer()) // Transform Events into Actions
            .compose(getResultTransformer()) // Transform Actions into Results
            .scan(initialState, (state, result) -> { // Transform Results into UiModels
                if (result.mStatus == Status.EMBED_REQUEST_IN_FLIGHT) {
                    FindLinkResult findLink = (FindLinkResult) result;
                    return PostUiModel.inProgress(state, findLink.mUrl);
                }
                if (result.mStatus == Status.EXPAND_LINK) {
                    FindLinkResult findLink = (FindLinkResult) result;
                    return PostUiModel.expandLink(state, findLink.mUrl, findLink.mResponse);
                }
                if (result.mStatus == Status.COLLAPSE_LINK) {
                    CollapseLinkResult collapseLinkResult = (CollapseLinkResult) result;
                    return PostUiModel.collapseLink(state, collapseLinkResult);
                }
                if (result.mStatus == Status.ERROR_EXPANDING_LINK) {
                    return PostUiModel.errorExpandingLink(state);
                }
                return state;
            });
        return uiModels;
    }

    private Observable<PostUiEvent> getUiEventSequence() {
        // Streams of UI events
        Observable<FindLinkEvent> findLinkEvents = RxTextView.afterTextChangeEvents(mEditText)
                .map(text -> new FindLinkEvent(text));
        // The adapter passes along clicks on any collapse button, with a reference to the
        // child view that was clicked
        Observable<CollapseLinkEvent> collapseLinkEvents = mAdapter.getViewClickedObservable()
                .map(view -> new CollapseLinkEvent(view));
        // Merge into a single stream
        Observable<PostUiEvent> uiEvents = Observable.merge(findLinkEvents, collapseLinkEvents);
        return uiEvents;
    }

    private ObservableTransformer<PostUiEvent, PostAction> getActionTransformer() {
        // Turn stream of text change events into a stream of FindLinkActions. Each action is
        // an URL to look up with embedly
        ObservableTransformer<FindLinkEvent, FindLinkAction> findLinkActions =
            actions -> actions
                    .flatMap(event -> Observable.fromArray(event.spans))
                    .filter(span -> !FindLinkAction.foundUrl(span.getURL()))
                    .map(span -> FindLinkAction.findLink(span.getURL()));

        // Turn stream of collapse events into actions, the URL was saved in the view holder
        ObservableTransformer<CollapseLinkEvent, CollapseLinkAction> collapseLinkAction =
            actions -> actions.map(clickEvent -> {
               EmbedViewHolder vh =
                       (EmbedViewHolder) mRecyclerView.getChildViewHolder(clickEvent.mView);
               return CollapseLinkAction.collapseLink(vh.mUrl);
            });

        // Merge all  UI events to single stream of Actions
        ObservableTransformer<PostUiEvent, PostAction> postActions =
            events -> events.publish(shared -> Observable.merge(
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
        EmbedlyApi embedlyApi = retrofit.create(EmbedlyApi.class);

        // The result of a find link request is the embedly response, or a failure
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

        // Also create the "in flight" result automatically (todo: is this right way to do this?)
        ObservableTransformer<FindLinkAction, FindLinkResult> inProgress =
                actions -> actions
                        .delay(200, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                        .map(action -> FindLinkResult.inFlight(action.getUrl()))
                        .observeOn(AndroidSchedulers.mainThread());

        // The result of the collapse link action is just a copy of the action
        ObservableTransformer<CollapseLinkAction, CollapseLinkResult> collapse =
               actions -> actions
                           .map(action -> CollapseLinkResult.collapse(action))
                           .observeOn(AndroidSchedulers.mainThread());

        // Merge all Actions into a sequence of Results
        ObservableTransformer<PostAction, PostResult> postResults
               = events -> events.publish(shared -> Observable.merge(
                       shared.ofType(FindLinkAction.class).compose(findLink),
                       shared.ofType(FindLinkAction.class).compose(inProgress),
                       shared.ofType(CollapseLinkAction.class).compose(collapse)
               ));
        return postResults;
    }
}
