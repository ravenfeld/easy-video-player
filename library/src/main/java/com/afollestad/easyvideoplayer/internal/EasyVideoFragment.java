package com.afollestad.easyvideoplayer.internal;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.afollestad.easyvideoplayer.BuildConfig;
import com.afollestad.easyvideoplayer.EasyVideoCallback;
import com.afollestad.easyvideoplayer.R;


public class EasyVideoFragment extends Fragment implements FullscreenCallback {

    private static final String FULLSCREEN = "FULLSCREEN";
    private static final String HAS_PLAYER = "HAS_PLAYER";
    private static final String TAG = "EasyVideoFragment";
    private EasyVideoCallback callback;
    private FragmentCallback fragmentCallback;
    private PlayerView playerView;
    private boolean hasPlayer = false;
    private boolean fullscreen;

    public static EasyVideoFragment newInstance(boolean fullscreen) {
        EasyVideoFragment fragment = new EasyVideoFragment();
        Bundle args = new Bundle();
        args.putBoolean(FULLSCREEN, fullscreen);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        boolean player = true;
        if (savedInstanceState != null) {
            player = savedInstanceState.getBoolean(HAS_PLAYER, true);
        }
        Bundle args = getArguments();
        fullscreen = args.getBoolean(FULLSCREEN, false);
        FrameLayout view;
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateView: " + (playerView == null) + " " + player);
        }
        if (playerView == null && player) {
            view = (FrameLayout) inflater.inflate(R.layout.evp_fragment_player, container, false);
            playerView = (PlayerView) view.findViewById(R.id.player);
            if (fragmentCallback != null) {
                fragmentCallback.onCreatedView(playerView);
            }
        } else {
            view = (FrameLayout) inflater.inflate(R.layout.evp_fragment_empty, container, false);
            if (playerView != null) {
                ((ViewGroup) playerView.getParent()).removeView(playerView);
                view.addView(playerView);
            }
        }

        if (playerView != null) {
            playerView.setCallback(callback);
            playerView.setFullScreenCallback(this);
            playerView.setVideoOnly(fullscreen);
            hasPlayer = true;
        }

        return view;
    }

    public void setCallback(@NonNull EasyVideoCallback callback) {
        this.callback = callback;
    }

    public void setFragmentCallback(@NonNull FragmentCallback fragmentCallback) {
        if (this.fragmentCallback == null && playerView != null) {
            fragmentCallback.onPlayerInitBefore(playerView);
        }
        this.fragmentCallback = fragmentCallback;

    }

    public void setPlayer(PlayerView player) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "setPlayer: ");
        }
        this.playerView = player;
        if (playerView != null && getView() != null) {
            ((ViewGroup) playerView.getParent()).removeView(playerView);
            ((FrameLayout) getView()).addView(playerView);
            playerView.setCallback(callback);
            playerView.setFullScreenCallback(this);
            hasPlayer = true;
        } else {
            hasPlayer = false;
        }
    }

    @Override
    public void onFullScreen(PlayerView player) {
        if (fragmentCallback != null) {
            fragmentCallback.onEnter(player);
        }
    }

    @Override
    public void onFullScreenExit(PlayerView player) {
        if (fragmentCallback != null) {
            fragmentCallback.onExit(player);
        }
        hasPlayer = false;
    }

    @Override
    public void onDetach() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onDetach: ");
        }
        if (!hasPlayer && playerView != null) {
            playerView.detach();
        } else if (!hasPlayer && playerView != null && fullscreen) {
            playerView.setVideoOnly(false);
            fragmentCallback.onExit(playerView);
        }
        super.onDetach();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(HAS_PLAYER, playerView != null);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (hasPlayer && playerView != null) {
            playerView.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasPlayer && playerView != null) {
            playerView.onResume();
        }
    }

    public void onAttachView() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onAttachView " + playerView + " " + hasPlayer);
        }
        if (hasPlayer && playerView != null) {
            playerView.attach();
        }
    }

}
