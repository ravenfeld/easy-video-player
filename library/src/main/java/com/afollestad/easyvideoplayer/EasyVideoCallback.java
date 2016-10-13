package com.afollestad.easyvideoplayer;

import android.net.Uri;

import com.afollestad.easyvideoplayer.internal.PlayerView;

public abstract class EasyVideoCallback {

    public void onStarted(PlayerView player){

    }

    public void onPaused(PlayerView player){

    }

    public void onPreparing(PlayerView player){

    }

    public void onPrepared(PlayerView player){

    }

    public void onBuffering(PlayerView player, int percent){

    }

    public void onError(PlayerView player, Exception e){

    }

    public void onCompletion(PlayerView player){

    }

    public void onRetry(PlayerView player, Uri source){

    }

    public void onSubmit(PlayerView player, Uri source){

    }

    public void onFullScreen(PlayerView player){

    }

    public void onFullScreenExit(PlayerView player){

    }

    public void onVideoProgressUpdate(PlayerView player, int position, int duration){

    }

}