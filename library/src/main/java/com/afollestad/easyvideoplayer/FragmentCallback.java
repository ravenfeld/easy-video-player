package com.afollestad.easyvideoplayer;

import android.net.Uri;


public interface  FragmentCallback {

    void onFullScreen(EasyVideoPlayer player) ;

    void onFullScreenExit(EasyVideoPlayer player);

    void onCreatedView(EasyVideoPlayer player) ;
}