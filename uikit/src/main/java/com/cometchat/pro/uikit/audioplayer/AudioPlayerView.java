package com.cometchat.pro.uikit.audioplayer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.cometchat.pro.uikit.R;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import constant.StringContract;
import listeners.ComposeActionListener;
import utils.Utils;

public class AudioPlayerView extends RelativeLayout implements View.OnClickListener {

    private static final String TAG = com.cometchat.pro.uikit.ComposeBox.ComposeBox.class.getName();
    private final Handler seekHandler = new Handler(Looper.getMainLooper());
    private final Bundle bundle = new Bundle();
    public ImageView ivSend, ivMic, ivDelete;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private Runnable timerRunnable;
    private Timer timer = new Timer();
    private String audioFileNameWithPath;
    private boolean isOpen, isRecording, isPlaying, voiceMessage;
    private SeekBar voiceSeekbar;
    private Chronometer recordTime;
    private RelativeLayout voiceMessageLayout;
    private boolean hasFocus;
    private ComposeActionListener composeActionListener;
    private Context context;

    public AudioPlayerView(Context context) {
        super(context);
        initViewComponent(context, null, -1, -1);
    }

    public AudioPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViewComponent(context, attrs, -1, -1);
    }

    public AudioPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViewComponent(context, attrs, defStyleAttr, -1);
    }

    private void initViewComponent(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {

        View view = View.inflate(context, R.layout.layout_audio_player, null);

        TypedArray a = getContext().getTheme().obtainStyledAttributes(attributeSet, R.styleable.ComposeBox, 0, 0);
        addView(view);

        this.context = context;

        ViewGroup viewGroup = (ViewGroup) view.getParent();
        viewGroup.setClipChildren(false);

        mediaPlayer = new MediaPlayer();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.isMusicActive()) {
            audioManager.requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {

                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        stopRecording(true);
                    }
                }
            }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        ivMic = this.findViewById(R.id.ivMic);
        ivDelete = this.findViewById(R.id.ivDelete);
        voiceMessageLayout = this.findViewById(R.id.voiceMessageLayout);
        recordTime = this.findViewById(R.id.record_time);
        voiceSeekbar = this.findViewById(R.id.voice_message_seekbar);
        ivSend = this.findViewById(R.id.ivSend);

        ivSend.setOnClickListener(this);
        ivDelete.setOnClickListener(this);
        ivMic.setOnClickListener(this);

        a.recycle();
    }

    public void setComposeBoxListener(ComposeActionListener composeActionListener) {
        this.composeActionListener = composeActionListener;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.ivDelete) {
            stopRecording(true);
            stopPlayingAudio();
            voiceMessageLayout.setVisibility(GONE);
            ivMic.setVisibility(View.VISIBLE);
            ivMic.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_white_24dp));
            isPlaying = false;
            isRecording = false;
            voiceMessage = false;
            ivDelete.setVisibility(GONE);
            ivSend.setVisibility(View.GONE);
        }

        if (view.getId() == R.id.ivSend) {
            composeActionListener.onVoiceNoteComplete(audioFileNameWithPath);
            audioFileNameWithPath = "";
            voiceMessageLayout.setVisibility(GONE);
            ivDelete.setVisibility(GONE);
            ivSend.setVisibility(GONE);
            ivMic.setVisibility(View.VISIBLE);
            isRecording = false;
            isPlaying = false;
            voiceMessage = false;
            ivMic.setImageResource(R.drawable.ic_mic_white_24dp);

        }
        if (view.getId() == R.id.ivMic) {
            if (Utils.hasPermissions(context, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                if (isOpen) {
//                    closeActionContainer();
                }
                if (!isRecording) {
                    startRecord();
                    ivMic.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop_24dp));
                    isRecording = true;
                    isPlaying = false;
                } else {
                    if (isRecording && !isPlaying) {
                        isPlaying = true;
                        stopRecording(false);
                        recordTime.stop();
                    }
                    ivMic.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));
                    ivSend.setVisibility(View.VISIBLE);
                    ivDelete.setVisibility(View.VISIBLE);
                    voiceSeekbar.setVisibility(View.VISIBLE);
                    voiceMessage = true;
                    if (audioFileNameWithPath != null)
                        startPlayingAudio(audioFileNameWithPath);
                    else
                        Toast.makeText(getContext(), "No File Found. Please", Toast.LENGTH_LONG).show();
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ((Activity) context).requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            StringContract.RequestCode.RECORD);
                }
            }
        }
    }

    public void usedIn(String className) {
        bundle.putString("type", className);
    }

    public void startRecord() {
        recordTime.setBase(SystemClock.elapsedRealtime());
        recordTime.start();
        voiceSeekbar.setVisibility(GONE);
        voiceMessageLayout.setVisibility(View.VISIBLE);
        startRecording();
    }

    private void startPlayingAudio(String path) {
        try {

            if (timerRunnable != null) {
                seekHandler.removeCallbacks(timerRunnable);
                timerRunnable = null;
            }

            mediaPlayer.reset();
            if (Utils.hasPermissions(context, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                mediaPlayer.setDataSource(path);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ((Activity) context).requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            StringContract.RequestCode.READ_STORAGE);
                } else {
                    Toast.makeText(context, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
            }

            final int duration = mediaPlayer.getDuration();
            voiceSeekbar.setMax(duration);
            recordTime.setBase(SystemClock.elapsedRealtime());
            recordTime.start();
            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    int pos = mediaPlayer.getCurrentPosition();
                    voiceSeekbar.setProgress(pos);

                    if (mediaPlayer.isPlaying() && pos < duration) {
//                        audioLength.setText(Utils.convertTimeStampToDurationTime(player.getCurrentPosition()));
                        seekHandler.postDelayed(this, 100);
                    } else {
                        seekHandler
                                .removeCallbacks(timerRunnable);
                        timerRunnable = null;
                    }
                }

            };
            seekHandler.postDelayed(timerRunnable, 100);
            mediaPlayer.setOnCompletionListener(mp -> {
                seekHandler
                        .removeCallbacks(timerRunnable);
                timerRunnable = null;
                mp.stop();
                recordTime.stop();
//                audioLength.setText(Utils.convertTimeStampToDurationTime(duration));
                voiceSeekbar.setProgress(0);
//                playButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            });

        } catch (Exception e) {
            Log.e("playAudioError: ", e.getMessage());
            stopPlayingAudio();
        }
    }


    private void stopPlayingAudio() {
        if (mediaPlayer != null)
            mediaPlayer.stop();
    }

    private void startRecording() {
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            audioFileNameWithPath = Utils.getOutputMediaFile(getContext());
            mediaRecorder.setOutputFile(audioFileNameWithPath);
            try {
                mediaRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    int currentMaxAmp = 0;
                    try {
                        currentMaxAmp = mediaRecorder != null ? mediaRecorder.getMaxAmplitude() : 0;
                        // audioRecordView.update(currentMaxAmp);
                        if (mediaRecorder == null)
                            timer = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 100);
            mediaRecorder.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRecording(boolean isCancel) {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                if (isCancel) {
                    new File(audioFileNameWithPath).delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
