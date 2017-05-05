package com.example.johnwilde.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class EmbedAdapter extends RecyclerView.Adapter<ViewHolder> {
    private Context mContext;
    OkHttpClient client = new OkHttpClient();

    private List<JSONObject> mList = new ArrayList<>();
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

        EmbedViewHolder(View view) {
            super(view);
            ButterKnife.inject(this, view);
        }

        void bindData(JSONObject object) {
            try {
                String url = object.get("thumbnail_url").toString();
                new DownloadImageTask(mImage).execute(url);
                mTitle.setText(object.getString("title"));
                mDescription.setText(object.getString("description"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.embed, parent, false);
        return new EmbedViewHolder(view);
    }

    public void add(String s) {
        try {
            JSONObject object = new JSONObject(s);
            mList.add(object);
        } catch (JSONException e) {
            e.printStackTrace();
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