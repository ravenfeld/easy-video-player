package com.ravenfeld.easyvideoplayer;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.ravenfeld.easyvideoplayer.internal.EasyVideoFragment;
import com.ravenfeld.easyvideoplayer.internal.FragmentCallback;
import com.ravenfeld.easyvideoplayer.internal.PlayerView;
import com.ravenfeld.easyvideoplayer.internal.Util;

import java.lang.ref.WeakReference;

public class EasyVideoPlayer extends FrameLayout implements FragmentCallback, IUserMethods {

    private static final String TAG = "EasyVideoPlayer";
    public static final String TAG_FULLSCREEN = "FULLSCREEN_";
    public static final String TAG_CONTENT = "CONTENT_";
    private WeakReference<EasyVideoCallback> callback;
    private Uri mSource;
    @PlayerView.LeftAction
    private int mLeftAction;
    @PlayerView.RightAction
    private int mRightAction;
    private CharSequence mRetryText;
    private CharSequence mSubmitText;
    private Drawable mRestartDrawable;
    private Drawable mPlayDrawable;
    private Drawable mPauseDrawable;
    private Drawable mFullScreenDrawable;
    private Drawable mFullScreenExitDrawable;
    private CharSequence mCustomLabelText;
    private CharSequence mBottomLabelText;
    private boolean mHideControlsOnPlay;
    private boolean mAutoPlay;
    private boolean mControlsDisabled;
    private boolean mSeekBarEnabled;
    private int mThemeColor;
    private boolean mAutoRotateInFullscreen;
    private float mVideoSizeLoading;
    private int mInitialPosition = 0;
    private boolean mIsVideoOnly = false;
    private PlayerView playerView;

    public EasyVideoPlayer(Context context) {
        this(context, null);
    }

    public EasyVideoPlayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EasyVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.EasyVideoPlayer,
                    0, 0);
            try {
                String source = a.getString(R.styleable.EasyVideoPlayer_source);
                if (source != null && !source.trim().isEmpty()) {
                    mSource = Uri.parse(source);
                }
                mLeftAction = a.getInteger(R.styleable.EasyVideoPlayer_leftAction, playerView.LEFT_ACTION_NONE);
                mRightAction = a.getInteger(R.styleable.EasyVideoPlayer_rightAction, playerView.RIGHT_ACTION_NONE);

                mCustomLabelText = a.getText(R.styleable.EasyVideoPlayer_customLabelText);
                mRetryText = a.getText(R.styleable.EasyVideoPlayer_retryText);
                mSubmitText = a.getText(R.styleable.EasyVideoPlayer_submitText);
                mBottomLabelText = a.getText(R.styleable.EasyVideoPlayer_bottomText);

                int restartDrawableResId = a.getResourceId(R.styleable.EasyVideoPlayer_restartDrawable, -1);
                int playDrawableResId = a.getResourceId(R.styleable.EasyVideoPlayer_playDrawable, -1);
                int pauseDrawableResId = a.getResourceId(R.styleable.EasyVideoPlayer_pauseDrawable, -1);
                int fullscreenDrawableResId = a.getResourceId(R.styleable.EasyVideoPlayer_fullscreenDrawable, -1);
                int fullscreenExitDrawableResId = a.getResourceId(R.styleable.EasyVideoPlayer_fullscreenExitDrawable, -1);

                if (restartDrawableResId != -1) {
                    mRestartDrawable = AppCompatResources.getDrawable(context, restartDrawableResId);
                }
                if (playDrawableResId != -1) {
                    mPlayDrawable = AppCompatResources.getDrawable(context, playDrawableResId);
                }
                if (pauseDrawableResId != -1) {
                    mPauseDrawable = AppCompatResources.getDrawable(context, pauseDrawableResId);
                }
                if (fullscreenDrawableResId != -1) {
                    mFullScreenDrawable = AppCompatResources.getDrawable(context, fullscreenDrawableResId);
                }
                if (fullscreenExitDrawableResId != -1) {
                    mFullScreenExitDrawable = AppCompatResources.getDrawable(context, fullscreenExitDrawableResId);
                }

                mHideControlsOnPlay = a.getBoolean(R.styleable.EasyVideoPlayer_hideControlsOnPlay, true);
                mAutoPlay = a.getBoolean(R.styleable.EasyVideoPlayer_autoPlay, false);
                mControlsDisabled = a.getBoolean(R.styleable.EasyVideoPlayer_disableControls, false);
                mSeekBarEnabled = a.getBoolean(R.styleable.EasyVideoPlayer_enableSeekBar, true);

                mThemeColor = a.getColor(R.styleable.EasyVideoPlayer_themeColor,
                        Util.resolveColor(context, R.attr.colorPrimary));

                mAutoRotateInFullscreen = a.getBoolean(R.styleable.EasyVideoPlayer_autoRotateInFullscreen, false);

                mIsVideoOnly = a.getBoolean(R.styleable.EasyVideoPlayer_startFullscreen, false);

                mVideoSizeLoading = a.getFloat(R.styleable.EasyVideoPlayer_videoSizeLoading, 16f / 10f);

            } finally {
                a.recycle();
            }
        } else {
            mLeftAction = playerView.LEFT_ACTION_NONE;
            mRightAction = playerView.RIGHT_ACTION_NONE;
            mHideControlsOnPlay = true;
            mAutoPlay = false;
            mControlsDisabled = false;
            mThemeColor = Util.resolveColor(context, R.attr.colorPrimary);
            mAutoRotateInFullscreen = false;
            mIsVideoOnly = false;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, "onAttachedToWindow: ");
        }
        EasyVideoFragment fragment = null;
        if (((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag(TAG_FULLSCREEN + getId()) != null) {
            fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag(TAG_FULLSCREEN + getId());
            if (callback != null && callback.get() != null) {
                fragment.setCallback(callback.get());
            }
            fragment.setFragmentCallback(EasyVideoPlayer.this);
        } else if (((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag(TAG_CONTENT + getId()) != null) {
            fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag(TAG_CONTENT + getId());
            if (callback != null && callback.get() != null) {
                fragment.setCallback(callback.get());
            }
            fragment.setFragmentCallback(EasyVideoPlayer.this);
            if (fragment.isDetached()) {
                ((AppCompatActivity) getContext()).getSupportFragmentManager().beginTransaction().attach(fragment).commitAllowingStateLoss();
            }
            if (!fragment.isAdded()) {
                ((AppCompatActivity) getContext()).getSupportFragmentManager().beginTransaction().replace(getId(), fragment, TAG_CONTENT + getId()).commitAllowingStateLoss();
            }
        }

        if (fragment == null) {

            fragment = EasyVideoFragment.newInstance(mIsVideoOnly, mAutoRotateInFullscreen);
            if (callback != null && callback.get() != null) {
                fragment.setCallback(callback.get());
            }
            fragment.setFragmentCallback(EasyVideoPlayer.this);
            if (mIsVideoOnly) {
                fragment.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), TAG_FULLSCREEN + getId());
            } else {
                ((AppCompatActivity) getContext()).getSupportFragmentManager().beginTransaction().replace(getId(), fragment, TAG_CONTENT + getId()).commitAllowingStateLoss();
            }
        }
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, "onDetachedFromWindow: ");
        }

        EasyVideoFragment fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag(TAG_CONTENT + getId());
        if (fragment != null) {
            if (fragment != null && !fragment.isDetached() && fragment.isResumed()) {
                ((AppCompatActivity) getContext()).getSupportFragmentManager().beginTransaction().detach(fragment).commitAllowingStateLoss();
            }
        }
    }

    @Override
    public void setSource(@NonNull Uri source) {
        mSource = source;
        if (playerView != null) {
            playerView.setSource(mSource);
        }
    }

    @Override
    public void setCallback(@NonNull EasyVideoCallback callback) {
        this.callback = new WeakReference<>(callback);
        EasyVideoFragment fragment;
        if (((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag(TAG_CONTENT + getId()) != null) {
            fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag(TAG_CONTENT + getId());
            if (callback != null && this.callback.get() != null) {
                fragment.setCallback(this.callback.get());
            }
        }
    }

    @Override
    public void setLeftAction(@PlayerView.LeftAction int action) {
        mLeftAction = action;
        if (playerView != null) {
            playerView.setLeftAction(mLeftAction);
        }
    }

    @Override
    public void setRightAction(@PlayerView.RightAction int action) {
        mRightAction = action;
        if (playerView != null) {
            playerView.setRightAction(mRightAction);
        }
    }

    @Override
    public void setCustomLabelText(@Nullable CharSequence text) {
        mCustomLabelText = text;
        if (playerView != null) {
            playerView.setCustomLabelText(mCustomLabelText);
        }
    }

    @Override
    public void setCustomLabelTextRes(@StringRes int textRes) {
        setCustomLabelText(getResources().getText(textRes));
    }

    @Override
    public void setBottomLabelText(@Nullable CharSequence text) {
        mBottomLabelText = text;
        if (playerView != null) {
            playerView.setBottomLabelText(mBottomLabelText);
        }
    }

    @Override
    public void setBottomLabelTextRes(@StringRes int textRes) {
        setBottomLabelText(getResources().getText(textRes));
    }

    @Override
    public void setRetryText(@Nullable CharSequence text) {
        mRetryText = text;
        if (playerView != null) {
            playerView.setRetryText(mRetryText);
        }
    }

    @Override
    public void setRetryTextRes(@StringRes int textRes) {
        setRetryText(getResources().getText(textRes));
    }

    @Override
    public void setSubmitText(@Nullable CharSequence text) {
        mSubmitText = text;
        if (playerView != null) {
            playerView.setSubmitText(mSubmitText);
        }
    }

    @Override
    public void setSubmitTextRes(@StringRes int textRes) {
        setSubmitText(getResources().getText(textRes));
    }

    @Override
    public void setRestartDrawable(@NonNull Drawable drawable) {
        mRestartDrawable = drawable;
        if (playerView != null) {
            playerView.setRestartDrawable(mRestartDrawable);
        }
    }

    @Override
    public void setRestartDrawableRes(@DrawableRes int res) {
        setRestartDrawable(AppCompatResources.getDrawable(getContext(), res));
    }

    @Override
    public void setPlayDrawable(@NonNull Drawable drawable) {
        mPlayDrawable = drawable;
        if (playerView != null) {
            playerView.setPlayDrawable(mPlayDrawable);
        }
    }

    @Override
    public void setPlayDrawableRes(@DrawableRes int res) {
        setPlayDrawable(AppCompatResources.getDrawable(getContext(), res));
    }

    @Override
    public void setPauseDrawable(@NonNull Drawable drawable) {
        mPauseDrawable = drawable;
        if (playerView != null) {
            playerView.setPauseDrawable(mPauseDrawable);
        }
    }

    @Override
    public void setPauseDrawableRes(@DrawableRes int res) {
        setPauseDrawable(AppCompatResources.getDrawable(getContext(), res));
    }

    @Override
    public void setFullScreenDrawable(@NonNull Drawable drawable) {
        mFullScreenDrawable = drawable;
        if (playerView != null) {
            playerView.setFullScreenDrawable(mFullScreenDrawable);
        }
    }

    @Override
    public void setFullScreenDrawableRes(@DrawableRes int res) {
        setFullScreenDrawable(AppCompatResources.getDrawable(getContext(), res));
    }

    @Override
    public void setFullScreenExitDrawable(@NonNull Drawable drawable) {
        mFullScreenExitDrawable = drawable;
        if (playerView != null) {
            playerView.setFullScreenExitDrawable(mFullScreenExitDrawable);
        }
    }

    @Override
    public void setFullScreenExitDrawableRes(@DrawableRes int res) {
        setFullScreenExitDrawable(AppCompatResources.getDrawable(getContext(), res));
    }

    @Override
    public void setThemeColor(@ColorInt int color) {
        mThemeColor = color;
        if (playerView != null) {
            playerView.setThemeColor(mThemeColor);
        }
    }

    @Override
    public void setThemeColorRes(@ColorRes int colorRes) {
        setThemeColor(ContextCompat.getColor(getContext(), colorRes));
    }

    @Override
    public void setHideControlsOnPlay(boolean hide) {
        mHideControlsOnPlay = hide;
        if (playerView != null) {
            playerView.setHideControlsOnPlay(mHideControlsOnPlay);
        }
    }

    @Override
    public void setAutoPlay(boolean autoPlay) {
        mAutoPlay = autoPlay;
        if (playerView != null) {
            playerView.setAutoPlay(mAutoPlay);
        }
    }

    @Override
    public boolean isAutoPlay() {
        return mAutoPlay;
    }

    @Override
    public void setInitialPosition(@IntRange(from = 0, to = Integer.MAX_VALUE) int pos) {
        mInitialPosition = pos;
        if (playerView != null) {
            playerView.setInitialPosition(mInitialPosition);
        }
    }

    @Override
    public void showControls() {
        if (playerView != null) {
            playerView.showControls();
        }
    }

    @Override
    public void hideControls() {
        if (playerView != null) {
            playerView.hideControls();
        }
    }

    @Override
    public boolean isControlsShown() {
        if (playerView != null) {
            return playerView.isControlsShown();
        }
        return false;
    }

    @Override
    public void toggleControls() {
        if (playerView != null) {
            playerView.toggleControls();
        }
    }

    @Override
    public void enableControls(boolean andShow) {
        mControlsDisabled = andShow;
        if (playerView != null) {
            playerView.enableControls(mControlsDisabled);
        }
    }

    @Override
    public void disableControls() {
        if (playerView != null) {
            playerView.disableControls();
        }
    }

    @Override
    public void setEnabledSeekBar(boolean enabled) {
        mSeekBarEnabled = enabled;
        if (playerView != null) {
            playerView.setEnabledSeekBar(mSeekBarEnabled);
        }
    }

    @Override
    public boolean isPrepared() {
        if (playerView != null) {
            return playerView.isPrepared();
        }
        return false;
    }

    @Override
    public boolean isOnPreparing() {
        if (playerView != null) {
            return playerView.isOnPreparing();
        }
        return false;
    }

    @Override
    public boolean isPlaying() {
        if (playerView != null) {
            return playerView.isPlaying();
        }
        return false;
    }

    @Override
    public boolean isVideoOnly() {
        if (playerView != null) {
            return playerView.isVideoOnly();
        }
        return false;
    }

    @Override
    public int getCurrentPosition() {
        if (playerView != null) {
            return playerView.getCurrentPosition();
        }
        return -1;
    }

    @Override
    public int getDuration() {
        if (playerView != null) {
            return playerView.getDuration();
        }
        return -1;
    }

    @Override
    public void start() {
        if (playerView != null) {
            if (isPrepared()) {
                playerView.start();
            } else {
                playerView.setSource(mSource);
            }
        }
    }

    @Override
    public void restart() {
        if (playerView != null) {
            playerView.restart();
        }
    }

    @Override
    public void seekTo(@IntRange(from = 0, to = Integer.MAX_VALUE) int pos) {
        if (playerView != null) {
            playerView.seekTo(pos);
        }
    }

    @Override
    public void setVolume(@FloatRange(from = 0f, to = 1f) float leftVolume, @FloatRange(from = 0f, to = 1f) float rightVolume) {
        if (playerView != null) {
            playerView.setVolume(leftVolume, rightVolume);
        }
    }

    @Override
    public void pause() {
        if (playerView != null) {
            playerView.pause();
        }
    }

    @Override
    public void stop() {
        if (playerView != null) {
            playerView.stop();
        }
    }

    @Override
    public void reset() {
        mInitialPosition = getCurrentPosition() > 0 ? getCurrentPosition() : mInitialPosition;
        if (playerView != null) {
            playerView.reset();
        }
    }

    @Override
    public void enterFullscreen() {
        playerView.enterFullscreen();
    }

    @Override
    public void exitFullscreen() {
        playerView.exitFullscreen();
    }

    @Override
    public void release() {
        if (playerView != null) {
            playerView.release();
        }
        if (callback != null) {
            callback.clear();
        }
        EasyVideoFragment fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag(TAG_CONTENT + getId());
        if (fragment != null) {
            ((AppCompatActivity) getContext()).getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
        }
    }

    public void setAutoRotateInFullscreen(boolean autoFullScreen) {
        mAutoRotateInFullscreen = autoFullScreen;
    }

    @Override
    public void setVideoSizeLoading(float videoSizeLoading) {
        mVideoSizeLoading = videoSizeLoading;
        if (playerView != null) {
            playerView.setVideoSizeLoading(mVideoSizeLoading);
        }
    }

    @Override
    public void onEnter(PlayerView player) {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, "onEnter");
        }
        mIsVideoOnly = true;
        mInitialPosition = getCurrentPosition() >= 0 ? getCurrentPosition() : mInitialPosition;
        mAutoPlay = getCurrentPosition() >= 0 ? playerView.isWasPlaying() : mAutoPlay;

        EasyVideoFragment fragmentFull;

        fragmentFull = EasyVideoFragment.newInstance(true, mAutoRotateInFullscreen);

        fragmentFull.setPlayer(playerView);
        if (callback != null && callback.get() != null) {
            fragmentFull.setCallback(callback.get());
        }
        fragmentFull.setFragmentCallback(this);

        fragmentFull.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), TAG_FULLSCREEN + getId());


        if (((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag(TAG_CONTENT + getId()) != null) {
            EasyVideoFragment fragmentMini = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag(TAG_CONTENT + getId());
            player.pause();
            fragmentMini.setPlayer(null);
        }
        if (callback != null && callback.get() != null) {
            callback.get().onFullScreen(player);
        }
    }

    @Override
    public void onExit(PlayerView player) {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, "onExit");
        }
        mIsVideoOnly = false;
        mInitialPosition = getCurrentPosition() >= 0 ? getCurrentPosition() : mInitialPosition;
        mAutoPlay = getCurrentPosition() >= 0 ? playerView.isWasPlaying() : mAutoPlay;

        EasyVideoFragment fragment;
        if (((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag(TAG_CONTENT + getId()) != null) {
            fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag(TAG_CONTENT + getId());

            if (fragment.isDetached()) {
                ((AppCompatActivity) getContext()).getSupportFragmentManager().beginTransaction().attach(fragment).commitAllowingStateLoss();
                fragment.setPlayer(player);
            }
            if (!fragment.isAdded()) {
                ((AppCompatActivity) getContext()).getSupportFragmentManager().beginTransaction().replace(getId(), fragment, TAG_CONTENT + getId()).commitAllowingStateLoss();
            }

        } else {
            fragment = EasyVideoFragment.newInstance(false, mAutoRotateInFullscreen);

            ((AppCompatActivity) getContext()).getSupportFragmentManager().beginTransaction().replace(getId(), fragment, TAG_CONTENT + getId()).commitAllowingStateLoss();

        }

        fragment.setPlayer(player);
        if (callback != null && callback.get() != null) {
            fragment.setCallback(callback.get());
        }
        fragment.setFragmentCallback(EasyVideoPlayer.this);

        if (callback != null && callback.get() != null) {
            callback.get().onFullScreenExit(player);
        }
    }

    @Override
    public void onCreatedView(PlayerView player) {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, "onCreatedView: " + mInitialPosition + " autoPlay " + mAutoPlay + " video only " + mIsVideoOnly + " source " + mSource);
        }
        playerView = player;
        if (mSource != null) {
            player.setSource(mSource);
        }
        player.setLeftAction(mLeftAction);
        player.setRightAction(mRightAction);
        if (mRetryText != null) {
            player.setRetryText(mRetryText);
        }
        if (mSubmitText != null) {
            player.setSubmitText(mSubmitText);
        }
        if (mRestartDrawable != null) {
            player.setRestartDrawable(mRestartDrawable);
        }
        if (mPlayDrawable != null) {
            player.setPlayDrawable(mPlayDrawable);
        }
        if (mPauseDrawable != null) {
            player.setPauseDrawable(mPauseDrawable);
        }
        if (mFullScreenDrawable != null) {
            player.setFullScreenDrawable(mFullScreenDrawable);
        }
        if (mFullScreenExitDrawable != null) {
            player.setFullScreenExitDrawable(mFullScreenExitDrawable);
        }
        if (mCustomLabelText != null) {
            player.setCustomLabelText(mCustomLabelText);
        }
        if (mBottomLabelText != null) {
            player.setBottomLabelText(mBottomLabelText);
        }
        player.setHideControlsOnPlay(mHideControlsOnPlay);
        player.setAutoPlay(mAutoPlay);
        player.setInitialPosition(mInitialPosition);
        if (mControlsDisabled) {
            player.disableControls();
        } else {
            player.enableControls(true);
        }
        player.setEnabledSeekBar(mSeekBarEnabled);
        player.setThemeColor(mThemeColor);
        player.setVideoSizeLoading(mVideoSizeLoading);

    }

    @Override
    public void onPlayerInitBefore(PlayerView player) {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, "onPlayerInitBefore: ");
        }
        playerView = player;
        onRestoreView(player);

    }

    @Override
    public void onRestoreView(PlayerView player) {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, "onRestoreView: ");
        }
        onCreatedView(player);
        if (playerView.isPrepared() && mAutoPlay) {
            playerView.start();
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        EasyVideoPlayer.SavedState ss = new EasyVideoPlayer.SavedState(superState);
        ss.position = getCurrentPosition() > 0 ? getCurrentPosition() : mInitialPosition;
        ss.play = getCurrentPosition() > 0 ? playerView.isWasPlaying() : mAutoPlay;
        ss.videoOnly = isVideoOnly();
        ss.source = (mSource != null) ? mSource.toString() : "";
        ss.id = getId();
        ss.autoRotate = mAutoRotateInFullscreen;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof EasyVideoPlayer.SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        EasyVideoPlayer.SavedState ss = (EasyVideoPlayer.SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        mInitialPosition = ss.position;
        mAutoPlay = ss.play;
        mIsVideoOnly = ss.videoOnly;
        mSource = Uri.parse(ss.source);
        setId(ss.id);
        mAutoRotateInFullscreen = ss.autoRotate;
    }

    static class SavedState extends BaseSavedState {
        int position;
        boolean play;
        boolean videoOnly;
        String source;
        int id;
        boolean autoRotate;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            position = in.readInt();
            play = in.readInt() != 0;
            videoOnly = in.readInt() != 0;
            source = in.readString();
            id = in.readInt();
            autoRotate = in.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(position);
            out.writeInt(play ? 1 : 0);
            out.writeInt(videoOnly ? 1 : 0);
            out.writeString(source);
            out.writeInt(id);
            out.writeInt(autoRotate ? 1 : 0);
        }

        public static final Parcelable.Creator<EasyVideoPlayer.SavedState> CREATOR =
                new Parcelable.Creator<EasyVideoPlayer.SavedState>() {
                    @Override
                    public EasyVideoPlayer.SavedState createFromParcel(Parcel in) {
                        return new EasyVideoPlayer.SavedState(in);
                    }

                    @Override
                    public EasyVideoPlayer.SavedState[] newArray(int size) {
                        return new EasyVideoPlayer.SavedState[size];
                    }
                };
    }
}
