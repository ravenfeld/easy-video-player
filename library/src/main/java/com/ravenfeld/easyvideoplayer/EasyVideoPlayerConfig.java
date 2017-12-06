package com.ravenfeld.easyvideoplayer;

public class EasyVideoPlayerConfig {
    private static boolean isDebug = false;

    public static void setDebug(boolean debug) {
        isDebug = debug;
    }

    public static boolean isDebug() {
        return isDebug;
    }
}
