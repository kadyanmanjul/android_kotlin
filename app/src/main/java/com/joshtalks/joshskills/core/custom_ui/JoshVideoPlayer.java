package com.joshtalks.joshskills.core.custom_ui;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.TimeBar;
import com.google.android.exoplayer2.util.Util;
import com.joshtalks.joshskills.R;
import com.joshtalks.joshskills.core.AppObjectController;
import com.joshtalks.joshskills.core.CountUpTimer;
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent;
import com.joshtalks.joshskills.core.analytics.AppAnalytics;
import com.joshtalks.joshskills.core.service.video_download.VideoDownloadController;
import com.joshtalks.joshskills.repository.local.entity.VideoEngage;
import com.joshtalks.joshskills.repository.local.eventbus.MediaProgressEventBus;
import com.joshtalks.joshskills.repository.server.engage.Graph;
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static com.joshtalks.joshskills.messaging.RxBus2.publish;

public class JoshVideoPlayer extends PlayerView implements View.OnTouchListener, View.OnClickListener {
    private final Handler timeHandler = new Handler();
    private final CountUpTimer countUpTimer = new CountUpTimer(false);
    private final Set<Graph> videoViewGraphList = new HashSet<>();
    private Uri uri;
    private long lastPosition = 0;
    private long currentPosition = 0;
    private Activity activity;
    private ImageView fullScreenToggle;
    private OrientationEventListener mOrientationListener;
    private ScreenOrientation screenOrientation = ScreenOrientation.PORTRAIT;
    private SimpleExoPlayer player;

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
                            Download.STATE_DOWNLOADING, "0", currentPosition, countUpTimer.getTime()
                    )
            );
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
    private DefaultTrackSelector trackSelector;
    private TrackGroupArray lastSeenTrackGroupArray;
    private boolean startAutoPlay;
    private int startWindow;
    private GestureDetector gestureDetector;
    private PlayerControlViewVisibilityListener playerControlViewVisibilityListener;
    private PlayerFullScreenListener playerFullScreenListener;
    private PlayerEventCallback playerEventCallback;
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

        appAnalytics = AppAnalytics.create(AnalyticsEvent.VIDEO_VIEW.getNAME())
                .addBasicParam()
                .addUserDetails();

        if (player == null) {
            try {
                TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory();
                RenderersFactory renderersFactory = VideoDownloadController.getInstance().buildRenderersFactory(false);
                trackSelector = new DefaultTrackSelector(trackSelectionFactory);
                DefaultTrackSelector.Parameters trackSelectorParameters = new DefaultTrackSelector.ParametersBuilder().build();
                trackSelector.setParameters(trackSelectorParameters);
                lastSeenTrackGroupArray = null;

                player = new SimpleExoPlayer.Builder(AppObjectController.getJoshApplication(), renderersFactory)
                        .setUseLazyPreparation(true)
                        .setTrackSelector(trackSelector)
                        .build();

            } catch (Exception e) {
                player = ExoPlayerFactory.newSimpleInstance(getContext());
                e.printStackTrace();
            }


            player.addListener(new PlayerEventListener());
            player.setPlayWhenReady(true);
            setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);

            player.addAnalyticsListener(new AnalyticsListener() {
                @Override
                public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady, int playbackState) {

                    if (playWhenReady) {
                        countUpTimer.resume();
                    } else {
                        countUpTimer.pause();
                    }

                    if (getContext() instanceof PlayerListener) {

                        PlayerListener listener = (PlayerListener) getContext();

                        if (playbackState == com.google.android.exoplayer2.Player.STATE_READY) {
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
                                        Download.STATE_DOWNLOADING, "0", position, countUpTimer.getTime()
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
                setControllerVisibilityListener(visibility -> {
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
        player.prepare(VideoDownloadController.getInstance().getMediaSource(uri), true, false);

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

    public SimpleExoPlayer getExoPlayer() {
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
                player.setMediaSource(audioSource, !haveStartPosition);
                player.setHandleAudioBecomingNoisy(true);
                player.setPlayWhenReady(true);
                player.prepare();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void downloadStreamButNotPlay() {
        if (player != null) {
            boolean haveStartPosition = startWindow != C.INDEX_UNSET;
            if (haveStartPosition) {
                player.seekTo(startWindow, currentPosition);
            }

            MediaSource audioSource = VideoDownloadController.getInstance().getMediaSource(uri);
            player.setMediaSource(audioSource, !haveStartPosition);
            player.setHandleAudioBecomingNoisy(true);
            player.setPlayWhenReady(false);
            player.prepare();
        }
    }

    public void setPlayListener(PlayerFullScreenListener playerFullScreenListener) {
        findViewById(R.id.exo_play).setOnClickListener(this);
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

    @Override
    public void onClick(View v) {
        try {

            if (v.getId() == R.id.ivFullScreenToggle) {
                if (getContext() instanceof FullscreenToggleListener)
                    ((FullscreenToggleListener) getContext()).onFullscreenToggle();
            } else if (v.getId() == R.id.exo_play) {
                if (!player.isPlaying()) {
                    onResume();
                }
            } else if (v.getId() == R.id.ivFullScreenToggleOp) {
                if (playerFullScreenListener != null) {
                    playerFullScreenListener.onFullScreen();
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
            player.getPlaybackState();
            timeHandler.post(timeRunnable);
        }
    }

    public void onPause() {
        if (player != null) {
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
        void onReceiveEvent(int event, boolean playbackState);
    }

    private class PlayerEventListener implements Player.EventListener {

        @Override
        @SuppressWarnings("ReferenceEquality")
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            try {
                if (trackGroups != lastSeenTrackGroupArray) {
                    MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                    if (mappedTrackInfo != null) {
                        if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                                == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        }
                        if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO)
                                == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        }
                    }
                    lastSeenTrackGroupArray = trackGroups;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playerEventCallback != null) {
                playerEventCallback.onReceiveEvent(playbackState, playWhenReady);
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException e) {

        }
    }

}