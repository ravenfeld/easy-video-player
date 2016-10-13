package com.afollestad.easyvideoplayer;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.afollestad.easyvideoplayer.internal.EasyVideoFragment;
import com.afollestad.easyvideoplayer.internal.PlayerView;
import com.afollestad.easyvideoplayer.internal.FragmentCallback;
import com.afollestad.easyvideoplayer.internal.Util;

public class EasyVideoPlayer extends FrameLayout implements FragmentCallback, IUserMethods {

    private EasyVideoCallback callback;
    private boolean init = false;
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
    private boolean mAutoFullscreen;
    private float mVideoSizeLoading;
    private int mInitialPosition = 0;
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
                if (source != null && !source.trim().isEmpty())
                    mSource = Uri.parse(source);

                //noinspection WrongConstant
                mLeftAction = a.getInteger(R.styleable.EasyVideoPlayer_leftAction, playerView.LEFT_ACTION_NONE);
                //noinspection WrongConstant
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

                mAutoFullscreen = a.getBoolean(R.styleable.EasyVideoPlayer_autoFullscreen, false);

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
            mAutoFullscreen = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!init) {
            init = true;
            EasyVideoFragment fragment;
            if (((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag("" + getId()) != null) {
                fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag("" + getId());
            } else if (((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentById(getId()) == null) {
                fragment = EasyVideoFragment.newInstance(false);
                ((AppCompatActivity) getContext()).getSupportFragmentManager().beginTransaction().add(getId(), fragment).commit();
            } else {
                fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentById(getId());
            }

            fragment.setCallback(callback);
            fragment.setFragmentCallback(this);

        }
    }

    @Override
    public void setSource(@NonNull Uri source) {
        mSource = source;
        if (playerView != null) {
            playerView.setSource(mSource);
        }
    }

    public void setCallback(@NonNull EasyVideoCallback callback) {
        this.callback = callback;
        EasyVideoFragment fragment;
        if (((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag("" + getId()) != null) {
            fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag("" + getId());
            fragment.setCallback(callback);
        } else if (((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentById(getId()) != null) {
            fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentById(getId());
            fragment.setCallback(callback);
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
    public boolean isPlaying() {
        if (playerView != null) {
            return playerView.isPlaying();
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
            playerView.start();
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
        if (playerView != null) {
            playerView.reset();
        }
    }

    @Override
    public void release() {
        if (playerView != null) {
            playerView.release();
        }
    }

    @Override
    public void setAutoFullscreen(boolean autoFullScreen) {
        mAutoFullscreen = autoFullScreen;
        if (playerView != null) {
            playerView.setAutoFullscreen(mAutoFullscreen);
        }
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

        EasyVideoFragment fragment;

        if (((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag("" + getId()) == null) {

            fragment = EasyVideoFragment.newInstance(true);
            ((AppCompatActivity) getContext()).getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment, "" + getId()).addToBackStack("VIDEO").commit();
        } else {
            fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag("" + getId());
        }
        fragment.setPlayer(player);
        fragment.setCallback(callback);
        fragment.setFragmentCallback(this);

        if (((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentById(getId()) != null) {
            fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentById(getId());
            fragment.setPlayer(null);
        }
        if (callback != null) callback.onFullScreen(player);
    }

    @Override
    public void onExit(PlayerView player) {
        if (((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentById(getId()) != null) {
            EasyVideoFragment fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentById(getId());
            fragment.setPlayer(player);
            fragment.setCallback(callback);
            fragment.setFragmentCallback(this);
            fragment.onAttach();
        }
        if (callback != null) {
            callback.onFullScreenExit(player);
        }
    }

    @Override
    public void onCreatedView(PlayerView player) {
        playerView = player;
        if (mSource != null) player.setSource(mSource);
        player.setLeftAction(mLeftAction);
        player.setRightAction(mRightAction);
        if (mRetryText != null) player.setRetryText(mRetryText);
        if (mSubmitText != null) player.setSubmitText(mSubmitText);
        if (mRestartDrawable != null) player.setRestartDrawable(mRestartDrawable);
        if (mPlayDrawable != null) player.setPlayDrawable(mPlayDrawable);
        if (mPauseDrawable != null) player.setPauseDrawable(mPauseDrawable);
        if (mFullScreenDrawable != null) player.setFullScreenDrawable(mFullScreenDrawable);
        if (mFullScreenExitDrawable != null)
            player.setFullScreenExitDrawable(mFullScreenExitDrawable);
        if (mCustomLabelText != null) player.setCustomLabelText(mCustomLabelText);
        if (mBottomLabelText != null) player.setBottomLabelText(mBottomLabelText);
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
        player.setAutoFullscreen(mAutoFullscreen);
        player.setVideoSizeLoading(mVideoSizeLoading);

    }

}
