package com.ravenfeld.easyvideoplayersample;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.ravenfeld.easyvideoplayer.EasyVideoPlayerConfig;

public class SimpleActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EasyVideoPlayerConfig.setDebug(true);
        setContentView(R.layout.activity_main);
    }


}