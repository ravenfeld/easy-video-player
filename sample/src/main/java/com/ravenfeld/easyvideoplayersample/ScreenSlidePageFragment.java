package com.ravenfeld.easyvideoplayersample;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ravenfeld.easyvideoplayer.EasyVideoCallback;
import com.ravenfeld.easyvideoplayer.EasyVideoPlayer;
import com.ravenfeld.easyvideoplayer.internal.PlayerView;


public class ScreenSlidePageFragment extends Fragment {
    private static final String TAG = "ScreenSlidePageFragment";
    private static final String URL = "URL";
    private static final String VIDEO = "VIDEO";
    private EasyVideoPlayer videoView;
    private FrameLayout container;
    private String url;
    private String urlSmart;
    private Parcelable save;
    private static final int LOADER_FINISHED = 1;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == LOADER_FINISHED) {
                videoView.setId(Math.abs(url.hashCode()));

                videoView.setCallback(new EasyVideoCallback() {
                    @Override
                    public void onStarted(PlayerView player) {
                        super.onStarted(player);
                    }

                    @Override
                    public void onPrepared(PlayerView player) {
                        super.onPrepared(player);
                        if (getUserVisibleHint()) {
                            videoView.hideControls();
                            videoView.start();
                        }
                    }

                    @Override
                    public boolean onPreparing(PlayerView player) {
                        return getUserVisibleHint();
                    }
                });
                if (save != null) {
                    videoView.onRestoreInstanceState(save);
                }
                videoView.setSource(Uri.parse(url));
                videoView.setAutoRotateInFullscreen(true);

                if (videoView.getParent() == null) {
                    container.addView(videoView);
                }


            }
        }
    };

    public static ScreenSlidePageFragment newInstance(String url) {

        final ScreenSlidePageFragment frag = new ScreenSlidePageFragment();
        final Bundle b = new Bundle();
        b.putString(URL, url);
        frag.setArguments(b);
        return frag;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = this.getArguments();
        if (args != null) {
            this.url = args.getString(URL);
            this.urlSmart = url.substring(url.lastIndexOf("/"));
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_slide, container, false);
        this.container = (FrameLayout) root.findViewById(R.id.content);
        TextView textView = (TextView) root.findViewById(R.id.title);
        textView.setText(urlSmart);
        videoView = (EasyVideoPlayer) inflater.inflate(R.layout.view_videoview, this.container, false);
        if (savedInstanceState != null) {
            save = savedInstanceState.getParcelable(VIDEO);
        }

        return root;
    }


    @Override
    public void onResume() {
        super.onResume();
        handler.sendEmptyMessageDelayed(LOADER_FINISHED, 500);

    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisible()) {
            if (isVisibleToUser) {
                if (videoView != null)
                    videoView.start();
            } else {
                videoView.reset();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (videoView != null) {
            outState.putParcelable(VIDEO, videoView.onSaveInstanceState());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {

        super.onViewStateRestored(savedInstanceState);
    }
}

