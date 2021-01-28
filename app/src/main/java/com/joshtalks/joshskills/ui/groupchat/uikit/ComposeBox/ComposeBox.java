package com.joshtalks.joshskills.ui.groupchat.uikit.ComposeBox;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.cometchat.pro.core.CometChat;
import com.joshtalks.joshskills.R;
import com.joshtalks.joshskills.core.PermissionUtils;
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent;
import com.joshtalks.joshskills.core.analytics.AppAnalytics;
import com.joshtalks.joshskills.ui.groupchat.listeners.ComposeActionListener;
import com.joshtalks.joshskills.ui.groupchat.utils.Utils;
import com.joshtalks.recordview.CustomRippleButton;
import com.joshtalks.recordview.OnRecordListener;
import com.joshtalks.recordview.RecordView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import info.kimjihyok.ripplelibrary.Rate;

import static com.joshtalks.joshskills.core.StaticConstantKt.EMPTY;
import static com.joshtalks.recordview.CustomImageButton.FIRST_STATE;
import static com.joshtalks.recordview.CustomImageButton.SECOND_STATE;


public class ComposeBox extends ConstraintLayout implements View.OnClickListener {

    private static final String TAG = ComposeBox.class.getName();
    private final Handler seekHandler = new Handler(Looper.getMainLooper());
    private final Bundle bundle = new Bundle();
    public ImageView ivAudio, ivCamera, ivGallery, ivFile, ivArrow;
    public CometChatEditText etComposeBox;
    public boolean isGalleryVisible = true, isAudioVisible = true, isCameraVisible = true,
            isFileVisible = true, isLocationVisible = true, isPollVisible = true;
    public CustomRippleButton recordButton;
    ImageView sendButton;
    public RecordView recordView;
    public CardView replyMessageLayout;
    public TextView replyTitle;
    public TextView replyMessage;
    public ImageView replyMedia;
    public ImageView replyClose;
    public View indicatorView;
    //    public VoiceRippleView voiceRipple;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private Runnable timerRunnable;
    private Timer timer = new Timer();
    private ComposeBoxActionFragment composeBoxActionFragment;
    private String audioFileNameWithPath;
    private ConstraintLayout composeBox;
    private RelativeLayout flBox;
    private RelativeLayout rlActionContainer;
    private ComposeActionListener composeActionListener;
    private Context context;
    private int color;
    private boolean listenTextChange = true;
    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (!listenTextChange)
                return;
            if (s != null && !s.toString().trim().equals(EMPTY)) {
//                recordButton.goToState(SECOND_STATE);
                goToState(SECOND_STATE);
                recordButton.setListenForRecord(false);
            } else {
//                recordButton.goToState(FIRST_STATE);
                goToState(FIRST_STATE);
                recordButton.setListenForRecord(PermissionUtils.checkPermissionForAudioRecord(getContext()));
            }
        }
    };

    private void goToState(int state) {
        if (state == SECOND_STATE) {
            sendButton.setVisibility(VISIBLE);
            recordButton.setVisibility(GONE);
        } else {
            sendButton.setVisibility(GONE);
            recordButton.setVisibility(VISIBLE);
        }
    }

    public ComposeBox(Context context) {
        super(context);
        initViewComponent(context, null, -1, -1);
    }

    public ComposeBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViewComponent(context, attrs, -1, -1);
    }

    public ComposeBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViewComponent(context, attrs, defStyleAttr, -1);
    }

    public void setAudioButtonVisible(boolean result) {
        isAudioVisible = result;
    }

    public void setGalleryButtonVisible(boolean result) {
        isGalleryVisible = result;
    }

    public void setCameraButtonVisible(boolean result) {
        isCameraVisible = result;
    }

    public void setFileButtonVisible(boolean result) {
        isFileVisible = result;
    }

    public void setLocationButtonVisible(boolean result) {
        isLocationVisible = result;
    }

    private void initViewComponent(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {

        View view = View.inflate(context, R.layout.layout_compose_box, null);

        TypedArray a = getContext().getTheme().obtainStyledAttributes(attributeSet, R.styleable.ComposeBox, 0, 0);
        color = a.getColor(R.styleable.ComposeBox_color, getResources().getColor(R.color.colorPrimary));
        addView(view);

        this.context = context;

        ViewGroup viewGroup = (ViewGroup) view.getParent();
        viewGroup.setClipChildren(false);

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.isMusicActive()) {
            audioManager.requestAudioFocus(focusChange -> {
                if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {

                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    hideRecordView();
                    stopRecord(true);
                }
            }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        composeBox = this.findViewById(R.id.message_box);
        flBox = this.findViewById(R.id.flBox);
        ivCamera = this.findViewById(R.id.ivCamera);
        ivGallery = this.findViewById(R.id.ivImage);
        ivAudio = this.findViewById(R.id.ivAudio);
        ivFile = this.findViewById(R.id.ivFile);
        ivArrow = this.findViewById(R.id.ivArrow);
        etComposeBox = this.findViewById(R.id.etComposeBox);
        rlActionContainer = this.findViewById(R.id.rlActionContainers);
        sendButton = this.findViewById(R.id.send_button);
        recordButton = this.findViewById(R.id.voice_ripple_view);
        recordView = this.findViewById(R.id.record_view);
        replyMessageLayout = findViewById(R.id.replyMessageLayout);
        replyTitle = findViewById(R.id.tv_reply_layout_title);
        replyMessage = findViewById(R.id.tv_reply_layout_subtitle);
        replyMedia = findViewById(R.id.iv_reply_media);
        replyClose = findViewById(R.id.iv_reply_close);
        indicatorView = findViewById(R.id.indicatorView);
//        voiceRipple = findViewById(R.id.voice_ripple_view);

        recordButton.setRippleColor(ContextCompat.getColor(context, R.color.colorPrimary));
        recordButton.setRippleSampleRate(Rate.LOW);
        recordButton.setRippleDecayRate(Rate.LOW);
        recordButton.setBackgroundRippleRatio(2.0);
// set inner icon for record and recording
        recordButton.setRecordDrawable(ContextCompat.getDrawable(context, R.drawable.recv_ic_mic_white), ContextCompat.getDrawable(context, R.drawable.recv_ic_mic_white));

        ivAudio.setOnClickListener(this);
        ivArrow.setOnClickListener(this);
        ivFile.setOnClickListener(this);
        ivGallery.setOnClickListener(this);
        ivCamera.setOnClickListener(this);

        // etComposeBox.setHint(String.format(context.getString(R.string.message), CometChat.getLoggedInUser().getName().split(" ")[0]));
        composeBoxActionFragment = new ComposeBoxActionFragment();
        composeBoxActionFragment.setComposeBoxActionListener(new ComposeBoxActionFragment.ComposeBoxActionListener() {
            @Override
            public void onGalleryClick() {
                composeActionListener.onGalleryActionClicked();
            }

            @Override
            public void onCameraClick() {
                composeActionListener.onCameraActionClicked();
            }

            @Override
            public void onFileClick() {
                composeActionListener.onFileActionClicked();
            }

            @Override
            public void onAudioClick() {
                composeActionListener.onAudioActionClicked();
            }

            @Override
            public void onLocationClick() {
                composeActionListener.onLocationActionClicked();
            }

            @Override
            public void onPollClick() {
                composeActionListener.onPollActionClicked();
            }
        });
        etComposeBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (composeActionListener != null) {
                    composeActionListener.beforeTextChanged(charSequence, i, i1, i2);
                }

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (composeActionListener != null) {
                    composeActionListener.onTextChanged(charSequence, i, i1, i2);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (composeActionListener != null) {
                    composeActionListener.afterTextChanged(editable);
                }
            }
        });
        etComposeBox.setMediaSelected(inputContentInfoCompat -> composeActionListener.onEditTextMediaSelected(inputContentInfoCompat));
        a.recycle();

        recordButton.setRecordView(recordView);
        recordView.setCancelBounds(2f);
        recordView.setSmallMicColor(Color.parseColor("#c2185b"));
        recordView.setLessThanSecondAllowed(false);
        recordView.setSlideToCancelText(getContext().getString(R.string.slide_to_cancel));
        recordView.setCustomSounds(
                R.raw.record_start,
                R.raw.record_finished,
                0
        );

        recordButton.setListenForRecord(PermissionUtils.checkPermissionForAudioRecord(getContext()));
        recordView.setOnRecordListener(new OnRecordListener() {
            @Override
            public void onStart() {
                AppAnalytics.create(AnalyticsEvent.AUDIO_BUTTON_CLICKED.getNAME()).push();
                startRecord();
                AppAnalytics.create(AnalyticsEvent.AUDIO_RECORD.getNAME()).push();
            }

            @Override
            public void onCancel() {
                stopRecord(true);
            }

            @Override
            public void onFinish(long recordTime) {
                try {
                    AppAnalytics.create(AnalyticsEvent.AUDIO_SENT.getNAME()).push();
                    hideRecordView();
                    stopRecord(false);
                    sendVoiceNote();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onLessThanSecond() {
                hideRecordView();
                stopRecord(true);
                AppAnalytics.create(AnalyticsEvent.AUDIO_CANCELLED.getNAME()).push();
            }
        });

        recordView.setOnBasketAnimationEndListener(() -> {
            hideRecordView();
            stopRecord(true);
            AppAnalytics.create(AnalyticsEvent.AUDIO_CANCELLED.getNAME()).push();
        });

        etComposeBox.addTextChangedListener(textWatcher);

        sendButton.setOnClickListener(v -> {
            composeActionListener.onSendActionClicked(etComposeBox);
            etComposeBox.setText("");
        });
        recordButton.setOnRecordClickListener(v -> {
            composeActionListener.onSendActionClicked(etComposeBox);
            listenTextChange = false;
            etComposeBox.setText("");
            listenTextChange = true;
        });

    }

    public void setColor(int color) {
        ivCamera.setImageTintList(ColorStateList.valueOf(color));
        ivGallery.setImageTintList(ColorStateList.valueOf(color));
        ivFile.setImageTintList(ColorStateList.valueOf(color));
        ivArrow.setImageTintList(ColorStateList.valueOf(color));
    }

    public void setComposeBoxListener(ComposeActionListener composeActionListener) {
        this.composeActionListener = composeActionListener;
        this.composeActionListener.getCameraActionView(ivCamera);
        this.composeActionListener.getGalleryActionView(ivGallery);
        this.composeActionListener.getFileActionView(ivFile);
    }

    public void setMediaRecorder(MediaRecorder mediaRecorder) {

//        voiceRipple.setMediaRecorder(mediaRecorder);
//        voiceRipple.setOutputFile(mediaRecorder.setOutgetAudioSourceMax());
//        voiceRipple.setAudioSource(MediaRecorder.AudioSource.MIC);
//        voiceRipple.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        voiceRipple.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    }

    @Override
    public void onClick(View view) {
//        if (view.getId() == R.id.ivDelete) {
//            stopRecording(true);
//            stopPlayingAudio();
//            //voiceMessageLayout.setVisibility(GONE);
//            etComposeBox.setVisibility(View.VISIBLE);
//            flBox.setVisibility(View.VISIBLE);
////            ivMic.setVisibility(View.VISIBLE);
////            ivMic.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_white_24dp));
//            isPlaying = false;
//            isRecording = false;
//            voiceMessage = false;
//            //ivDelete.setVisibility(GONE);
////            ivSend.setVisibility(View.GONE);
//        }
//        if (view.getId() == R.id.ivSend) {
//            if (!voiceMessage) {
//                composeActionListener.onSendActionClicked(etComposeBox);
//            } else {
//                sendVoiceNote();
//            }
//
//        }
        if (view.getId() == R.id.ivArrow) {
//            if (isOpen) {
//               closeActionContainer();
//            } else {
//                openActionContainer();
//            }
            FragmentManager fm = ((AppCompatActivity) getContext()).getSupportFragmentManager();
            bundle.putBoolean("isGalleryVisible", isGalleryVisible);
            bundle.putBoolean("isCameraVisible", isCameraVisible);
            bundle.putBoolean("isFileVisible", isFileVisible);
            bundle.putBoolean("isAudioVisible", isAudioVisible);
            bundle.putBoolean("isLocationVisible", isLocationVisible);
            if (CometChat.isExtensionEnabled("polls"))
                bundle.putBoolean("isPollsVisible", isPollVisible);
            composeBoxActionFragment.setArguments(bundle);
            composeBoxActionFragment.show(fm, composeBoxActionFragment.getTag());
        }
//        if (view.getId() == R.id.ivMic) {
//            if (Utils.hasPermissions(context, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//
//                if (isOpen) {
////                    closeActionContainer();
//                }
//                if (!isRecording) {
//                    startRecord();
//                    ivMic.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop_24dp));
//                    isRecording = true;
//                    isPlaying = false;
//                } else {
//                    if (isRecording && !isPlaying) {
//                        isPlaying = true;
//                        stopRecording(false);
//                        //recordTime.stop();
//                    }
//                    ivMic.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));
//                    ivMic.setVisibility(View.GONE);
//                    ivSend.setVisibility(View.VISIBLE);
//                    //ivDelete.setVisibility(View.VISIBLE);
//                    //voiceSeekbar.setVisibility(View.VISIBLE);
//                    voiceMessage = true;
//                    if (audioFileNameWithPath != null)
//                        startPlayingAudio(audioFileNameWithPath);
//                    else
//                        Toast.makeText(getContext(), "No File Found. Please", Toast.LENGTH_LONG).show();
//                }
//            } else {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    ((Activity) context).requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                            StringContract.RequestCode.RECORD);
//                }
//            }
//        }
    }

    public void usedIn(String className) {
        bundle.putString("type", className);
    }

//    public void openActionContainer() {
//        ivArrow.setRotation(45f);
//        isOpen = true;
//        Animation rightAnimate = AnimationUtils.loadAnimation(getContext(), R.anim.animate_right_slide);
//        rlActionContainer.startAnimation(rightAnimate);
//        rlActionContainer.setVisibility(View.VISIBLE);
//    }
//
//    public void closeActionContainer() {
//        ivArrow.setRotation(0);
//        isOpen = false;
//        Animation leftAnim = AnimationUtils.loadAnimation(getContext(), R.anim.animate_left_slide);
//        rlActionContainer.startAnimation(leftAnim);
//        rlActionContainer.setVisibility(GONE);
//    }

    public void startRecord() {
        showRecordView();
        startRecording();
    }

    public void stopRecord(boolean isCancel) {
        stopRecording(isCancel);
    }

//    private void startPlayingAudio(String path) {
//        try {
//
//            if (timerRunnable != null) {
//                seekHandler.removeCallbacks(timerRunnable);
//                timerRunnable = null;
//            }
//
//            mediaPlayer.reset();
//            if (Utils.hasPermissions(context, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                mediaPlayer.setDataSource(path);
//                mediaPlayer.prepare();
//                mediaPlayer.start();
//            } else {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    ((Activity) context).requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
//                            StringContract.RequestCode.READ_STORAGE);
//                } else {
//                    Toast.makeText(context, "Permission Denied!", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            final int duration = mediaPlayer.getDuration();
////            voiceSeekbar.setMax(duration);
////            recordTime.setBase(SystemClock.elapsedRealtime());
////            recordTime.start();
//            timerRunnable = new Runnable() {
//                @Override
//                public void run() {
//                    int pos = mediaPlayer.getCurrentPosition();
////                    voiceSeekbar.setProgress(pos);
//
//                    if (mediaPlayer.isPlaying() && pos < duration) {
////                        audioLength.setText(Utils.convertTimeStampToDurationTime(player.getCurrentPosition()));
//                        seekHandler.postDelayed(this, 100);
//                    } else {
//                        seekHandler
//                                .removeCallbacks(timerRunnable);
//                        timerRunnable = null;
//                    }
//                }
//
//            };
//            seekHandler.postDelayed(timerRunnable, 100);
//            mediaPlayer.setOnCompletionListener(mp -> {
//                seekHandler
//                        .removeCallbacks(timerRunnable);
//                timerRunnable = null;
//                mp.stop();
//                //recordTime.stop();
////                audioLength.setText(Utils.convertTimeStampToDurationTime(duration));
//                //voiceSeekbar.setProgress(0);
////                playButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
//            });
//
//        } catch (Exception e) {
//            Log.e("playAudioError: ", e.getMessage());
//            stopPlayingAudio();
//        }
//    }
//
//
//    private void stopPlayingAudio() {
//        if (mediaPlayer != null)
//            mediaPlayer.stop();
//    }

    private void startRecording() {
        try {
            mediaRecorder = new MediaRecorder();
            audioFileNameWithPath = Utils.getOutputMediaFile(getContext());
//            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//            mediaRecorder.setOutputFile(audioFileNameWithPath);
            if (recordButton.isRecording()) {
                recordButton.stopRecording();
            } else {
                try {
//                        startRecord();

                    recordButton.setMediaRecorder(new MediaRecorder());
                    recordButton.setOutputFile(audioFileNameWithPath);
                    recordButton.setAudioSource(MediaRecorder.AudioSource.MIC);
                    recordButton.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    recordButton.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

                    recordButton.startRecording();
                } catch (Exception e) {
                    Log.e(TAG, "startRecording() error: ", e);
                }
            }

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

    public void stopRecording(boolean isCancel) {
        try {
            recordButton.stopRecording();
            recordButton.reset();
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

    public void sendVoiceNote() {
        JSONObject metadata = new JSONObject();
        try {
            metadata.put("audioDurationInMs", recordView.getCounterTimeInMs());
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        composeActionListener.onVoiceNoteComplete(audioFileNameWithPath, metadata);
        audioFileNameWithPath = "";
        hideRecordView();
    }

    private void showRecordView() {
        recordView.setVisibility(VISIBLE);
    }

    private void hideRecordView() {
        recordView.setVisibility(GONE);
    }

}
