package com.ravenfeld.easyvideoplayersample;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;

import com.ravenfeld.easyvideoplayer.EasyVideoPlayer;


public class TestFragment extends Fragment {
    private static final String VIDEO = "VIDEO";

    EasyVideoPlayer videoView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_empty, container, false);
        FrameLayout content = (FrameLayout) root.findViewById(R.id.content);
        videoView = (EasyVideoPlayer) inflater.inflate(R.layout.view_videoview, container, false);
        if (savedInstanceState != null) {
            Parcelable save = savedInstanceState.getParcelable(VIDEO);
            videoView.onRestoreInstanceState(save);
        } else {
            videoView.setAutoPlay(true);
            videoView.setSource(Uri.parse("asset://big_buck_bunny.mp4"));
        }
        content.addView(videoView);
        return content;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(VIDEO, videoView.onSaveInstanceState());
        super.onSaveInstanceState(outState);
    }

}


