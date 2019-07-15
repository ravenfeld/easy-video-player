package com.ravenfeld.easyvideoplayer.internal;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.CheckResult;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.ravenfeld.easyvideoplayer.EasyVideoCallback;
import com.ravenfeld.easyvideoplayer.EasyVideoPlayerConfig;
import com.ravenfeld.easyvideoplayer.IUserMethods;
import com.ravenfeld.easyvideoplayer.R;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayDeque;


public class PlayerView extends FrameLayout implements IUserMethods, TextureView.SurfaceTextureListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "PlayerView";

    private enum Work {
        SEEK, START, PAUSE
    }

    @IntDef({LEFT_ACTION_NONE, LEFT_ACTION_RESTART, LEFT_ACTION_RETRY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LeftAction {
    }

    @IntDef({RIGHT_ACTION_NONE, RIGHT_ACTION_SUBMIT, RIGHT_ACTION_CUSTOM_LABEL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RightAction {
    }

    public static final int LEFT_ACTION_NONE = 0;
    public static final int LEFT_ACTION_RESTART = 1;
    public static final int LEFT_ACTION_RETRY = 2;
    public static final int RIGHT_ACTION_NONE = 3;
    public static final int RIGHT_ACTION_SUBMIT = 4;
    public static final int RIGHT_ACTION_CUSTOM_LABEL = 5;
    private static final int UPDATE_INTERVAL = 100;

    public PlayerView(Context context) {
        this(context, null);
    }

    public PlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private TextureView mTextureView;
    private Surface mSurface;

    private View mControlsFrame;
    private View mProgressFrame;
    private View mTextErrorFrame;
    private TextView mTextError;
    private View mClickFrame;

    private SeekBar mSeeker;
    private TextView mLabelPosition;
    private TextView mLabelDuration;
    private ImageButton mBtnRestart;
    private TextView mBtnRetry;
    private ImageButton mBtnPlayPause;
    private ImageButton mBtnPlayPauseControl;
    private ImageButton mBtnPlayPauseVideo;
    private ImageButton mBtnFullScreen;
    private TextView mBtnSubmit;
    private TextView mLabelCustom;
    private TextView mLabelBottom;

    private MediaPlayer mPlayer;
    private boolean mSurfaceAvailable;
    private boolean mIsPrepared;
    private boolean mIsBuffered;
    private boolean mIsOnPreparing;
    private boolean mWasPlaying;
    private ArrayDeque<Work> workedList = new ArrayDeque<>();

    private Handler mHandler;

    private Uri mSource;
    private EasyVideoCallback mCallback;
    private InternalCallback mInternalCallback;
    @LeftAction
    private int mLeftAction = LEFT_ACTION_NONE;
    @RightAction
    private int mRightAction = RIGHT_ACTION_NONE;
    private CharSequence mRetryText;
    private CharSequence mSubmitText;
    private Drawable mRestartDrawable;
    private Drawable mPlayDrawable;
    private Drawable mPauseDrawable;
    private Drawable mFullScreenDrawable;
    private Drawable mFullScreenExitDrawable;
    private CharSequence mCustomLabelText;
    private CharSequence mBottomLabelText;
    private boolean mHideControlsOnPlay = true;
    private boolean mAutoPlay = false;
    private int mInitialPosition = -1;
    private boolean mControlsDisabled = false;
    private boolean mEnabledSeekBar = true;
    private int mThemeColor = 0;
    private float mVideoSizeLoading = 16f / 10f;

    private boolean isVideoOnly = false;
    private boolean isError = false;
    private String errorMessage = "";

    // Runnable used to run code on an interval to update counters and seeker
    private final Runnable mUpdateCounters = new Runnable() {
        @Override
        public void run() {
            if (mHandler == null || !mIsPrepared) {
                return;
            }
            updateUi();

            if (mHandler != null) {
                mHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        }
    };


    private void init(Context context) {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, hashCode() + " init:");
        }
        setBackgroundColor(Color.BLACK);

        if (mThemeColor == 0) {
            mThemeColor = Util.resolveColor(context, R.attr.colorPrimary);
        }

        if (mRetryText == null) {
            mRetryText = context.getResources().getText(R.string.evp_retry);
        }
        if (mSubmitText == null) {
            mSubmitText = context.getResources().getText(R.string.evp_submit);
        }

        if (mRestartDrawable == null) {
            mRestartDrawable = AppCompatResources.getDrawable(context, R.drawable.evp_action_restart);
        }
        if (mPlayDrawable == null) {
            mPlayDrawable = AppCompatResources.getDrawable(context, R.drawable.evp_action_play);
        }
        if (mPauseDrawable == null) {
            mPauseDrawable = AppCompatResources.getDrawable(context, R.drawable.evp_action_pause);
        }
        if (mFullScreenDrawable == null) {
            mFullScreenDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_fullscreen);
        }
        if (mFullScreenExitDrawable == null) {
            mFullScreenExitDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_fullscreen_exit);
        }

        onInflate();
        initPlayer();
        prepare();
    }

    @Override
    public void setSource(@NonNull Uri source) {
        mSource = source;
        if (mPlayer != null) {
            prepare();
        }
    }

    @Override
    public void setCallback(@NonNull EasyVideoCallback callback) {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, hashCode() + " setCallback: " + callback);
        }
        mCallback = callback;
    }

    public void setFullScreenCallback(@NonNull InternalCallback callback) {
        mInternalCallback = callback;
    }

    @Override
    public void setLeftAction(@LeftAction int action) {
        if (action < LEFT_ACTION_NONE || action > LEFT_ACTION_RETRY) {
            throw new IllegalArgumentException("Invalid left action specified.");
        }
        mLeftAction = action;
        invalidateActions();
    }

    @Override
    public void setRightAction(@RightAction int action) {
        if (action < RIGHT_ACTION_NONE || action > RIGHT_ACTION_CUSTOM_LABEL) {
            throw new IllegalArgumentException("Invalid right action specified.");
        }
        mRightAction = action;
        invalidateActions();
    }

    @Override
    public void setCustomLabelText(@Nullable CharSequence text) {
        mCustomLabelText = text;
        mLabelCustom.setText(text);
        setRightAction(RIGHT_ACTION_CUSTOM_LABEL);
    }

    @Override
    public void setCustomLabelTextRes(@StringRes int textRes) {
        setCustomLabelText(getResources().getText(textRes));
    }

    @Override
    public void setBottomLabelText(@Nullable CharSequence text) {
        mBottomLabelText = text;
        mLabelBottom.setText(text);
        if (text == null || text.toString().trim().length() == 0) {
            mLabelBottom.setVisibility(View.GONE);
        } else {
            mLabelBottom.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setBottomLabelTextRes(@StringRes int textRes) {
        setBottomLabelText(getResources().getText(textRes));
    }

    @Override
    public void setRetryText(@Nullable CharSequence text) {
        mRetryText = text;
        mBtnRetry.setText(text);
    }

    @Override
    public void setRetryTextRes(@StringRes int res) {
        setRetryText(getResources().getText(res));
    }

    @Override
    public void setSubmitText(@Nullable CharSequence text) {
        mSubmitText = text;
        mBtnSubmit.setText(text);
    }

    @Override
    public void setSubmitTextRes(@StringRes int res) {
        setSubmitText(getResources().getText(res));
    }

    @Override
    public void setRestartDrawable(@NonNull Drawable drawable) {
        mRestartDrawable = drawable;
        mBtnRestart.setImageDrawable(drawable);
    }

    @Override
    public void setRestartDrawableRes(@DrawableRes int res) {
        setRestartDrawable(AppCompatResources.getDrawable(getContext(), res));
    }

    @Override
    public void setPlayDrawable(@NonNull Drawable drawable) {
        mPlayDrawable = drawable;
        if (!isPlaying()) {
            mBtnPlayPause.setImageDrawable(drawable);
        }
    }

    @Override
    public void setPlayDrawableRes(@DrawableRes int res) {
        setPlayDrawable(AppCompatResources.getDrawable(getContext(), res));
    }

    @Override
    public void setPauseDrawable(@NonNull Drawable drawable) {
        mPauseDrawable = drawable;
        if (isPlaying()) {
            mBtnPlayPause.setImageDrawable(drawable);
        }
    }

    @Override
    public void setPauseDrawableRes(@DrawableRes int res) {
        setPauseDrawable(AppCompatResources.getDrawable(getContext(), res));
    }

    @Override
    public void setFullScreenDrawable(@NonNull Drawable drawable) {
        mFullScreenDrawable = drawable;
        if (!isVideoOnly) {
            mBtnFullScreen.setImageDrawable(drawable);
        }
    }

    @Override
    public void setFullScreenDrawableRes(@DrawableRes int res) {
        setPlayDrawable(AppCompatResources.getDrawable(getContext(), res));
    }

    @Override
    public void setFullScreenExitDrawable(@NonNull Drawable drawable) {
        mFullScreenExitDrawable = drawable;
        if (isVideoOnly) {
            mBtnFullScreen.setImageDrawable(drawable);
        }
    }

    @Override
    public void setFullScreenExitDrawableRes(@DrawableRes int res) {
        setPauseDrawable(AppCompatResources.getDrawable(getContext(), res));
    }

    @Override
    public void setThemeColor(@ColorInt int color) {
        mThemeColor = color;
        invalidateThemeColors();
    }

    @Override
    public void setThemeColorRes(@ColorRes int colorRes) {
        setThemeColor(ContextCompat.getColor(getContext(), colorRes));
    }

    @Override
    public void setHideControlsOnPlay(boolean hide) {
        mHideControlsOnPlay = hide;
    }

    @Override
    public void setAutoPlay(boolean autoPlay) {
        mAutoPlay = autoPlay;
    }

    @Override
    public boolean isAutoPlay() {
        return mAutoPlay;
    }

    @Override
    public void setInitialPosition(@IntRange(from = 0, to = Integer.MAX_VALUE) int pos) {
        mInitialPosition = pos;
    }

    private synchronized void prepare() {
        if (!mSurfaceAvailable || mSource == null || mPlayer == null || mIsPrepared) {
            return;
        }
        try {
            mPlayer.reset();
            boolean continuePrepa = true;
            if (mCallback != null) {
                continuePrepa = mCallback.onPreparing(this);
            }
            if (continuePrepa) {
                mIsOnPreparing = true;
                mPlayer.setSurface(mSurface);
                if (mSource.getScheme() != null &&
                        (mSource.getScheme().equals("http") || mSource.getScheme().equals("https"))) {
                    if (EasyVideoPlayerConfig.isDebug()) {
                        Log.d(TAG, hashCode() + " Loading web URI: " + mSource.toString());
                    }
                    mIsBuffered = false;
                    mPlayer.setDataSource(mSource.toString());
                } else if (mSource.getScheme() != null && (mSource.getScheme().equals("file") && mSource.getPath().contains("/android_asset/"))) {
                    if (EasyVideoPlayerConfig.isDebug()) {
                        Log.d(TAG, hashCode() + " Loading assets URI: " + mSource.toString());
                    }
                    mIsBuffered = true;
                    AssetFileDescriptor afd;
                    afd = getContext().getAssets().openFd(mSource.toString().replace("file:///android_asset/", ""));
                    mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    afd.close();
                } else if (mSource.getScheme() != null && mSource.getScheme().equals("asset")) {
                    if (EasyVideoPlayerConfig.isDebug()) {
                        Log.d(TAG, hashCode() + " Loading assets URI: " + mSource.toString());
                    }
                    mIsBuffered = true;
                    AssetFileDescriptor afd;
                    afd = getContext().getAssets().openFd(mSource.toString().replace("asset://", ""));
                    mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    afd.close();
                } else {
                    if (EasyVideoPlayerConfig.isDebug()) {
                        Log.d(TAG, hashCode() + " Loading local URI: " + mSource.toString());
                    }
                    mIsBuffered = true;
                    mPlayer.setDataSource(getContext(), mSource);
                }
                mPlayer.prepareAsync();
            }
        } catch (IOException | IllegalStateException e) {
            throwError(e);
        }
    }

    private void updateUi() {
        if (mPlayer == null || mSeeker == null || mLabelPosition == null || mLabelDuration == null || isError) {
            return;
        }

        int pos = getCurrentPosition();
        int dur = getDuration();
        if (pos > dur) {
            pos = dur;
        }
        mLabelPosition.setText(Util.getDurationString(pos, false));
        mLabelDuration.setText(Util.getDurationString(dur - pos, true));
        mSeeker.setProgress(pos);
        mSeeker.setMax(dur);
        if (mCallback != null) {
            mCallback.onVideoProgressUpdate(this, pos, dur);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        if (mSeeker == null) {
            return;
        }
        mSeeker.setEnabled(mEnabledSeekBar);
        mBtnPlayPause.setEnabled(enabled);
        mBtnFullScreen.setEnabled(enabled);
        mBtnSubmit.setEnabled(enabled);
        mBtnRestart.setEnabled(enabled);
        mBtnRetry.setEnabled(enabled);

        float disabledAlpha = .4f;
        mBtnPlayPause.setAlpha(enabled ? 1f : disabledAlpha);
        mBtnFullScreen.setAlpha(enabled ? 1f : disabledAlpha);
        mBtnSubmit.setAlpha(enabled ? 1f : disabledAlpha);
        mBtnRestart.setAlpha(enabled ? 1f : disabledAlpha);

        mClickFrame.setEnabled(enabled);
    }

    @Override
    public void showControls() {
        if (mControlsDisabled || isControlsShown() || mSeeker == null) {
            return;
        }

        mControlsFrame.animate().cancel();
        mControlsFrame.setAlpha(0f);
        mControlsFrame.setVisibility(View.VISIBLE);
        mControlsFrame.animate().alpha(1f)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(null).start();
        if (mLeftAction == LEFT_ACTION_NONE && mRightAction == RIGHT_ACTION_NONE) {
            mBtnPlayPause.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideControls() {
        if (mControlsDisabled || !isControlsShown() || mSeeker == null) {
            return;
        }
        mControlsFrame.animate().cancel();
        mControlsFrame.setAlpha(1f);
        mControlsFrame.setVisibility(View.VISIBLE);
        mControlsFrame.animate().alpha(0f)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (mControlsFrame != null) {
                            mControlsFrame.setVisibility(View.INVISIBLE);
                        }
                    }
                }).start();
        if (mLeftAction == LEFT_ACTION_NONE && mRightAction == RIGHT_ACTION_NONE) {
            mBtnPlayPause.setVisibility(View.INVISIBLE);
        }
    }

    @CheckResult
    @Override
    public boolean isControlsShown() {
        return !mControlsDisabled && mControlsFrame != null && mControlsFrame.getAlpha() > .5f;
    }

    @Override
    public void toggleControls() {
        if (mControlsDisabled) {
            return;
        }
        if (isControlsShown()) {
            hideControls();
        } else {
            showControls();
        }
    }

    @Override
    public void enableControls(boolean andShow) {
        mControlsDisabled = false;
        if (andShow) {
            showControls();
        }
        GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                playSoundEffect(android.view.SoundEffectConstants.CLICK);
                toggleControls();
                return true;

            }

        };
        final GestureDetector gestureDetector = new GestureDetector(getContext(), gestureListener);
        mClickFrame.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent);
            }
        });

        mClickFrame.setClickable(true);
    }

    @Override
    public void disableControls() {
        mControlsDisabled = true;
        mControlsFrame.setVisibility(View.GONE);
        mClickFrame.setOnTouchListener(null);
        mClickFrame.setClickable(false);
    }

    @Override
    public void setEnabledSeekBar(boolean enabled) {
        mEnabledSeekBar = enabled;
        mSeeker.setEnabled(mEnabledSeekBar);
    }


    @CheckResult
    public boolean isBuffered() {
        return mPlayer != null && mIsPrepared && mIsBuffered;
    }

    @CheckResult
    @Override
    public boolean isPrepared() {
        return mPlayer != null && mIsPrepared;
    }

    @CheckResult
    @Override
    public boolean isOnPreparing() {
        return mPlayer != null && mIsOnPreparing;
    }

    @CheckResult
    @Override
    public boolean isPlaying() {
        return mPlayer != null && mPlayer.isPlaying();
    }

    public boolean isWasPlaying() {
        return mWasPlaying;
    }

    @CheckResult
    @Override
    public int getCurrentPosition() {
        if (mPlayer == null || !isPrepared()) {
            return -1;
        }
        if (isError) {
            return mInitialPosition;
        } else {
            return mPlayer.getCurrentPosition();
        }
    }

    @CheckResult
    @Override
    public int getDuration() {
        if (mPlayer == null || !isPrepared()) {
            return -1;
        }
        return mPlayer.getDuration();
    }

    @Override
    public void start() {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, hashCode() + " start: ");
        }
        if (mPlayer == null) {
            return;
        }
        if (workedList.isEmpty()) {
            if (isBuffered()) {
                isError = false;
                mTextErrorFrame.setVisibility(INVISIBLE);
                mPlayer.start();

                mWasPlaying = true;
                if (mHandler == null) {
                    mHandler = new Handler();
                }
                mHandler.post(mUpdateCounters);
                mBtnPlayPause.setImageDrawable(mPauseDrawable);
                if (mCallback != null) {
                    mCallback.onStarted(this);
                }
            }
        } else {
            workedList.add(Work.START);
        }
    }

    @Override
    public void restart() {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, hashCode() + " restart: ");
        }
        if (mPlayer == null) {
            return;
        }
        if (isBuffered()) {
            seekTo(0);
            if (!isPlaying()) {
                start();
            }
        }
    }

    @Override
    public void seekTo(@IntRange(from = 0, to = Integer.MAX_VALUE) int pos) {
        if (mPlayer == null) {
            return;
        }
        mPlayer.seekTo(pos);
        mInitialPosition = pos;
        updateUi();
    }

    @Override
    public void setVolume(@FloatRange(from = 0f, to = 1f) float leftVolume, @FloatRange(from = 0f, to = 1f) float rightVolume) {
        if (mPlayer == null || !mIsPrepared) {
            throw new IllegalStateException("You cannot use setVolume(float, float) until the player is prepared.");
        }
        mPlayer.setVolume(leftVolume, rightVolume);
    }

    public void onResume() {
        setKeepScreenOn(true);
        if (mWasPlaying) {
            start();
        } else {
            getFrame();
        }
    }

    public void onPause() {
        setKeepScreenOn(false);
        mWasPlaying = isPlaying();
        mInitialPosition = mInitialPosition > getCurrentPosition() ? mInitialPosition : getCurrentPosition();
        pause();
    }

    @Override
    public void pause() {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, hashCode() + " pause: ");
        }
        if (mPlayer == null || !isPlaying() || !isPrepared()) {
            return;
        }
        if (workedList.isEmpty()) {
            mPlayer.pause();
            if (mCallback != null) {
                mCallback.onPaused(this);
            }
            if (mHandler == null) {
                return;
            }
            mHandler.removeCallbacks(mUpdateCounters);
            mBtnPlayPause.setImageDrawable(mPlayDrawable);
        } else {
            workedList.add(Work.PAUSE);
        }
    }

    @Override
    public void stop() {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, hashCode() + " stop: ");
        }
        if (mPlayer == null) {
            return;
        }
        try {
            mPlayer.stop();
        } catch (Throwable ignored) {
        }
        if (mHandler == null) {
            return;
        }
        mHandler.removeCallbacks(mUpdateCounters);
        mBtnPlayPause.setImageDrawable(mPauseDrawable);
    }

    @Override
    public void reset() {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, hashCode() + " reset: ");
        }

        mIsPrepared = false;
        mIsBuffered = false;
        isError = false;
        mSource = null;
        if (mPlayer != null) {
            mPlayer.reset();
        }

        mProgressFrame.setVisibility(VISIBLE);
        mTextErrorFrame.setVisibility(INVISIBLE);
        if (mHandler != null) {
            mHandler.removeCallbacks(mUpdateCounters);
            mHandler = null;
        }
        updateUi();
        showControls();
        invalidateActions();
    }

    private void resetPlayer() {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, hashCode() + " resetPlayer: ");
        }
        if (mPlayer == null) {
            return;
        }

        mIsPrepared = false;
        isError = false;
        mPlayer.reset();
        mProgressFrame.setVisibility(VISIBLE);
        mTextErrorFrame.setVisibility(INVISIBLE);
    }

    public void release(boolean force) {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, hashCode() + " release: ");
        }
        reset();

        if (force) {
            if (mPlayer != null) {
                try {
                    mPlayer.release();
                } catch (Throwable ignored) {
                    if (EasyVideoPlayerConfig.isDebug()) {
                        Log.d(TAG, hashCode() + " release: error", ignored);
                    }
                }
                mPlayer.setOnPreparedListener(null);
                mPlayer.setOnBufferingUpdateListener(null);
                mPlayer.setOnCompletionListener(null);
                mPlayer.setOnVideoSizeChangedListener(null);
                mPlayer.setOnErrorListener(null);
                mPlayer = null;

            }
            if (mSurface != null) {
                mSurface.release();
            }
            if (mTextureView != null) {
                mTextureView.setSurfaceTextureListener(null);
            }
        }
    }

    @Override
    public void release() {
        release(!isVideoOnly);
    }

    @Override
    public void setVideoSizeLoading(float videoSizeLoading) {
        mVideoSizeLoading = videoSizeLoading;
    }

    public void setVideoOnly(boolean videoOnly) {
        isVideoOnly = videoOnly;
        if (isVideoOnly) {
            mBtnFullScreen.setImageDrawable(mFullScreenExitDrawable);
        } else {
            mBtnFullScreen.setImageDrawable(mFullScreenDrawable);
        }
    }

    @Override
    public boolean isVideoOnly() {
        return isVideoOnly;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, hashCode() + " onSurfaceTextureAvailable: " + width + " " + height);
        }
        mSurfaceAvailable = true;
        mSurface = new Surface(surfaceTexture);
        if (mIsPrepared) {
            mPlayer.setSurface(mSurface);
        } else {
            prepare();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, hashCode() + " onSurfaceTextureSizeChanged: " + width + " " + height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, hashCode() + " onSurfaceTextureDestroyed: ");
        }

        mSurfaceAvailable = false;
        mSurface.release();
        mSurface = null;
        if (mPlayer != null) {
            mPlayer.setSurface(mSurface);
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, hashCode() + " onPrepared: ");
        }
        mIsOnPreparing = false;
        mIsPrepared = true;
        mLabelPosition.setText(Util.getDurationString(0, false));
        mLabelDuration.setText(Util.getDurationString(mediaPlayer.getDuration(), false));
        mSeeker.setProgress(0);
        mSeeker.setMax(mediaPlayer.getDuration());
        setControlsEnabled(true);

        Log.e("TEST", "onPrepared" + isBuffered());
        if (isBuffered()) {
            mIsBuffered = false;
            onBufferingUpdate(mPlayer, 100);
        }
    }

    private void getFrame() {
        Log.e("TEST", "getFrame" + isPrepared());

        if (isPrepared()) {
            mPlayer.start();
            if (mInitialPosition >= 0) {
                seekTo(mInitialPosition);
            }
            mPlayer.pause();
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        workedList.pop();
        if (!workedList.isEmpty()) {
            switch (workedList.pop()) {
                case START:
                    start();
                    break;
                case PAUSE:
                    pause();
                    break;
            }
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {
        Log.e("TEST", "onBufferingUpdate" + percent);

        if (!mIsBuffered) {
            mIsBuffered = true;
            if (mAutoPlay) {
                Log.e("TEST", "mAutoPlay" + percent);

                if (!mControlsDisabled && mHideControlsOnPlay) {
                    hideControls();
                }
                start();
                if (mInitialPosition > 0) {
                    seekTo(mInitialPosition);
                }
            } else {
                // Hack to show first frame, is there another way?
                Log.e("TEST", "getFrame" + percent);

                getFrame();
            }
            if (EasyVideoPlayerConfig.isDebug()) {
                Log.d(TAG, hashCode() + " mCallback: " + (mCallback != null));
            }
            if (mCallback != null) {
                mCallback.onPrepared(this);
            }
        }
        if (mSeeker != null) {
            if (percent == 100) {
                mSeeker.setSecondaryProgress(0);
                displayIconPlayPause();
            } else {
                int percentSeeker = (int) (mSeeker.getMax() * (percent / 100f));
                mSeeker.setSecondaryProgress(percentSeeker);
                if (percentSeeker < mediaPlayer.getCurrentPosition()) {
                    mProgressFrame.setVisibility(VISIBLE);
                    if (mLeftAction == LEFT_ACTION_NONE && mRightAction == RIGHT_ACTION_NONE) {
                        mBtnPlayPause.setVisibility(View.INVISIBLE);
                    }
                } else {
                    displayIconPlayPause();
                }
            }
        }
        if (mCallback != null) {
            mCallback.onBuffering(this, percent);
        }
    }

    private void displayIconPlayPause() {
        if (mProgressFrame != null && mProgressFrame.getVisibility() == VISIBLE) {
            mProgressFrame.setVisibility(INVISIBLE);
            if (mLeftAction == LEFT_ACTION_NONE && mRightAction == RIGHT_ACTION_NONE) {
                if (isControlsShown()) {
                    mBtnPlayPause.setVisibility(View.VISIBLE);
                } else {
                    mBtnPlayPause.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private void displayMessageError() {
        mProgressFrame.setVisibility(INVISIBLE);
        mTextErrorFrame.setVisibility(VISIBLE);
        mTextError.setText(errorMessage);
        if (mLeftAction == LEFT_ACTION_NONE && mRightAction == RIGHT_ACTION_NONE) {
            mBtnPlayPause.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, hashCode() + " onCompletion: ");
        }
        if (mHandler != null) {
            mHandler.removeCallbacks(mUpdateCounters);
        }
        if (isError) {
            displayMessageError();
            mPlayer.reset();
        } else {
            mBtnPlayPause.setImageDrawable(mPlayDrawable);
            mSeeker.setProgress(0);
            showControls();
        }
        updateUi();
        if (mCallback != null) {
            mCallback.onCompletion(this);
        }
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, hashCode() + " onVideoSizeChanged: " + width + " " + height);
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        if (what == -38) {
            // Error code -38 happens on some Samsung devices
            // Just ignore it
            return false;
        }
        String errorMsg = "Preparation/playback error (" + what + " " + extra + "): ";
        errorMessage = getContext().getString(R.string.evp_error);
        switch (what) {
            default:
                errorMsg += "Unknown error";
                if (extra == -1005 || extra == -1004) {
                    errorMessage = getContext().getString(R.string.evp_network_error);
                    errorMsg += " network error";
                }
                break;
            case MediaPlayer.MEDIA_ERROR_IO:
                errorMsg += "I/O error";
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                errorMsg += "Malformed";
                break;
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                errorMsg += "Not valid for progressive playback";
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                errorMsg += "Server died";
                break;
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                errorMsg += "Timed out";
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                errorMsg += "Unsupported";
                break;
        }
        mInitialPosition = mInitialPosition > getCurrentPosition() ? mInitialPosition : getCurrentPosition();
        isError = true;
        throwError(new Exception(errorMsg));
        return false;
    }

    private void onInflate() {
        setKeepScreenOn(true);
        // Instantiate and add TextureView for rendering
        FrameLayout.LayoutParams textureLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mTextureView = new TextureView(getContext());
        addView(mTextureView, textureLp);

        LayoutInflater li = LayoutInflater.from(getContext());

        // Inflate and add progress
        mProgressFrame = li.inflate(R.layout.evp_include_progress, this, false);
        addView(mProgressFrame);

        // Instantiate and add click frame (used to toggle controls)
        mClickFrame = new FrameLayout(getContext());
        ((FrameLayout) mClickFrame).setForeground(Util.resolveDrawable(getContext(), R.attr.selectableItemBackground));
        addView(mClickFrame, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Inflate controls
        mControlsFrame = li.inflate(R.layout.evp_include_controls, this, false);
        FrameLayout.LayoutParams controlsLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        controlsLp.gravity = Gravity.BOTTOM;
        addView(mControlsFrame, controlsLp);
        if (mControlsDisabled) {
            mClickFrame.setOnClickListener(null);
            mControlsFrame.setVisibility(View.GONE);
        } else {
            enableControls(true);
        }

        mTextErrorFrame = li.inflate(R.layout.evp_include_text_error, this, false);
        mTextErrorFrame.setOnClickListener(this);
        addView(mTextErrorFrame);
        mTextError = (TextView) mTextErrorFrame.findViewById(R.id.title_error);

        // Retrieve controls
        mSeeker = (SeekBar) mControlsFrame.findViewById(R.id.seeker);
        mSeeker.setOnSeekBarChangeListener(this);

        mLabelPosition = (TextView) mControlsFrame.findViewById(R.id.position);
        mLabelPosition.setText(Util.getDurationString(0, false));

        mLabelDuration = (TextView) mControlsFrame.findViewById(R.id.duration);
        mLabelDuration.setText(Util.getDurationString(0, true));

        invalidateThemeColors();

        mBtnRestart = (ImageButton) mControlsFrame.findViewById(R.id.btnRestart);
        mBtnRestart.setOnClickListener(this);
        mBtnRestart.setImageDrawable(mRestartDrawable);

        mBtnRetry = (TextView) mControlsFrame.findViewById(R.id.btnRetry);
        mBtnRetry.setOnClickListener(this);
        mBtnRetry.setText(mRetryText);

        mBtnPlayPauseVideo = (ImageButton) li.inflate(R.layout.evp_include_btn_play_pause, this, false);
        addView(mBtnPlayPauseVideo);
        mBtnPlayPauseVideo.setOnClickListener(this);
        mBtnPlayPauseVideo.setImageDrawable(mPlayDrawable);
        mBtnPlayPauseControl = (ImageButton) mControlsFrame.findViewById(R.id.btnPlayPauseControl);
        mBtnPlayPauseControl.setOnClickListener(this);
        mBtnPlayPauseControl.setImageDrawable(mPlayDrawable);

        if (mLeftAction == LEFT_ACTION_NONE && mRightAction == RIGHT_ACTION_NONE) {
            mBtnPlayPause = mBtnPlayPauseVideo;
        } else {
            mBtnPlayPause = mBtnPlayPauseControl;
        }

        mBtnSubmit = (TextView) mControlsFrame.findViewById(R.id.btnSubmit);
        mBtnSubmit.setOnClickListener(this);
        mBtnSubmit.setText(mSubmitText);

        mLabelCustom = (TextView) mControlsFrame.findViewById(R.id.labelCustom);
        mLabelCustom.setText(mCustomLabelText);

        mLabelBottom = (TextView) mControlsFrame.findViewById(R.id.labelBottom);
        setBottomLabelText(mBottomLabelText);

        mBtnFullScreen = (ImageButton) mControlsFrame.findViewById(R.id.btnFullScreen);
        mBtnFullScreen.setOnClickListener(this);
        setVideoOnly(isVideoOnly);

        setControlsEnabled(false);
        invalidateActions();
    }

    public void initPlayer() {
        if (!isVideoOnly) {
            mHandler = new Handler();
            mPlayer = new MediaPlayer();
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnBufferingUpdateListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnVideoSizeChangedListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mTextureView.setSurfaceTextureListener(this);

        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnPlayPause || view.getId() == R.id.btnPlayPauseControl) {
            if (isPlaying()) {
                pause();
            } else {
                if (mHideControlsOnPlay && !mControlsDisabled) {
                    hideControls();
                }
                start();
            }
        } else if (view.getId() == R.id.btnRestart) {
            restart();
        } else if (view.getId() == R.id.btnRetry) {
            if (mCallback != null) {
                mCallback.onRetry(this, mSource);
            }
        } else if (view.getId() == R.id.btnSubmit) {
            if (mCallback != null) {
                mCallback.onSubmit(this, mSource);
            }
        } else if (view.getId() == R.id.btnFullScreen) {
            if (!isVideoOnly) {
                mInitialPosition = mInitialPosition > getCurrentPosition() ? mInitialPosition : getCurrentPosition();
                mWasPlaying = isPlaying();
                if (mCallback != null) {
                    mCallback.onFullScreen(this);
                }
                if (mInternalCallback != null) {
                    mInternalCallback.onFullScreen(this);
                }
            } else {
                mWasPlaying = isPlaying();
                if (mCallback != null) {
                    mCallback.onFullScreenExit(this);
                }
                if (mInternalCallback != null) {
                    mInternalCallback.onFullScreenExit(this);
                }
            }
        } else if (view.getId() == R.id.error) {
            resetPlayer();
            prepare();
        }


    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
        if (fromUser) {
            seekTo(value);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mWasPlaying = isPlaying();
        if (mWasPlaying && isBuffered()) {
            mPlayer.pause();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mWasPlaying && isBuffered()) {
            mPlayer.start();
        }
    }

    public void detach() {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.d(TAG, hashCode() + " detach: ");
        }
        release(true);
        mTextErrorFrame.setOnClickListener(null);
        mSeeker.setOnSeekBarChangeListener(null);
        mBtnPlayPauseVideo.setOnClickListener(null);
        mBtnPlayPauseControl.setOnClickListener(null);
        mBtnRestart.setOnClickListener(null);
        mBtnSubmit.setOnClickListener(null);
        mClickFrame.setOnTouchListener(null);
        mBtnFullScreen.setOnClickListener(null);
        mCallback = null;
        mInternalCallback = null;
        removeAllViews();
    }

    private void invalidateActions() {
        switch (mLeftAction) {
            case LEFT_ACTION_NONE:
                mBtnRetry.setVisibility(View.GONE);
                mBtnRestart.setVisibility(View.GONE);
                break;
            case LEFT_ACTION_RESTART:
                mBtnRetry.setVisibility(View.GONE);
                mBtnRestart.setVisibility(View.VISIBLE);
                break;
            case LEFT_ACTION_RETRY:
                mBtnRetry.setVisibility(View.VISIBLE);
                mBtnRestart.setVisibility(View.GONE);
                break;
        }
        switch (mRightAction) {
            case RIGHT_ACTION_NONE:
                mBtnSubmit.setVisibility(View.GONE);
                mLabelCustom.setVisibility(View.GONE);
                break;
            case RIGHT_ACTION_SUBMIT:
                mBtnSubmit.setVisibility(View.VISIBLE);
                mLabelCustom.setVisibility(View.GONE);
                break;
            case RIGHT_ACTION_CUSTOM_LABEL:
                mBtnSubmit.setVisibility(View.GONE);
                mLabelCustom.setVisibility(View.VISIBLE);
                break;
        }
        mBtnPlayPause.setVisibility(GONE);
        if (mLeftAction == LEFT_ACTION_NONE && mRightAction == RIGHT_ACTION_NONE) {
            mBtnPlayPause = mBtnPlayPauseVideo;
            if (mProgressFrame != null && mProgressFrame.getVisibility() != VISIBLE) {
                mBtnPlayPause.setVisibility(VISIBLE);
            }

        } else {
            mBtnPlayPause = mBtnPlayPauseControl;
            mBtnPlayPause.setVisibility(VISIBLE);
        }

    }

    private void adjustAspectRatio(int viewWidth, int viewHeight, int videoWidth, int videoHeight, int widthMeasureSpec, int heightMeasureSpec) {
        double aspectRatio;
        if (videoWidth == 0 || videoHeight == 0) {
            aspectRatio = 1f / mVideoSizeLoading;
        } else {
            aspectRatio = (double) videoHeight / videoWidth;
        }

        int newWidth, newHeight;

        if (viewHeight > (int) (viewWidth * aspectRatio) || viewHeight == 0) {
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }

        int xoff = 0;
        int yoff = 0;
        ViewGroup.LayoutParams layoutParamsTexture = mTextureView.getLayoutParams();
        if (widthMeasureSpec != 0 && !isVideoOnly && (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST || MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED)) {
            ViewGroup.LayoutParams layoutParamsControls = mControlsFrame.getLayoutParams();
            layoutParamsControls.width = newWidth;
            mControlsFrame.setLayoutParams(layoutParamsControls);

            layoutParamsTexture.width = newWidth;
            newWidth = viewWidth;
        } else {
            layoutParamsTexture.width = ViewGroup.LayoutParams.MATCH_PARENT;
            xoff = (viewWidth - newWidth) / 2;
        }


        if (heightMeasureSpec != 0 && !isVideoOnly && (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST || MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) || heightMeasureSpec == 0) {
            layoutParamsTexture.height = newHeight;
            newHeight = viewHeight;
        } else {

            layoutParamsTexture.height = ViewGroup.LayoutParams.MATCH_PARENT;
            yoff = (viewHeight - newHeight) / 2;
        }

        if (viewHeight == 0) {
            viewHeight = 1;
            newHeight = 1;
        }

        Matrix txform = new Matrix();
        mTextureView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        txform.postTranslate(xoff, yoff);
        mTextureView.setTransform(txform);
        mTextureView.setLayoutParams(layoutParamsTexture);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int videoWidth = 0;
        int videoHeight = 0;
        if (isBuffered()) {
            videoWidth = mPlayer.getVideoWidth();
            videoHeight = mPlayer.getVideoHeight();
        }
        adjustAspectRatio(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec), videoWidth, videoHeight, widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void throwError(Exception e) {
        if (EasyVideoPlayerConfig.isDebug()) {
            Log.e(TAG, hashCode() + " throwError ", e);
        }
        if (mCallback != null) {
            mCallback.onError(this, e);
        } else {
            throw new RuntimeException(e);
        }
    }

    private static void setTint(@NonNull SeekBar seekBar, @ColorInt int color) {
        ColorStateList s1 = ColorStateList.valueOf(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            seekBar.setThumbTintList(s1);
            seekBar.setProgressTintList(s1);
            seekBar.setSecondaryProgressTintList(s1);
        } else {
            Drawable progressDrawable = DrawableCompat.wrap(seekBar.getProgressDrawable());
            seekBar.setProgressDrawable(progressDrawable);
            DrawableCompat.setTintList(progressDrawable, s1);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                Drawable thumbDrawable = DrawableCompat.wrap(seekBar.getThumb());
                DrawableCompat.setTintList(thumbDrawable, s1);
                seekBar.setThumb(thumbDrawable);
            }
        }
    }

    private void invalidateThemeColors() {
        int labelColor = Util.isColorDark(mThemeColor) ? Color.WHITE : Color.BLACK;
        mControlsFrame.setBackgroundColor(Util.adjustAlpha(mThemeColor, 0.85f));
        mLabelDuration.setTextColor(labelColor);
        mLabelPosition.setTextColor(labelColor);
        setTint(mSeeker, labelColor);

    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);

        if (mInternalCallback != null) {
            mInternalCallback.onRestoreInstance(this);
        }
    }


    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == GONE) {
            pause();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Rect rect = new Rect();
        getGlobalVisibleRect(rect);
        if (rect.bottom < 0) {
            pause();
        }
    }
}
