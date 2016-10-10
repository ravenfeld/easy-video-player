package com.afollestad.easyvideoplayer;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class EasyVideo extends FrameLayout implements EasyVideoCallback {
    private static final String TAG = "EasyVideo";
    private EasyVideoCallback callback;
    private boolean init = false;


    private Uri mSource;
    @EasyVideoPlayer.LeftAction
    private int mLeftAction;
    @EasyVideoPlayer.RightAction
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
    private int mInitialPosition;
    private boolean mControlsDisabled;
    private int mThemeColor;
    private boolean mAutoFullscreen;
    private float mVideoSizeLoading;

    private AttributeSet attributeSet;

    public EasyVideo(Context context) {
        this(context, null);
    }

    public EasyVideo(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EasyVideo(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        //faire un objet pour les attributs, et un style par défaut
        attributeSet = attrs;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.EasyVideoPlayer,
                    0, 0);
            try {
                String source = a.getString(R.styleable.EasyVideoPlayer_evp_source);
                if (source != null && !source.trim().isEmpty())
                    mSource = Uri.parse(source);

                //noinspection WrongConstant
                mLeftAction = a.getInteger(R.styleable.EasyVideoPlayer_evp_leftAction, EasyVideoPlayer.LEFT_ACTION_NONE);
                //noinspection WrongConstant
                mRightAction = a.getInteger(R.styleable.EasyVideoPlayer_evp_rightAction, EasyVideoPlayer.RIGHT_ACTION_NONE);

                mCustomLabelText = a.getText(R.styleable.EasyVideoPlayer_evp_customLabelText);
                mRetryText = a.getText(R.styleable.EasyVideoPlayer_evp_retryText);
                mSubmitText = a.getText(R.styleable.EasyVideoPlayer_evp_submitText);
                mBottomLabelText = a.getText(R.styleable.EasyVideoPlayer_evp_bottomText);

                int restartDrawableResId = a.getResourceId(R.styleable.EasyVideoPlayer_evp_restartDrawable, -1);
                int playDrawableResId = a.getResourceId(R.styleable.EasyVideoPlayer_evp_playDrawable, -1);
                int pauseDrawableResId = a.getResourceId(R.styleable.EasyVideoPlayer_evp_pauseDrawable, -1);
                int fullscreenDrawableResId = a.getResourceId(R.styleable.EasyVideoPlayer_evp_fullscreenDrawable, -1);
                int fullscreenExitDrawableResId = a.getResourceId(R.styleable.EasyVideoPlayer_evp_fullscreenExitDrawable, -1);

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

                mHideControlsOnPlay = a.getBoolean(R.styleable.EasyVideoPlayer_evp_hideControlsOnPlay, true);
                mAutoPlay = a.getBoolean(R.styleable.EasyVideoPlayer_evp_autoPlay, false);
                mControlsDisabled = a.getBoolean(R.styleable.EasyVideoPlayer_evp_disableControls, false);

                mThemeColor = a.getColor(R.styleable.EasyVideoPlayer_evp_themeColor,
                        Util.resolveColor(context, R.attr.colorPrimary));

                mAutoFullscreen = a.getBoolean(R.styleable.EasyVideoPlayer_evp_autoFullscreen, false);

                mVideoSizeLoading = a.getFloat(R.styleable.EasyVideoPlayer_evp_videoSizeLoading, 16f / 10f);

            } finally {
                a.recycle();
            }
        } else {
            mLeftAction = EasyVideoPlayer.LEFT_ACTION_NONE;
            mRightAction = EasyVideoPlayer.RIGHT_ACTION_NONE;
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
                fragment = EasyVideoFragment.newInstant(false, attributeSet, mSource);
                ((AppCompatActivity) getContext()).getSupportFragmentManager().beginTransaction().add(getId(), fragment).commit();
            } else {
                fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentById(getId());
            }

            fragment.setCallback(this);
        }
    }

    public void setCallback(EasyVideoCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onStarted(EasyVideoPlayer player) {

    }

    @Override
    public void onPaused(EasyVideoPlayer player) {

    }

    @Override
    public void onPreparing(EasyVideoPlayer player) {

    }

    @Override
    public void onPrepared(EasyVideoPlayer player) {

    }

    @Override
    public void onBuffering(int percent) {

    }

    @Override
    public void onError(EasyVideoPlayer player, Exception e) {

    }

    @Override
    public void onCompletion(EasyVideoPlayer player) {

    }

    @Override
    public void onRetry(EasyVideoPlayer player, Uri source) {

    }

    @Override
    public void onSubmit(EasyVideoPlayer player, Uri source) {

    }

    @Override
    public void onFullScreen(EasyVideoPlayer player) {


        EasyVideoFragment fragment;

        if (((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag("" + getId()) == null) {

            fragment = EasyVideoFragment.newInstant(true, attributeSet, mSource);
            ((AppCompatActivity) getContext()).getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment, "" + getId()).addToBackStack("VIDEO").commit();
        } else {
            fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentByTag("" + getId());
        }
        fragment.setPlayer(player);
        fragment.setCallback(this);
        if (((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentById(getId()) != null) {
            fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentById(getId());
            fragment.setPlayer(null);
        }
        if (callback != null) callback.onFullScreen(player);
    }

    @Override
    public void onFullScreenExit(EasyVideoPlayer player) {
        if (((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentById(getId()) != null) {
            EasyVideoFragment fragment = (EasyVideoFragment) ((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentById(getId());
            fragment.setPlayer(player);
            fragment.setCallback(this);
            fragment.onAttach();
        }
    }

    @Override
    public void onCreatedView(EasyVideoPlayer player) {
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
        player.setThemeColor(mThemeColor);
        player.setAutoFullscreen(mAutoFullscreen);
        player.setVideoSizeLoading(mVideoSizeLoading);

    }

}
