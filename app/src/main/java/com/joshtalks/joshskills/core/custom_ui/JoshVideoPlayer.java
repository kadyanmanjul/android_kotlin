package com.joshtalks.joshskills.core.custom_ui;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static com.joshtalks.joshskills.messaging.RxBus2.publish;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.analytics.*;
import com.google.android.exoplayer2.audio.*;
import com.google.android.exoplayer2.offline.*;
import com.google.android.exoplayer2.source.*;
import com.google.android.exoplayer2.trackselection.*;
import com.google.android.exoplayer2.ui.*;
import com.google.android.exoplayer2.util.*;
import com.joshtalks.joshskills.R;
import com.joshtalks.joshskills.core.AppObjectController;
import com.joshtalks.joshskills.core.CountUpTimer;
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent;
import com.joshtalks.joshskills.core.analytics.AppAnalytics;
import com.joshtalks.joshskills.core.service.video_download.*;
import com.joshtalks.joshskills.repository.local.entity.VideoEngage;
import com.joshtalks.joshskills.repository.local.eventbus.MediaProgressEventBus;
import com.joshtalks.joshskills.repository.server.engage.Graph;
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;
import timber.log.*;

public class JoshVideoPlayer extends StyledPlayerView implements View.OnTouchListener, View.OnClickListener {
    private static final String TAG = "ManjulJVP";
    private final Handler timeHandler = new Handler();
    private final CountUpTimer countUpTimer = new CountUpTimer(false);
    private final Set<Graph> videoViewGraphList = new HashSet<>();
    private Uri uri;
    private long lastPosition = 0;
    private long currentPosition = 0;
    private Activity activity;
    private ImageView fullScreenToggle;
    private ScreenOrientation screenOrientation = ScreenOrientation.PORTRAIT;
    private ExoPlayer player;
    private boolean isVideoEnded = false;
    private float playbackSpeed = 1f;

    private final Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            timeHandler.postDelayed(this, 1000);

            if (player == null) {
                return;
            }

            long currentPosition = player.getCurrentPosition();
            publish(
                    new MediaProgressEventBus(
                            Download.STATE_DOWNLOADING, "0", currentPosition, (long) (countUpTimer.getTime() * playbackSpeed)
                    )
            );
            if (!(getContext() instanceof PlayerListener)) {
                return;
            }
            if (currentPosition == lastPosition) {
                return;
            }
            if (currentPosition != getExoPlayer().getDuration()) {
                findViewById(R.id.playAgain).setVisibility(View.GONE);
                findViewById(R.id.llControlsContainer).setVisibility(View.VISIBLE);
            }
            PlayerListener listener = (PlayerListener) getContext();
            listener.onCurrentTimeUpdated(currentPosition);

            lastPosition = currentPosition;

        }
    };
    private DefaultTrackSelector trackSelector;
    private Tracks lastSeenTrackGroups;
    private int startWindow;
    private GestureDetector gestureDetector;
    private PlayerControlViewVisibilityListener playerControlViewVisibilityListener;
    private PlayerFullScreenListener playerFullScreenListener;
    private PlayerEventCallback playerEventCallback;
    private PlayerCompletionCallback playerCompletionCallback;
    private ControllerButtonCallback controllerButtonCallback;
    private String videoId;
    @Nullable
    private Graph graph;
    private int courseId;
    private AppAnalytics appAnalytics;

    public JoshVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public JoshVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public JoshVideoPlayer(Context context) {
        super(context);
        init();
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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        pushAnalyticsEvents();
    }

    @Override
    public void addOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
        super.addOnAttachStateChangeListener(listener);
    }

    public void init() {
        fullScreenToggle = findViewById(R.id.ivFullScreenToggle);
        DefaultTimeBar defaultTimeBar = findViewById(R.id.exo_progress);
        isVideoEnded = false;
        appAnalytics = AppAnalytics.create(AnalyticsEvent.VIDEO_VIEW.getNAME())
                .addBasicParam()
                .addUserDetails();

        if (player == null) {
            try {
                AdaptiveTrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory();
                RenderersFactory renderersFactory = VideoDownloadController.getInstance().buildRenderersFactory(false);
                trackSelector = new DefaultTrackSelector(getContext(), trackSelectionFactory);
                DefaultTrackSelector.Parameters trackSelectorParameters = new DefaultTrackSelector.Parameters.Builder(getContext()).build();
                trackSelector.setParameters(trackSelectorParameters);
                lastSeenTrackGroups = null;
                player = new ExoPlayer.Builder(AppObjectController.getJoshApplication(), renderersFactory)
                        .setSeekBackIncrementMs(10000)
                        .setSeekForwardIncrementMs(10000)
                        .setUseLazyPreparation(true)
                        .setTrackSelector(trackSelector)
                        .build();

            } catch (Exception e) {
                player = new ExoPlayer.Builder(getContext())
                        .setUseLazyPreparation(true)
                        .setSeekBackIncrementMs(10000)
                        .setSeekForwardIncrementMs(10000)
                        .build();
                e.printStackTrace();
                Timber.tag(TAG).e(e, "init: ");
            }

            findViewById(R.id.playAgain).setOnClickListener(this);
            findViewById(R.id.playAgain).setVisibility(GONE);
            findViewById(R.id.exo_pause).setVisibility(GONE);
            findViewById(R.id.exo_play).setVisibility(VISIBLE);
            findViewById(R.id.playbackSpeed).setOnClickListener(this);
            findViewById(R.id.llControlsContainer).setVisibility(VISIBLE);
            player.addListener(new PlayerEventListener());
            player.setPlayWhenReady(true);
            setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);

            player.addAnalyticsListener(new AnalyticsListener() {
                @Override
                public void onPlaybackStateChanged(EventTime eventTime, int state) {
                    AnalyticsListener.super.onPlaybackStateChanged(eventTime, state);
                }

                @Override
                public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady, int playbackState) {
                    if (playWhenReady) {
                        countUpTimer.resume();
                    } else {
                        countUpTimer.pause();
                    }

                    if (getContext() instanceof PlayerListener) {

                        PlayerListener listener = (PlayerListener) getContext();

                        if (playbackState == Player.STATE_READY) {
                            listener.onPlayerReady();
                        }

                        listener.onBufferingUpdated(playbackState == Player.STATE_BUFFERING);

                    }

                    setKeepScreenOn(playbackState != Player.STATE_IDLE
                            && playbackState != Player.STATE_ENDED
                            && playWhenReady);

                }

            });

            defaultTimeBar.addListener(new TimeBar.OnScrubListener() {
                @Override
                public void onScrubStart(TimeBar timeBar, long position) {
                    try {
                        publish(
                                new MediaProgressEventBus(
                                        Download.STATE_DOWNLOADING, "0", position, (long) (countUpTimer.getTime() * playbackSpeed)
                                )
                        );
                        PlayerListener listener = (PlayerListener) getContext();
                        listener.onPositionDiscontinuity(1, player.getCurrentPosition());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onScrubMove(TimeBar timeBar, long position) {

                }

                @Override
                public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {

                }
            });


            timeHandler.post(timeRunnable);
            try {
                setControllerShowTimeoutMs(5000);
                setControllerVisibilityListener((ControllerVisibilityListener) visibility -> {
                    if (playerControlViewVisibilityListener != null) {
                        playerControlViewVisibilityListener.onVisibilityChange(visibility);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            setPlayer(player);


            // checkIfFullscreenToggleSupported();
            setupAudioFocus();
            attachForwardRewindAnimator();

            if (graph == null)
                graph = new Graph(player.getCurrentPosition());

        }

        player.seekTo(currentPosition);
        play();
        /*setPlayerControlViewVisibilityListener(new PlayerControlViewVisibilityListener() {
            @Override
            public void onVisibilityChange(int visibility) {

            }
        });*/

    }

    private void setupAudioFocus() {
        @SuppressLint("WrongConstant")
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
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
        try {
            if (VideoDownloadController.getInstance().getMediaSource(uri)!=null){
                player.setMediaSource(VideoDownloadController.getInstance().getMediaSource(uri), true);
                player.prepare();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void supportFullScreen() {
        checkIfFullscreenToggleSupported();
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

    public ExoPlayer getExoPlayer() {
        return player;
    }

    public void onStart() {
        if (Util.SDK_INT > 23) {
            init();
        }
    }

    public void onStop() {
        releasePlayer();
    }

    private void releasePlayer() {
        if (player != null) {
            if (getContext() instanceof PlayerListener) {
                ((PlayerListener) getContext()).onPlayerReleased();
            }
            currentPosition = player.getCurrentPosition();
            pushAnalyticsEvents();
            player.release();
        }
        player = null;
    }

    private void pushAnalyticsEvents() {
        try {
            System.out.println("JoshVideoPlayer.pushAnalyticsEvents " + uri + " " + videoId);
            if (uri != null && videoId != null) {
                if (graph != null) {
                    graph.setEndTime(player.getCurrentPosition());
                    videoViewGraphList.add(graph);

                    graph = null;
                    EngagementNetworkHelper.engageVideoApi(
                            new VideoEngage(
                                    new ArrayList<>(videoViewGraphList),
                                    Integer.parseInt(videoId),
                                    countUpTimer.getTime(),
                                    courseId
                            )
                    );
                }
                appAnalytics.addParam("video_url", uri.toString());
                appAnalytics.addParam("video_id", videoId);
                appAnalytics.push();
                countUpTimer.reset();
            }
        } catch (Exception e) {
            Timber.tag(TAG).e(e, "pushAnalyticsEvents: ");
        }
    }

    public void setUrl(String url) {
        uri = Uri.parse(url);
    }

    public void downloadStreamPlay() {
        try {
            if (player != null) {
                boolean haveStartPosition = startWindow != C.INDEX_UNSET;
                if (haveStartPosition) {
                    player.seekTo(startWindow, currentPosition);
                }

                MediaSource audioSource = VideoDownloadController.getInstance().getMediaSource(uri);
                if (audioSource != null) {
                    player.setHandleAudioBecomingNoisy(true);
                    player.setMediaSource(audioSource, true);
                    player.prepare();
                    player.setPlayWhenReady(true);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void seekToStart() {
        try {
            if (player != null) {
                player.seekTo(0);
                player.setPlayWhenReady(false);
                isVideoEnded = false;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void downloadStreamButNotPlay() {
        try {
            if (player != null) {
                boolean haveStartPosition = startWindow != C.INDEX_UNSET;
                if (haveStartPosition) {
                    player.seekTo(startWindow, currentPosition);
                }

                MediaSource audioSource = VideoDownloadController.getInstance().getMediaSource(uri);
                if (audioSource != null) {
                    player.setHandleAudioBecomingNoisy(true);
                    player.setMediaSource(audioSource, true);
                    player.prepare();
                    player.setPlayWhenReady(false);
                }
            }
        } catch (Exception ex) {
            Timber.tag(TAG).e(ex, "downloadStreamButNotPlay: ");
        }
    }

    public void setFullScreenListener(PlayerFullScreenListener playerFullScreenListener) {
        findViewById(R.id.exo_play).setOnClickListener(this);
        findViewById(R.id.exo_pause).setOnClickListener(this);
        findViewById(R.id.ivFullScreenToggleOp).setOnClickListener(this);
        findViewById(R.id.ivFullScreenToggleOp).setVisibility(VISIBLE);
        this.playerFullScreenListener = playerFullScreenListener;
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

    @SuppressLint("SetTextI18n")
    @Override
    public void onClick(View v) {
        try {
            if (v.getId() == R.id.ivFullScreenToggle) {
                if (getContext() instanceof FullscreenToggleListener)
                    ((FullscreenToggleListener) getContext()).onFullscreenToggle();
            } else if (v.getId() == R.id.exo_play) {
                if (!player.isPlaying()) {
                    onResume();
                    if (controllerButtonCallback != null)
                        controllerButtonCallback.onPlay();
                }
            }else if (v.getId() == R.id.exo_pause) {
                if (player.isPlaying()) {
                    onPause();
                }
            } else if (v.getId() == R.id.ivFullScreenToggleOp) {
                if (playerFullScreenListener != null) {
                    playerFullScreenListener.onFullScreen();
                }
            } else if (v.getId() == R.id.playAgain) {
                if (player != null) {
                    findViewById(R.id.playAgain).setVisibility(View.GONE);
                    findViewById(R.id.llControlsContainer).setVisibility(View.VISIBLE);
                    player.seekTo(0);
                    player.setPlayWhenReady(true);
                    isVideoEnded = false;
                    if (controllerButtonCallback != null)
                        controllerButtonCallback.onWatchAgain();
                }
            } else if (v.getId() == R.id.playbackSpeed) {
                if (player != null) {
                    PopupMenu popup = new PopupMenu(getContext(), v, Gravity.TOP);
                    popup.getMenuInflater().inflate(R.menu.playback_speed_menu, popup.getMenu());
                    popup.setOnMenuItemClickListener(item -> {
                        String t = item.getTitle().toString().replace("x", "");
                        playbackSpeed = Float.parseFloat(t);
                        player.setPlaybackParameters(new PlaybackParameters(playbackSpeed));
                        ((TextView) findViewById(R.id.playbackSpeed)).setText(t + "x");
                        return true;
                    });
                    popup.show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fitToScreen() {
        setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
    }

    public void setGestureDetector(GestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public long getLastPosition() {
        return lastPosition;
    }

    public PlayerControlViewVisibilityListener getPlayerControlViewVisibilityListener() {
        return playerControlViewVisibilityListener;
    }

    public void setPlayerControlViewVisibilityListener(PlayerControlViewVisibilityListener playerControlViewVisibilityListener) {
        this.playerControlViewVisibilityListener = playerControlViewVisibilityListener;
    }

    public void setPlayerEventCallback(PlayerEventCallback playerEventCallback) {
        this.playerEventCallback = playerEventCallback;
    }

    public void setSkipCallback(View.OnClickListener skipVideoListener) {
        findViewById(R.id.skip).setVisibility(View.VISIBLE);
        findViewById(R.id.skip).setOnClickListener(skipVideoListener);
    }

    public void setPlayerCompletionCallback(PlayerCompletionCallback playerCompletionCallback) {
        this.playerCompletionCallback = playerCompletionCallback;
    }

    public void setControllerButtonCallback(ControllerButtonCallback controllerButtonCallback) {
        this.controllerButtonCallback = controllerButtonCallback;
    }

    public long getSecondsWatched() {
        return (long) (countUpTimer.getTime() * playbackSpeed) / 1000;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(ev);
        }
        return super.onTouchEvent(ev);
    }

    public void onResume() {
        if (player != null) {
            player.setPlayWhenReady(true);
            findViewById(R.id.exo_pause).setVisibility(VISIBLE);
            findViewById(R.id.exo_play).setVisibility(GONE);
            int playerState = player.getPlaybackState();
            if (playerState == Player.STATE_IDLE) {
                downloadStreamPlay();
                player.setPlayWhenReady(true);
            }
            timeHandler.post(timeRunnable);
        }
    }

    public void onPause() {
        if (player != null) {
            findViewById(R.id.exo_pause).setVisibility(GONE);
            findViewById(R.id.exo_play).setVisibility(VISIBLE);
            currentPosition = player.getCurrentPosition();
            player.setPlayWhenReady(false);
            player.getPlaybackState();
        }
        timeHandler.removeCallbacks(timeRunnable);
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public void setProgress(Long progress) {
        currentPosition = progress;
        player.seekTo(progress);
    }

    public Long getProgress() {
        return player.getCurrentPosition();
    }

    public void setClickListners() {
        findViewById(R.id.playAgain).setOnClickListener(this);
        findViewById(R.id.exo_play).setOnClickListener(this);
        findViewById(R.id.exo_pause).setOnClickListener(this);
    }

    enum ScreenOrientation {
        PORTRAIT,
        LANDSCAPE
    }


    public interface PlayerControlViewVisibilityListener {
        void onVisibilityChange(int visibility);
    }

    public interface PlayerFullScreenListener {
        void onFullScreen();
    }

    public interface PlayerEventCallback {
        void onReceiveEvent(int event);
    }

    public interface PlayerCompletionCallback {
        void onCompleted();
    }

    public interface ControllerButtonCallback {
        void onPlay();

        void onWatchAgain();
    }

    private class PlayerEventListener implements Player.Listener {

        @Override
        public void onTracksChanged(Tracks trackGroups) {
            Player.Listener.super.onTracksChanged(trackGroups);
            try {
                if (trackGroups != lastSeenTrackGroups) {
                    MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                    if (mappedTrackInfo != null) {
                        if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                                == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        }
                        if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO)
                                == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        }
                    }
                    lastSeenTrackGroups = trackGroups;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            Player.Listener.super.onPlaybackStateChanged(playbackState);
            if (playerEventCallback != null) {
                playerEventCallback.onReceiveEvent(playbackState);
            }
            if (playbackState == Player.STATE_ENDED && !isVideoEnded) {
                isVideoEnded = true;
                if (playerCompletionCallback != null)
                    playerCompletionCallback.onCompleted();
                findViewById(R.id.playAgain).setVisibility(VISIBLE);
                findViewById(R.id.llControlsContainer).setVisibility(GONE);
            }

        }

        @Override
        public void onPlayerError(PlaybackException error) {
            Player.Listener.super.onPlayerError(error);
            error.printStackTrace();
        }
    }

}