package com.ravenfeld.easyvideoplayer.internal;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ravenfeld.easyvideoplayer.BuildConfig;
import com.ravenfeld.easyvideoplayer.EasyVideoCallback;
import com.ravenfeld.easyvideoplayer.R;


public class EasyVideoFragment extends DialogFragment implements InternalCallback {

    private static final String FULLSCREEN = "FULLSCREEN";
    private static final String AUTO_ROTATE_IN_FULLSCREEN = "AUTO_ROTATE_IN_FULLSCREEN";
    private static final String HAS_PLAYER = "HAS_PLAYER";
    private static final String TAG = "EasyVideoFragment";
    private EasyVideoCallback callback;
    private FragmentCallback fragmentCallback;
    private PlayerView playerView;
    private boolean hasPlayer = false;
    private boolean fullscreen = false;
    private boolean autoRotateInFullscreen = false;
    private static int saveOrientation = 0;


    public static EasyVideoFragment newInstance(boolean fullscreen, boolean autoRotateInFullscreen) {
        EasyVideoFragment fragment = new EasyVideoFragment();
        Bundle args = new Bundle();
        args.putBoolean(FULLSCREEN, fullscreen);
        args.putBoolean(AUTO_ROTATE_IN_FULLSCREEN, autoRotateInFullscreen);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen);
        Bundle args = getArguments();
        fullscreen = args.getBoolean(FULLSCREEN, false);
        autoRotateInFullscreen = args.getBoolean(AUTO_ROTATE_IN_FULLSCREEN, false);
        if (savedInstanceState != null) {
            EasyVideoFragment fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag("CONTENT_" + getId());
            if (fragment != null) {
                ((AppCompatActivity) getContext()).getSupportFragmentManager().beginTransaction().remove(fragment).commit();
            }
        }

    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        boolean player = true;
        if (savedInstanceState != null) {
            player = savedInstanceState.getBoolean(HAS_PLAYER, true);
        }
        RelativeLayout view;
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onCreateView: " + (playerView == null) + " " + player);
        }
        if (playerView == null && player) {
            view = (RelativeLayout) inflater.inflate(R.layout.evp_fragment_player, container, false);
            playerView = (PlayerView) view.findViewById(R.id.player);
            if (fragmentCallback != null && playerView != null) {
                fragmentCallback.onCreatedView(playerView);
            }
        } else {
            view = (RelativeLayout) inflater.inflate(R.layout.evp_fragment_empty, container, false);
            if (playerView != null) {
                ((ViewGroup) playerView.getParent()).removeView(playerView);
                view.addView(playerView);
            }
        }
        TextView format = (TextView) view.findViewById(R.id.identifier);
        if (BuildConfig.DEBUG) {
            if (!fullscreen) {
                format.setText(getString(R.string.mini));
            } else {
                format.setText(getString(R.string.fullscreen));
            }
            format.setVisibility(View.VISIBLE);
            format.bringToFront();
        } else {
            format.setVisibility(View.GONE);
        }

        if (playerView != null) {
            playerView.setCallback(callback);
            playerView.setFullScreenCallback(this);
            playerView.setVideoOnly(fullscreen);
            hasPlayer = true;
        }

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog d = super.onCreateDialog(savedInstanceState);
        d.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    onFullScreenExit(playerView);
                    return true;
                }
                return false;
            }
        });
        return d;
    }

    private int switchDecorView(boolean fullscreen) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && getActivity() != null && getActivity().isInMultiWindowMode()) {
            return getActivity().getWindow().getDecorView().getSystemUiVisibility();
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            if (fullscreen && autoRotateInFullscreen) {
                return
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        ;
            } else {
                return View.SYSTEM_UI_FLAG_VISIBLE;
            }
        } else {
            return getActivity().getWindow().getDecorView().getSystemUiVisibility();
        }
    }

    public void setCallback(@NonNull EasyVideoCallback callback) {
        this.callback = callback;
    }

    public void setFragmentCallback(@NonNull FragmentCallback fragmentCallback) {
        if (playerView != null) {
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
            ((RelativeLayout) getView()).addView(playerView);
            getView().findViewById(R.id.identifier).bringToFront();
            playerView.setCallback(callback);
            playerView.setFullScreenCallback(this);
            hasPlayer = true;
        } else {
            hasPlayer = false;
        }
    }


    @Override
    public void onFullScreen(PlayerView player) {
        playerView.setVideoOnly(true);
        autoRotateInFullscreen = player.getAutoRotateInFullscreen();
        Activity a = getActivity();
        if (a != null) {
            saveOrientation = a.getRequestedOrientation();
            a.getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(switchDecorView(true));
        }

        if (fragmentCallback != null) {
            fragmentCallback.onEnter(player);
        }

        if (a != null && player.getAutoRotateInFullscreen()) {
            a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    @Override
    public void onFullScreenExit(PlayerView player) {
        playerView.setVideoOnly(false);
        Activity a = getActivity();
        if (a != null) {
            a.getWindow().getDecorView().setSystemUiVisibility(switchDecorView(false));
        }
        hasPlayer = false;
        if (getDialog() != null)
            dismiss();
        if (fragmentCallback != null) {
            fragmentCallback.onExit(player);
        }
        if (a != null && player.getAutoRotateInFullscreen()) {
            a.setRequestedOrientation(saveOrientation);
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(HAS_PLAYER, hasPlayer);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onPause: ");
        }
        if (hasPlayer && playerView != null) {
            playerView.onPause();
        }
        super.onPause();
    }


    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            getDialog().getWindow().getDecorView()
                    .setSystemUiVisibility(switchDecorView(true));
        }
    }

    @Override
    public void onResume() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onResume: ");
        }
        super.onResume();
        if (hasPlayer && playerView != null) {
            playerView.onResume();
        }

    }


    @Override
    public void onRestoreInstance(PlayerView player) {
        if (fragmentCallback != null) {
            fragmentCallback.onRestoreView(player);
        }
    }
}
