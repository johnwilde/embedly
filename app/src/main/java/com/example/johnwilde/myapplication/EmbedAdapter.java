package com.example.johnwilde.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class EmbedAdapter extends RecyclerView.Adapter<ViewHolder> {
    private Context mContext;

    private PublishSubject<View> mViewClickSubject = PublishSubject.create();

    public Observable<View> getViewClickedObservable() {
        return mViewClickSubject;
    }

    private List<OembedResponse> mList = new ArrayList<>();

    EmbedAdapter(Context context) {
        mContext = context;
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {

            final OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(urls[0])
                    .build();

            Response response = null;
            Bitmap mIcon11 = null;
            try {
                response = client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (response.isSuccessful()) {
                try {
                    mIcon11 = BitmapFactory.decodeStream(response.body().byteStream());
                } catch (Exception e) {
                    Log.e("Error", e.getMessage());
                    e.printStackTrace();
                }

            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    class EmbedViewHolder extends ViewHolder {
        @InjectView(R.id.image)
        ImageView mImage;

        @InjectView(R.id.title)
        TextView mTitle;

        @InjectView(R.id.description)
        TextView mDescription;

        @InjectView(R.id.close_button)
        Button mButton;

        String mUrl;

        EmbedViewHolder(View view) {
            super(view);
            ButterKnife.inject(this, view);
        }

        void bindData(OembedResponse response) {
            String url = response.thumbnail_url;
            if (url != null) {
                new DownloadImageTask(mImage).execute(url);
            }
            mTitle.setText(response.title);
            mDescription.setText(response.description);
            mUrl = response.requestUrl;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.embed, parent, false);
        EmbedViewHolder viewHolder = new EmbedViewHolder(view);
        RxView.clicks(viewHolder.mButton)
                .takeUntil(RxView.detaches(parent))
                .map(aVoid -> view)
                .subscribe(mViewClickSubject);

        return viewHolder;
    }

    public void add(OembedResponse response) {
        mList.add(response);
        notifyDataSetChanged();
    }

    public void remove(String url) {
        for (OembedResponse response : mList) {
            if (response.requestUrl.equals(url)) {
                mList.remove(response);
                notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ((EmbedViewHolder) holder).bindData(mList.get(position));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }
}