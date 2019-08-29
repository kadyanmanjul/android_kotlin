package com.joshtalks.joshskills;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.emoji.text.EmojiCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.joshtalks.joshskills.emoji.ChatAdapter;
import com.joshtalks.joshskills.emoji.PageTransformer;
import com.joshtalks.recordview.OnBasketAnimationEnd;
import com.joshtalks.recordview.OnRecordListener;
import com.joshtalks.recordview.CustomImageButton;
import com.joshtalks.recordview.RecordView;
import com.vanniktech.emoji.EmojiEditText;
import com.vanniktech.emoji.EmojiPopup;

// We don't care about duplicated code in the sample.
@SuppressWarnings("CPD-START")
public class TestActivity extends AppCompatActivity {
    static final String TAG = "MainActivity";

    ChatAdapter chatAdapter;
    EmojiPopup emojiPopup;

    EmojiEditText editText;
    ViewGroup rootView;
    ImageView emojiButton;
    EmojiCompat emojiCompat;
    private boolean writeText = false;
    CustomImageButton customImageButton;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_test);


        RecordView recordView = (RecordView) findViewById(R.id.record_view);
        customImageButton = (CustomImageButton) findViewById(R.id.record_button);


        //Cancel Bounds is when the Slide To Cancel text gets before the timer . default is 8
        //recordView.setCancelBounds(8);


        //recordView.setSmallMicColor(Color.parseColor("#c2185b"));

        //prevent recording under one Second
        //recordView.setLessThanSecondAllowed(false);


        // recordView.setSlideToCancelText("Slide To Cancel");


        recordView.setCustomSounds(R.raw.record_start, R.raw.record_finished, 0);


        recordView.setOnRecordListener(new OnRecordListener() {
            @Override
            public void onStart() {
                recordView.setVisibility(View.VISIBLE);
                Log.e("RecordView", "onStart");
                // Toast.makeText(MainActivity.this, "OnStartRecord", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onCancel() {
                //  Toast.makeText(MainActivity.this, "onCancel", Toast.LENGTH_SHORT).show();

                Log.e("RecordView", "onCancel");
//          recordView.setVisibility(View.GONE);


            }

            @Override
            public void onFinish(long recordTime) {
                recordView.setVisibility(View.GONE);
            }

            @Override
            public void onLessThanSecond() {
                recordView.setVisibility(View.GONE);
            }
        });


        recordView.setOnBasketAnimationEndListener(new OnBasketAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                Log.d("RecordView", "Basket Animation Finished");
                recordView.setVisibility(View.GONE);
                Log.e("RecordView", "Basket Animation Finished");


            }
        });


        //IMPORTANT
        customImageButton.setRecordView(recordView);


        EmojiEditText editTextt = findViewById(R.id.chat_edit);
        editTextt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.e("what", "beforeTextChanged");

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.e("what", "onTextChanged");

            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.e("what", "afterTextChanged");
                if (s.length() > 0) {
                    if (customImageButton.getState() == CustomImageButton.SECOND_STATE) {
                        return;
                    }
                    customImageButton.goToState(CustomImageButton.SECOND_STATE);

                } else {
                    customImageButton.goToState(CustomImageButton.FIRST_STATE);
                }
            }
        });

        editTextt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP) {

                }
                Log.e("what", "" + event.getAction());


                return false;
            }
        });


        chatAdapter = new ChatAdapter();

        editText = findViewById(R.id.main_activity_chat_bottom_message_edittext);
        rootView = findViewById(R.id.main_activity_root_view);
        emojiButton = findViewById(R.id.main_activity_emoji);
        final ImageView sendButton = findViewById(R.id.main_activity_send);

        emojiButton.setColorFilter(ContextCompat.getColor(this, R.color.emoji_icons), PorterDuff.Mode.SRC_IN);
        sendButton.setColorFilter(ContextCompat.getColor(this, R.color.emoji_icons), PorterDuff.Mode.SRC_IN);

        emojiButton.setOnClickListener(ignore -> emojiPopup.toggle());
        sendButton.setOnClickListener(ignore -> {
            final String text = editText.getText().toString().trim();

            if (text.length() > 0) {
                chatAdapter.add(text);

                editText.setText("");
            }
        });

        final RecyclerView recyclerView = findViewById(R.id.main_activity_recycler_view);
        recyclerView.setAdapter(chatAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

        setUpEmojiPopup();
    }


    @Override
    protected void onStop() {
        if (emojiPopup != null) {
            emojiPopup.dismiss();
        }

        super.onStop();
    }

    private void setUpEmojiPopup() {
        emojiPopup = EmojiPopup.Builder.fromRootView(rootView)
                .setOnEmojiBackspaceClickListener(ignore -> Log.d(TAG, "Clicked on Backspace"))
                .setOnEmojiClickListener((ignore, ignore2) -> Log.d(TAG, "Clicked on emoji"))
                .setOnEmojiPopupShownListener(() -> emojiButton.setImageResource(R.drawable.ic_keyboard))
                .setOnSoftKeyboardOpenListener(ignore -> Log.d(TAG, "Opened soft keyboard"))
                .setOnEmojiPopupDismissListener(() -> emojiButton.setImageResource(R.drawable.happy_face))
                .setOnSoftKeyboardCloseListener(() -> Log.d(TAG, "Closed soft keyboard"))
                .setKeyboardAnimationStyle(R.style.emoji_fade_animation_style)
                .setPageTransformer(new PageTransformer())
                .build(editText);
    }


    void showRecordVisibleButton() {
        customImageButton.goToState(CustomImageButton.SECOND_STATE);

        customImageButton.setListenForRecord(true);
        /*if (findViewById(R.id.record_button).getVisibility() == View.INVISIBLE) {



            new BounceAnimation(findViewById(R.id.send_button)).setNumOfBounces(1)
                    .setDuration(Animation.DURATION_DEFAULT)
                    .setListener(new AnimationListener() {

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            findViewById(R.id.record_button).setVisibility(View.VISIBLE);
                            findViewById(R.id.send_button).setVisibility(View.INVISIBLE);
                        }
                    }).animate();
        }*/
    }

    void showSendVisibleButton() {
        customImageButton.goToState(CustomImageButton.FIRST_STATE);

        customImageButton.setListenForRecord(false);
       /* if (findViewById(R.id.send_button).getVisibility() == View.INVISIBLE) {
            new BounceAnimation(findViewById(R.id.record_button)).setNumOfBounces(1)
                    .setDuration(Animation.DURATION_DEFAULT)
                    .setListener(new AnimationListener() {

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            findViewById(R.id.record_button).setVisibility(View.INVISIBLE);
                            findViewById(R.id.send_button).setVisibility(View.VISIBLE);
                        }
                    }).animate();
        }*/
    }


}
