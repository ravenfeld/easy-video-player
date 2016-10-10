package com.afollestad.easyvideoplayersample;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.afollestad.easyvideoplayer.EasyVideo;
import com.afollestad.easyvideoplayer.EasyVideoCallback;
import com.afollestad.easyvideoplayer.EasyVideoPlayer;
import com.afollestad.materialdialogs.MaterialDialog;

public class MainActivity extends AppCompatActivity implements EasyVideoCallback {

    private EasyVideo player;
    private EasyVideo player2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.content);
//        frameLayout.addView(new EasyVideoPlayer(this));
//
//        player = (EasyVideo) findViewById(R.id.player);
//        player2 = (EasyVideo) findViewById(R.id.player2);
//        //assert player != null;
//        player.setCallback(this);
//        player2.setCallback(this);
        // All further configuration is done from the XML layout.
    }

    @Override
    protected void onPause() {
        super.onPause();
        //player.pause();
    }

    @Override
    public void onStarted(EasyVideoPlayer player) {
    }

    @Override
    public void onPaused(EasyVideoPlayer player) {
    }

    @Override
    public void onPreparing(EasyVideoPlayer player) {
        Log.d("EVP-Sample", "onPreparing()");
    }

    @Override
    public void onPrepared(EasyVideoPlayer player) {
        Log.d("EVP-Sample", "onPrepared()");
    }

    @Override
    public void onBuffering(int percent) {
        Log.d("EVP-Sample", "onBuffering(): " + percent + "%");
    }

    @Override
    public void onError(EasyVideoPlayer player, Exception e) {
        Log.d("EVP-Sample", "onError(): " + e.getMessage());
        new MaterialDialog.Builder(this)
                .title(R.string.error)
                .content(e.getMessage())
                .positiveText(android.R.string.ok)
                .show();
    }

    @Override
    public void onCompletion(EasyVideoPlayer player) {
        Log.d("EVP-Sample", "onCompletion()");
    }

    @Override
    public void onRetry(EasyVideoPlayer player, Uri source) {
        Toast.makeText(this, "Retry", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSubmit(EasyVideoPlayer player, Uri source) {
        Toast.makeText(this, "Submit", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFullScreen(EasyVideoPlayer player) {
        Log.e("TEST", "FullScreen");
        //this.player.visibility(player);
        //this.player2.visibility(player);
    }

    @Override
    public void onFullScreenExit(EasyVideoPlayer player) {

    }

    @Override
    public void onCreatedView(EasyVideoPlayer player) {

    }

}