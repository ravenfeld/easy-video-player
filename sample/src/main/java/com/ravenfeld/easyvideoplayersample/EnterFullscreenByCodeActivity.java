package com.ravenfeld.easyvideoplayersample;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.ravenfeld.easyvideoplayer.EasyVideoPlayer;
import com.ravenfeld.easyvideoplayer.EasyVideoPlayerConfig;

public class EnterFullscreenByCodeActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EasyVideoPlayerConfig.setDebug(true);
        setContentView(R.layout.activity_fullscreen_by_code);

        findViewById(R.id.enterFullscreenBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((EasyVideoPlayer) findViewById(R.id.toggleBunny)).enterFullscreen();
            }
        });
    }


}