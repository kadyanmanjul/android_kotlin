package com.joshtalks.joshskills.ui.groupchat.screens;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.cometchat.pro.constants.CometChatConstants;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.joshtalks.joshskills.R;
import com.joshtalks.joshskills.ui.groupchat.constant.StringContract;
import com.joshtalks.joshskills.ui.groupchat.utils.Utils;

public class CometChatMediaViewActivity extends AppCompatActivity {

    private final String TAG = CometChatMediaViewActivity.class.getName();
    private ImageView imageMessage;
    private VideoView videoMessage;
    private Toolbar toolbar;
    private String senderName;
    private long sentAt;
    private String mediaUrl;
    private String mediaType;
    private int mSize;
    private ImageView playBtn;
    private MediaPlayer mediaPlayer;
    private TextView mediaSize;
    private RelativeLayout audioMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comet_chat_media_view);
        handleIntent();
        mediaPlayer = new MediaPlayer();
        toolbar = findViewById(R.id.toolbar);
        toolbar.getNavigationIcon().setTint(getResources().getColor(R.color.white));
        toolbar.setTitle(senderName);
        toolbar.setSubtitle(Utils.getMessageDate(sentAt));
        imageMessage = findViewById(R.id.image_message);
        videoMessage = findViewById(R.id.video_message);
        audioMessage = findViewById(R.id.audio_message);
        mediaSize = findViewById(R.id.media_size_tv);
        playBtn = findViewById(R.id.playBtn);
        if (mediaType.equals(CometChatConstants.MESSAGE_TYPE_IMAGE)) {
            Glide.with(this).load(mediaUrl).into(imageMessage);
            imageMessage.setVisibility(View.VISIBLE);
        } else if (mediaType.equals(CometChatConstants.MESSAGE_TYPE_VIDEO)) {
            MediaController mediacontroller = new MediaController(this);
            mediacontroller.setAnchorView(videoMessage);
            videoMessage.setMediaController(mediacontroller);
            videoMessage.setVideoURI(Uri.parse(mediaUrl));
            videoMessage.setVisibility(View.VISIBLE);
        } else if (mediaType.equals(CometChatConstants.MESSAGE_TYPE_AUDIO)) {
            inttPlayer();

            mediaPlayer.reset();
            mediaSize.setText(Utils.getFileSize(mSize));
            playBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        mediaPlayer.setDataSource(mediaUrl);
                        mediaPlayer.prepare();
                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                playBtn.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "MediaPlayerError: " + e.getMessage());
                    }
                    if (!mediaPlayer.isPlaying()) {
                        mediaPlayer.start();
                        playBtn.setImageResource(R.drawable.ic_pause);
                    } else {
                        mediaPlayer.pause();
                        playBtn.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                    }
                }
            });
            audioMessage.setVisibility(View.VISIBLE);
        }
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void inttPlayer() {
        SimpleExoPlayer player = new SimpleExoPlayer.Builder(this).build();
        PlayerView pp = findViewById(R.id.video_view);
        pp.setPlayer(player);
        player.setPlayWhenReady(true);
        player.seekTo(0, 0);
        Uri uri = Uri.parse("https://data-us.cometchat.io/25039c54194d899/media/1606377435_1183340688_f3dadc7cf88c858b3ee1c8c281331ff7.mp3");
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(
                this, Util.getUserAgent(this, "joshskills"));
        ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri);
        player.prepare(mediaSource);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleIntent() {
        if (getIntent().hasExtra(StringContract.IntentStrings.MEDIA_SIZE))
            mSize = getIntent().getIntExtra(StringContract.IntentStrings.MEDIA_SIZE, 0);
        if (getIntent().hasExtra(StringContract.IntentStrings.NAME))
            senderName = getIntent().getStringExtra(StringContract.IntentStrings.NAME);
        if (getIntent().hasExtra(StringContract.IntentStrings.SENTAT))
            sentAt = getIntent().getLongExtra(StringContract.IntentStrings.SENTAT, 0);
        if (getIntent().hasExtra(StringContract.IntentStrings.INTENT_MEDIA_MESSAGE))
            mediaUrl = getIntent().getStringExtra(StringContract.IntentStrings.INTENT_MEDIA_MESSAGE);
        if (getIntent().hasExtra(StringContract.IntentStrings.MESSAGE_TYPE))
            mediaType = getIntent().getStringExtra(StringContract.IntentStrings.MESSAGE_TYPE);
    }
}