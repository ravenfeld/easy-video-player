package com.ravenfeld.easyvideoplayer.internal;


public interface InternalCallback {

    void onFullScreen(PlayerView player);

    void onFullScreenExit(PlayerView player);

    void onRestoreInstance(PlayerView player);
}