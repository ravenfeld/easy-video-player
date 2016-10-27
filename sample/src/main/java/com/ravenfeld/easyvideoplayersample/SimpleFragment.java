package com.ravenfeld.easyvideoplayersample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.ravenfeld.easyvideoplayer.EasyVideoPlayer;

public class SimpleFragment extends AppCompatActivity {


    private static final String TAG = "ChooseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate: ");

        setContentView(R.layout.activity_main_fragment);
    }


}