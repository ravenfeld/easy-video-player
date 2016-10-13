package com.afollestad.easyvideoplayer.internal;


public interface  FragmentCallback {

    void onEnter(PlayerView player) ;

    void onExit(PlayerView player);

    void onCreatedView(PlayerView player) ;
}
