package com.joshtalks.joshskills.core.custom_ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.joshtalks.joshskills.R;
import com.joshtalks.joshskills.core.AppObjectController;
import com.joshtalks.joshskills.core.Utils;
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent;
import com.joshtalks.joshskills.core.analytics.AppAnalytics;
import com.joshtalks.joshskills.core.interfaces.AudioPlayerInterface;
import com.joshtalks.joshskills.core.io.AppDirectory;
import com.joshtalks.joshskills.repository.local.entity.AudioType;
import com.joshtalks.joshskills.repository.local.entity.ChatModel;
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS;
import com.joshtalks.joshskills.util.AudioPlayerManager;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.pnikosis.materialishprogress.ProgressWheel;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AudioView extends FrameLayout {

    private static final String TAG = AudioView.class.getSimpleName();
    ChatModel message;
    private final @NonNull
    AnimatingToggle controlToggle;
    private final @NonNull
    ImageView playButton;
    private final @NonNull
    ImageView pauseButton;
    private final @NonNull
    ImageView downloadButton;
    private final @NonNull
    SeekBar seekPlayerProgress;
    private final @NonNull
    TextView timestamp;
    private int backwardsCounter;

    private boolean isPlaying = false;
    private Uri uri;
    private AudioPlayerManager audioPlayerManager;
    private Long duration;
    private static final int SEEKBAR_STEPS = 100;
    public static final int MAXIMUM_WORK_CYCLES = 500000;
    private static final float WORK_CYCLES_PER_STEP = MAXIMUM_WORK_CYCLES / SEEKBAR_STEPS;
    private static int workCycles = 0;
    private static final int AUDIO_PROGRESS_UPDATE_TIME = 100;
    private AudioPlayerInterface audioPlayerInterface;


    AppCompatImageView startDownloadIV;
    ProgressWheel progressWheel;
    AppCompatImageView cancelDownloadIv;
    Activity activity;
    FrameLayout downloadContainer;


    private ExoPlayer.EventListener eventListener = new ExoPlayer.EventListener() {
        @Override
        public void onTimelineChanged(Timeline timeline, @Nullable Object manifest, int reason) {

        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }

        @Override
        public void onLoadingChanged(boolean isLoading) {

        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Log.i(TAG, "onPlayerStateChanged: playWhenReady = " + String.valueOf(playWhenReady)
                    + " playbackState = " + playbackState);
            switch (playbackState) {
                case ExoPlayer.STATE_ENDED:
                    Log.i(TAG, "Playback ended!");
                    setPause();
                    audioPlayerManager.seekTo(0);
                    break;
                case ExoPlayer.STATE_READY:
                    Log.i(TAG, "ExoPlayer ready! pos: " +
                            audioPlayerManager.getCurrentPosition()
                            + " max: " + (int) audioPlayerManager.getDuration());
                    setProgress();

                    break;
                case ExoPlayer.STATE_BUFFERING:
                    Log.i(TAG, "Playback buffering!");
                    break;
                case ExoPlayer.STATE_IDLE:
                    Log.i(TAG, "ExoPlayer idle!");
                    break;
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {

        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {

        }

        @Override
        public void onPositionDiscontinuity(int reason) {

        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

        }

        @Override
        public void onSeekProcessed() {

        }
    };

    public AudioView(Context context) {
        this(context, null);
    }

    public AudioView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AudioView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.audio_view, this);
        audioPlayerManager = AudioPlayerManager.getInstance(getContext());
        this.controlToggle = (AnimatingToggle) findViewById(R.id.control_toggle);
        this.playButton = (ImageView) findViewById(R.id.play);
        this.pauseButton = (ImageView) findViewById(R.id.pause);
        this.downloadButton = (ImageView) findViewById(R.id.download);
        this.seekPlayerProgress = (SeekBar) findViewById(R.id.seek);
        this.timestamp = (TextView) findViewById(R.id.timestamp);

        this.startDownloadIV = findViewById(R.id.iv_start_download);
        this.progressWheel = findViewById(R.id.progress_dialog);
        this.cancelDownloadIv = findViewById(R.id.iv_cancel_download);
        this.downloadContainer = findViewById(R.id.download_container);
        this.startDownloadIV.setOnClickListener(new DownloadingStartClickedListener());
        this.cancelDownloadIv.setOnClickListener(new DownloadingCancelClickedListener());


        this.playButton.setOnClickListener(new PlayClickedListener());
        this.pauseButton.setOnClickListener(new PauseClickedListener());

    }


    public void prepareAudioPlayer(Activity activity, ChatModel obj, AudioPlayerInterface audioPlayerInterface) {
        this.activity = activity;
        this.message = obj;
        this.audioPlayerInterface = audioPlayerInterface;
        updateUI();

    }


    private void initSeekBar() {
        seekPlayerProgress.requestFocus();
        seekPlayerProgress.setProgress(0);

        seekPlayerProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    // We're not interested in programmatically generated changes to
                    // the progress bar's position.
                    return;
                }

                audioPlayerManager.seekTo(progress * 1000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekPlayerProgress.setMax(0);
        seekPlayerProgress.setMax((int) audioPlayerManager.getDuration() / 1000);

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //  if (!EventBus.getDefault().isRegistered(this)) EventBus.getDefault().register(this);
    }


    private class PlayClickedListener implements View.OnClickListener {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onClick(View v) {
            try {
                // Log.d(TAG, "playbutton onClick");
                // if (audioSlidePlayer != null) {
                //   togglePlayToPause();
                // audioSlidePlayer.play(getProgress());
                // }
                setPlay();
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }
    }

    private class PauseClickedListener implements View.OnClickListener {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onClick(View v) {
            Log.d(TAG, "pausebutton onClick");
            setPause();
            controlToggle.displayQuick(playButton);


            /*if (audioSlidePlayer != null) {
                togglePauseToPlay();
                audioSlidePlayer.stop();
            }*/
        }
    }


    private class SeekBarModifiedListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }

        @Override
        public synchronized void onStartTrackingTouch(SeekBar seekBar) {
            /*if (audioSlidePlayer != null && pauseButton.getVisibility() == View.VISIBLE) {
                audioSlidePlayer.stop();
            }*/
        }

        @Override
        public synchronized void onStopTrackingTouch(SeekBar seekBar) {
            /*try {
                if (audioSlidePlayer != null && pauseButton.getVisibility() == View.VISIBLE) {
                    audioSlidePlayer.play(getProgress());
                }
            } catch (IOException e) {
                Log.w(TAG, e);
            }*/
        }
    }

    private class DownloadingStartClickedListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            audioPlayerInterface.downloadInQueue();
        }
    }

    private class DownloadingCancelClickedListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            audioPlayerInterface.downloadStop();


        }
    }


    private void setPlay() {
        AppAnalytics.create(AnalyticsEvent.AUDIO_OPENED.getNAME()).addParam("ChatId",message.getChatId());
        isPlaying = true;
        audioPlayerManager.play(this.uri, eventListener);
        setProgress();
        controlToggle.displayQuick(pauseButton);
        if (Utils.INSTANCE.getCurrentMediaVolume(getContext()) <= 0) {
            showToast();
        }

    }


    private void setPause() {
        isPlaying = false;
        audioPlayerManager.pause();
        controlToggle.displayQuick(playButton);
        setTimeStampOfAudio();

    }


    private void setProgress() {
        Log.e(TAG, "PROGRESS");
        seekPlayerProgress.setProgress(audioPlayerManager.getCurrentPosition());
        seekPlayerProgress.setMax(audioPlayerManager.getDuration() / 1000);
        //txtCurrentTime.setText(stringForTime((int)exoPlayer.getCurrentPosition()));
        //txtEndTime.setText(stringForTime((int)exoPlayer.getDuration()));
        //Make sure you update Seekbar on UI thread
        AppObjectController.getUiHandler().post(new Runnable() {
            @Override
            public void run() {
                if (audioPlayerManager != null && isPlaying) {
                    seekPlayerProgress.setMax((int) audioPlayerManager.getDuration() / 1000);
                    int mCurrentPosition = (int) audioPlayerManager.getCurrentPosition() / 1000;
                    seekPlayerProgress.setProgress(mCurrentPosition);
                    timestamp.setText(String.format(Locale.getDefault(), "%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(audioPlayerManager.getCurrentPosition()),
                            TimeUnit.MILLISECONDS.toSeconds(audioPlayerManager.getCurrentPosition())));
                    //  txtCurrentTime.setText(stringForTime((int)exoPlayer.getCurrentPosition()));
                    //txtEndTime.setText(stringForTime((int)exoPlayer.getDuration()));

                    AppObjectController.getUiHandler().postDelayed(this, AUDIO_PROGRESS_UPDATE_TIME);
                }
            }
        });
    }


    void setTimeStampOfAudio() {
        try {
            Long duration = Utils.INSTANCE.getDurationOfMedia(getContext(), uri.getPath());
            if (duration == null) {
                duration = this.duration;

            }
            timestamp.setText(String.format(Locale.getDefault(), "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(duration),
                    TimeUnit.MILLISECONDS.toSeconds(duration)));
        } catch (Exception e) {
            //  e.printStackTrace();
        }
    }


    private void showToast() {
        Toast toast = Toast.makeText(getContext(), getContext().getString(R.string.volume_up_message), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }


    void updateUI() {
        if (message.getUrl() != null) {
            if (message.getDownloadStatus() == DOWNLOAD_STATUS.DOWNLOADED) {
                if (message.getDownloadedLocalPath() != null && AppDirectory.isFileExist(message.getDownloadedLocalPath())) {
                    Dexter.withActivity(activity)
                            .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                            .withListener(new PermissionListener() {
                                @Override
                                public void onPermissionGranted(PermissionGrantedResponse response) {
                                    mediaDownloaded();
                                }

                                @Override
                                public void onPermissionDenied(PermissionDeniedResponse response) {

                                }

                                @Override
                                public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                                    mediaNotDownloaded();
                                }
                            }).check();

                } else {
                    mediaNotDownloaded();


                }


            } else if (message.getDownloadStatus() == DOWNLOAD_STATUS.DOWNLOADING) {
                mediaDownloading();
                audioPlayerInterface.downloadStart(message.getUrl());
            } else {
                mediaNotDownloaded();

            }
        } else {

            if (message.getQuestion() != null && message.getQuestion().getAudioList() != null && message.getQuestion().getAudioList().size() > 0) {
                AudioType audioTypeObj = message.getQuestion().getAudioList().get(0);
                if (message.getDownloadStatus() == DOWNLOAD_STATUS.DOWNLOADED) {

                    if (audioTypeObj.getDownloadedLocalPath() != null && AppDirectory.isFileExist(audioTypeObj.getDownloadedLocalPath())) {
                        Dexter.withActivity(activity)
                                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                                .withListener(new PermissionListener() {
                                    @Override
                                    public void onPermissionGranted(PermissionGrantedResponse response) {
                                        mediaDownloaded();
                                    }

                                    @Override
                                    public void onPermissionDenied(PermissionDeniedResponse response) {

                                    }

                                    @Override
                                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                                        mediaNotDownloaded();
                                    }
                                }).check();

                    } else {
                        mediaNotDownloaded();
                    }

                } else if (message.getDownloadStatus() == DOWNLOAD_STATUS.DOWNLOADING) {
                    mediaDownloading();
                    audioPlayerInterface.downloadStart(audioTypeObj.getAudio_url());
                } else {
                    mediaNotDownloaded();
                }
            }
        }
    }

    private void mediaNotDownloaded() {
        downloadContainer.setVisibility(VISIBLE);
        controlToggle.setVisibility(GONE);
        progressWheel.setVisibility(GONE);
        cancelDownloadIv.setVisibility(GONE);
        startDownloadIV.setVisibility(VISIBLE);

    }

    private void mediaDownloading() {
        downloadContainer.setVisibility(VISIBLE);
        controlToggle.setVisibility(GONE);
        progressWheel.setVisibility(VISIBLE);
        cancelDownloadIv.setVisibility(VISIBLE);
        startDownloadIV.setVisibility(GONE);
    }

    private void mediaDownloaded() {
        downloadContainer.setVisibility(GONE);
        controlToggle.setVisibility(VISIBLE);
        initSeekBar();
        setTimeStampOfAudio();
        try {
            if (message.getUrl() != null) {
                this.uri = Uri.fromFile(new File(message.getUrl()));
                this.duration = Utils.getDurationOfMedia(getContext(), message.getUrl());

            } else {
                this.uri = Uri.fromFile(new File(message.getQuestion().getAudioList().get(0).getDownloadedLocalPath()));
                this.duration = Utils.getDurationOfMedia(getContext(), message.getQuestion().getAudioList().get(0).getDownloadedLocalPath());

            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }


}

