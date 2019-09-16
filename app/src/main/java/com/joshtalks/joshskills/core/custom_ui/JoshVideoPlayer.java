package com.joshtalks.joshskills.core.custom_ui;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.hls.playlist.DefaultHlsPlaylistParserFactory;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;
import com.joshtalks.joshskills.R;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static com.google.android.exoplayer2.C.VIDEO_SCALING_MODE_SCALE_TO_FIT;
import static com.google.android.exoplayer2.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING;

public class JoshVideoPlayer extends PlayerView implements View.OnTouchListener, View.OnClickListener {

    private SimpleExoPlayer player;

    private Uri uri;

    private long lastPosition = 0;
    private long currentPosition = 0;
    private Activity activity;
    private ImageView fullScreenToggle;
    private OrientationEventListener mOrientationListener;
    private ScreenOrientation screenOrientation = ScreenOrientation.PORTRAIT;

    enum ScreenOrientation {
        PORTRAIT,
        LANDSCAPE
    }

    public static final int STATE_IDLE = 1;
    public static final int STATE_BUFFERING = 2;
    public static final int STATE_READY = 3;
    public static final int STATE_ENDED = 4;


    private Handler timeHandler = new Handler();

    private Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            timeHandler.postDelayed(this, 1000);

            if (player == null) {
                return;
            }

            long currentPosition = player.getCurrentPosition();

            if (!(getContext() instanceof PlayerListener)) {
                return;
            }

            if (currentPosition == lastPosition)
                return;

            PlayerListener listener = (PlayerListener) getContext();
            listener.onCurrentTimeUpdated(currentPosition);

            lastPosition = currentPosition;

        }
    };

    public JoshVideoPlayer(Context context) {
        super(context);

        init();
    }


    public void init() {
        fullScreenToggle = findViewById(R.id.ivFullScreenToggle);


        if (player == null) {

            player = ExoPlayerFactory.newSimpleInstance(getContext());
            player.setPlayWhenReady(true);
           // player.setVideoScalingMode(VIDEO_SCALING_MODE_SCALE_TO_FIT);
            //setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
            //player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            fitToScreen();


            player.addAnalyticsListener(new AnalyticsListener() {
                @Override
                public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady, int playbackState) {

                    if (getContext() instanceof PlayerListener) {

                        PlayerListener listener = (PlayerListener) getContext();

                        if (playbackState == com.google.android.exoplayer2.Player.STATE_READY) {
                            listener.onPlayerReady();
                        }

                        if (playbackState == com.google.android.exoplayer2.Player.STATE_BUFFERING) {
                            listener.onBufferingUpdated(true);
                        } else {
                            listener.onBufferingUpdated(false);
                        }

                    }

                    if (playbackState == com.google.android.exoplayer2.Player.STATE_IDLE
                            || playbackState == com.google.android.exoplayer2.Player.STATE_ENDED
                            || !playWhenReady) {
                        setKeepScreenOn(false);
                    } else {
                        setKeepScreenOn(true);
                    }

                }

            });
            player.addVideoListener(new VideoListener() {
                // This is where we will resize view to fit aspect ratio of video
                @Override
                public void onVideoSizeChanged(
                        int width,
                        int height,
                        int unappliedRotationDegrees,
                        float pixelWidthHeightRatio
                ) {
                    // Get layout params of view
                    // Use MyView.this to refer to the current MyView instance
                    // inside a callback
                   // LayoutParams p =getLayoutParams();
                    //int currWidth = MyView.this.getWidth();

                    // Set new width/height of view
                    // height or width must be cast to float as int/int will give 0
                    // and distort view, e.g. 9/16 = 0 but 9.0/16 = 0.5625.
                    // p.height is int hence the final cast to int.
                    //p.width = currWidth;
                    //p.height = (int) ((float) height / width * currWidth);

                    // Redraw myView
                    //MyView.this.requestLayout();
                }

                @Override
                public void onRenderedFirstFrame() {
                    // ...
                }
            });


            timeHandler.post(timeRunnable);
            setPlayer(player);

            checkIfFullscreenToggleSupported();
            setupAudioFocus();
            attachForwardRewindAnimator();

        }

        player.seekTo(currentPosition);
        play();
        addOrientationListener();

    }

    private void checkIfFullscreenToggleSupported() {


        findViewById(R.id.ivFullScreenToggle).setVisibility(
                getContext() instanceof FullscreenToggleListener
                        ? View.VISIBLE
                        : View.GONE
        );
        findViewById(R.id.ivFullScreenToggle).setVisibility(VISIBLE);
        fullScreenToggle.setOnClickListener(v -> {
            if (screenOrientation == ScreenOrientation.PORTRAIT) {
                activity.setRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);
                screenOrientation = ScreenOrientation.LANDSCAPE;
            } else {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                screenOrientation = ScreenOrientation.PORTRAIT;
            }

        });

    }

    private void addOrientationListener() {
        if (mOrientationListener != null) {
            return;
        }

    }

    private void setupAudioFocus() {

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MOVIE)
                .build();

        player.setAudioAttributes(audioAttributes, true);

    }

    private void attachForwardRewindAnimator() {
        findViewById(R.id.exo_ffwd).setOnTouchListener(this);
        findViewById(R.id.exo_rew).setOnTouchListener(this);
        //    findViewById(R.id.ivFullScreenToggle).setOnClickListener(this);
    }

    public void play() {

        if (uri == null || player == null)
            return;

        player.prepare(buildMediaSource(), true, false);

    }

    public SimpleExoPlayer getExoPlayer() {

        return player;
    }

    public void setVideScalingMode(@C.VideoScalingMode int scaleMode) {
        player.setVideoScalingMode(scaleMode);
    }

    private MediaSource buildMediaSource() {

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getContext(),
                Util.getUserAgent(getContext(), "joshtalks"));

        if (uri.getLastPathSegment().endsWith("m3u8")) {

            return new HlsMediaSource.Factory(dataSourceFactory)
                    .setPlaylistParserFactory(new DefaultHlsPlaylistParserFactory())
                    .createMediaSource(uri);

        } else {

            return new ExtractorMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(uri);

        }

    }

    public JoshVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public JoshVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void onStart() {
        if (Util.SDK_INT > 23) {
            init();
        }
    }


    public void onPause() {
        player.setPlayWhenReady(false);
        player.getPlaybackState();
    }

    public void onResume() {
        player.setPlayWhenReady(true);
        player.getPlaybackState();
    }

    private void releasePlayer() {
        if (player != null) {

            if (getContext() instanceof PlayerListener) {
                ((PlayerListener) getContext()).onPlayerReleased();
            }

            currentPosition = player.getCurrentPosition();

            player.release();
            player = null;
        }
    }

    public void onStop() {
        releasePlayer();
    }

    public void setUrl(String url) {
        uri = Uri.parse(url);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (event.getAction() != MotionEvent.ACTION_DOWN)
            return false;

        if (v.getId() == R.id.exo_ffwd) {
            v.setRotation(90);
        }

        if (v.getId() == R.id.exo_rew) {
            v.setRotation(-90);
        }

        v.animate().setInterpolator(new DecelerateInterpolator())
                .rotation(0)
                .start();

        return false;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.ivFullScreenToggle:
                if (getContext() instanceof FullscreenToggleListener)
                    ((FullscreenToggleListener) getContext()).onFullscreenToggle();
                break;
        }

    }

    public void fitToScreen() {
        setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
    }

    public static AppCompatActivity getActivityFromView(View view) {
        if (null != view) {
            Context context = view.getContext();
            while (context instanceof ContextWrapper) {
                if (context instanceof AppCompatActivity) {
                    return (AppCompatActivity) context;
                }
                context = ((ContextWrapper) context).getBaseContext();
            }
        }
        return null;
    }


    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public long getLastPosition() {
        return lastPosition;
    }
}