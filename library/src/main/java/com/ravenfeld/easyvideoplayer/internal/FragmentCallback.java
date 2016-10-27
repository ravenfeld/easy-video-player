package com.ravenfeld.easyvideoplayer.internal;


public interface FragmentCallback {

    void onEnter(PlayerView player);

    void onExit(PlayerView player);

    void onCreatedView(PlayerView player);

    void onPlayerInitBefore(PlayerView player);

    void onRestoreView(PlayerView playerView);

}
