package com.joshtalks.appcamera.pix;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fxn.pixeditor.EditOptions;
import com.fxn.pixeditor.PixEditor;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.iammert.library.cameravideobuttonlib.CameraVideoButton;
import com.joshtalks.appcamera.R;
import com.joshtalks.appcamera.VideoTrimmerActivity;
import com.joshtalks.appcamera.adapters.InstantImageAdapter;
import com.joshtalks.appcamera.adapters.MainImageAdapter;
import com.joshtalks.appcamera.interfaces.OnSelectionListener;
import com.joshtalks.appcamera.interfaces.WorkFinish;
import com.joshtalks.appcamera.modals.Img;
import com.joshtalks.appcamera.utility.Constants;
import com.joshtalks.appcamera.utility.HeaderItemDecoration;
import com.joshtalks.appcamera.utility.ImageFetcher;
import com.joshtalks.appcamera.utility.PermUtil;
import com.joshtalks.appcamera.utility.Utility;
import com.joshtalks.appcamera.utility.ui.FastScrollStateChangeListener;
import com.wonderkiln.camerakit.CameraKit;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventCallback;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import org.jetbrains.annotations.NotNull;

public class JoshCameraActivity extends AppCompatActivity implements View.OnTouchListener {

    private static final int sBubbleAnimDuration = 1000;
    private static final int sScrollbarHideDelay = 1000;
    private static final int IMAGE_SELECT = 124;
    private static final int VIDEO_SELECT = 125;


    private static final String OPTIONS = "options";
    private static final int sTrackSnapRange = 5;
    public static String IMAGE_RESULTS = "image_results";
    public static String VIDEO_RESULTS = "video_results";

    public static float TOPBAR_HEIGHT;
    CameraView cameraView;
    boolean camAvail = true;
    Handler handler2 = new Handler();
    private int BottomBarHeight = 0;
    private int colorPrimaryDark;
    private Timer timer = new Timer();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {

            // Log.e("start","foto apparat-----------------------------------------------------------------");
            if (camAvail) {
                cameraView.start();
            }
        }
    };
    private float zoom = 0.0f;
    private float dist = 0.0f;
    private Handler handler = new Handler();
    private FastScrollStateChangeListener mFastScrollStateChangeListener;
    private RecyclerView recyclerView, instantRecyclerView;
    private BottomSheetBehavior mBottomSheetBehavior;
    private InstantImageAdapter initaliseadapter;
    private View status_bar_bg, mScrollbar, topbar, bottomButtons, sendButton;
    private TextView mBubbleView, img_count;
    private ImageView mHandleView, selection_back, selection_check;
    private ViewPropertyAnimator mScrollbarAnimator;
    private ViewPropertyAnimator mBubbleAnimator;
    private Set<Img> selectionList = new HashSet<>();
    private Runnable mScrollbarHider = new Runnable() {
        @Override
        public void run() {
            hideScrollbar();
        }
    };
    private MainImageAdapter mainImageAdapter;
    private float mViewHeight;
    private boolean mHideScrollbar = true;
    private boolean LongSelection = false;
    private Options options = null;
    private CameraVideoButton clickme;
    private TextView tv_record_view;
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private int duration = 0;

    private RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (!mHandleView.isSelected() && recyclerView.isEnabled()) {
                setViewPositions(getScrollProportion(recyclerView));
            }
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (recyclerView.isEnabled()) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        handler.removeCallbacks(mScrollbarHider);
                        Utility.cancelAnimation(mScrollbarAnimator);
                        if (!Utility.isViewVisible(mScrollbar) && (recyclerView.computeVerticalScrollRange()
                                - mViewHeight > 0)) {
                            mScrollbarAnimator = Utility.showScrollbar(mScrollbar, JoshCameraActivity.this);
                        }
                        break;
                    case RecyclerView.SCROLL_STATE_IDLE:
                        if (mHideScrollbar && !mHandleView.isSelected()) {
                            handler.postDelayed(mScrollbarHider, sScrollbarHideDelay);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    };
    private TextView selection_count;
    private OnSelectionListener onSelectionListener = new OnSelectionListener() {
        @Override
        public void onClick(Img img, View view, int position) {
            if (LongSelection) {
                if (selectionList.contains(img)) {
                    selectionList.remove(img);
                    initaliseadapter.select(false, position);
                    mainImageAdapter.select(false, position);
                } else {
                    if (options.getCount() <= selectionList.size()) {

                        return;
                    }
                    img.setPosition(position);
                    selectionList.add(img);
                    initaliseadapter.select(true, position);
                    mainImageAdapter.select(true, position);
                }
                if (selectionList.size() == 0) {
                    LongSelection = false;
                    selection_check.setVisibility(View.VISIBLE);
                    DrawableCompat.setTint(selection_back.getDrawable(), colorPrimaryDark);
                    topbar.setBackgroundColor(Color.parseColor("#ffffff"));
                    Animation anim = new ScaleAnimation(
                            1f, 0f, // Start and end values for the X axis scaling
                            1f, 0f, // Start and end values for the Y axis scaling
                            Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                            Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
                    anim.setFillAfter(true); // Needed to keep the result of the animation
                    anim.setDuration(300);
                    anim.setAnimationListener(new Animation.AnimationListener() {

                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            sendButton.setVisibility(View.GONE);
                            sendButton.clearAnimation();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    sendButton.startAnimation(anim);
                }
                //selection_count.setText(getResources().getString(R.string.pix_selected) + " " + selectionList.size());
                img_count.setText(String.valueOf(selectionList.size()));
            } else {
                img.setPosition(position);
                selectionList.add(img);
                returnObjects();
                DrawableCompat.setTint(selection_back.getDrawable(), colorPrimaryDark);
                topbar.setBackgroundColor(Color.parseColor("#ffffff"));
            }
        }

        @Override
        public void onLongClick(Img img, View view, int position) {
            if (options.getCount() > 1) {
                Utility.vibe(JoshCameraActivity.this, 50);
                //Log.e("onLongClick", "onLongClick");
                LongSelection = true;
                if ((selectionList.size() == 0) && (mBottomSheetBehavior.getState()
                        != BottomSheetBehavior.STATE_EXPANDED)) {
                    sendButton.setVisibility(View.VISIBLE);
                    Animation anim = new ScaleAnimation(
                            0f, 1f, // Start and end values for the X axis scaling
                            0f, 1f, // Start and end values for the Y axis scaling
                            Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                            Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
                    anim.setFillAfter(true); // Needed to keep the result of the animation
                    anim.setDuration(300);
                    sendButton.startAnimation(anim);
                }
                if (selectionList.contains(img)) {
                    selectionList.remove(img);
                    initaliseadapter.select(false, position);
                    mainImageAdapter.select(false, position);
                } else {
                    if (options.getCount() <= selectionList.size()) {
                        // Toast.makeText(JoshCameraActivity.this,String.format(getResources().getString(R.string.selection_limiter_pix),selectionList.size()), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    img.setPosition(position);
                    selectionList.add(img);
                    initaliseadapter.select(true, position);
                    mainImageAdapter.select(true, position);
                }
                selection_check.setVisibility(View.GONE);
                topbar.setBackgroundColor(colorPrimaryDark);
                // selection_count.setText(getResources().getString(R.string.pix_selected) + " " + selectionList.size());
                img_count.setText(String.valueOf(selectionList.size()));
                DrawableCompat.setTint(selection_back.getDrawable(), Color.parseColor("#ffffff"));
            }
        }
    };

    private FrameLayout flash;
    private ImageView front;
    private int flashDrawable;
    private View.OnTouchListener onCameraTouchListner = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getPointerCount() > 1) {

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        dist = Utility.getFingerSpacing(event);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float maxZoom = 1f;

                        float newDist = Utility.getFingerSpacing(event);
                        if (newDist > dist) {
                            //zoom in
                            if (zoom < maxZoom) {
                                zoom = zoom + 0.01f;
                            }
                        } else if ((newDist < dist) && (zoom > 0)) {
                            //zoom out
                            zoom = zoom - 0.01f;
                        }
                        dist = newDist;
                        //fotoapparat.setZoom(zoom);
                        break;
                    default:
                        break;
                }
            }
            return false;
        }
    };

    public static void start(final Fragment context, final Options options) {
        PermUtil.checkForCamaraWritePermissions(context, new WorkFinish() {
            @Override
            public void onWorkFinish(Boolean check) {
                Intent i = new Intent(context.getActivity(), JoshCameraActivity.class);
                i.putExtra(OPTIONS, options);
                context.startActivityForResult(i, options.getRequestCode());
            }
        });
    }

    public static void start(Fragment context, int requestCode) {
        start(context, Options.init().setRequestCode(requestCode).setCount(1));
    }

    public static void start(final FragmentActivity context, final Options options) {
        PermUtil.checkForCamaraWritePermissions(context, new WorkFinish() {
            @Override
            public void onWorkFinish(Boolean check) {
                Intent i = new Intent(context, JoshCameraActivity.class);
                i.putExtra(OPTIONS, options);
                context.startActivityForResult(i, options.getRequestCode());
            }
        });
    }

    public static void start(final FragmentActivity context, int requestCode) {
        start(context, Options.init().setRequestCode(requestCode).setCount(1));
    }

    private void hideScrollbar() {
        float transX = getResources().getDimensionPixelSize(R.dimen.fastscroll_scrollbar_padding_end);
        mScrollbarAnimator = mScrollbar.animate().translationX(transX).alpha(0f)
                .setDuration(Constants.sScrollbarAnimDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mScrollbar.setVisibility(View.GONE);
                        mScrollbarAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        mScrollbar.setVisibility(View.GONE);
                        mScrollbarAnimator = null;
                    }
                });
    }

    public void returnObjects() {
        ArrayList<String> list = new ArrayList<>();
        for (Img i : selectionList) {
            list.add(i.getUrl());
            // Log.e("JoshCameraActivity images", "img " + i.getUrl());
        }

        EditOptions editoptions = EditOptions.init();
        editoptions.setRequestCode(IMAGE_SELECT);
        editoptions.setSelectedlist(list);
        PixEditor.start(this, editoptions);
        selectionList.clear();

        //
        //  Intent resultIntent = new Intent();
        //    resultIntent.putStringArrayListExtra(IMAGE_RESULTS, list);
        //    setResult(Activity.RESULT_OK, resultIntent);
        //
        //  finish();
    }

    public void startTrimmer() {


        //IMAGE_SELECT

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utility.setupStatusBarHidden(this);
        Utility.hideStatusBar(this);
        setContentView(R.layout.activity_main_lib);
        initialize();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        handler2.postDelayed(runnable, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler2.postDelayed(runnable, 0);
    }

    @Override
    protected void onPause() {
        tv_record_view.setText("00:00");
        tv_record_view.setVisibility(View.GONE);
        flash.setVisibility(View.VISIBLE);
        front.setVisibility(View.VISIBLE);
        topbar.setVisibility(View.VISIBLE);
        instantRecyclerView.setVisibility(View.VISIBLE);
        cameraView.stop();
        if (timer!=null) {
            timer.cancel();
        }


        super.onPause();
    }

    private void initialize() {
        Utility.getScreenSize(this);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        try {
            options = (Options) getIntent().getSerializableExtra(OPTIONS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setRequestedOrientation(options.getScreenOrientation());
        colorPrimaryDark =
                ResourcesCompat.getColor(getResources(), R.color.colorPrimaryPix, getTheme());
        cameraView = findViewById(R.id.camera_view);
        cameraView.setFacing(CameraKit.Constants.FACING_BACK);
        cameraView.setVideoQuality(CameraKit.Constants.VIDEO_QUALITY_480P);
        cameraView.setVideoBitRate(CameraKit.Constants.VIDEO_QUALITY_480P);
        //  cameraView.setJpegQuality(80);


        zoom = 0.0f;
        handler2.postDelayed(runnable, 0);
        tv_record_view = findViewById(R.id.tv_record_view);
        clickme = findViewById(R.id.clickme);
        clickme.setVideoDuration(2 * 60 * 1000);
        clickme.enableVideoRecording(true);
        clickme.enablePhotoTaking(true);
        flash = findViewById(R.id.flash);
        front = findViewById(R.id.front);
        topbar = findViewById(R.id.topbar);
        selection_count = findViewById(R.id.selection_count);
        selection_back = findViewById(R.id.selection_back);
        selection_check = findViewById(R.id.selection_check);
        selection_check.setVisibility((options.getCount() > 1) ? View.VISIBLE : View.GONE);
        sendButton = findViewById(R.id.sendButton);
        img_count = findViewById(R.id.img_count);
        mBubbleView = findViewById(R.id.fastscroll_bubble);
        mHandleView = findViewById(R.id.fastscroll_handle);
        mScrollbar = findViewById(R.id.fastscroll_scrollbar);
        mScrollbar.setVisibility(View.GONE);
        mBubbleView.setVisibility(View.GONE);
        bottomButtons = findViewById(R.id.bottomButtons);
        TOPBAR_HEIGHT = Utility.convertDpToPixel(56, JoshCameraActivity.this);
        status_bar_bg = findViewById(R.id.status_bar_bg);
        instantRecyclerView = findViewById(R.id.instantRecyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        instantRecyclerView.setLayoutManager(linearLayoutManager);
        initaliseadapter = new InstantImageAdapter(this);
        initaliseadapter.addOnSelectionListener(onSelectionListener);
        instantRecyclerView.setAdapter(initaliseadapter);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.addOnScrollListener(mScrollListener);
        FrameLayout mainFrameLayout = findViewById(R.id.mainFrameLayout);
        BottomBarHeight = Utility.getSoftButtonsBarSizePort(this);
        FrameLayout.LayoutParams lp =
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(0, 0, 0, BottomBarHeight);
        mainFrameLayout.setLayoutParams(lp);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) sendButton.getLayoutParams();
        layoutParams.setMargins(0, 0, (int) (Utility.convertDpToPixel(16, this)),
                (int) (Utility.convertDpToPixel(174, this)));
        sendButton.setLayoutParams(layoutParams);
        mainImageAdapter = new MainImageAdapter(this);
        GridLayoutManager mLayoutManager = new GridLayoutManager(this, MainImageAdapter.SPAN_COUNT);
        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (mainImageAdapter.getItemViewType(position) == MainImageAdapter.HEADER) {
                    return MainImageAdapter.SPAN_COUNT;
                }
                return 1;
            }
        });
        recyclerView.setLayoutManager(mLayoutManager);
        mainImageAdapter.addOnSelectionListener(onSelectionListener);
        recyclerView.setAdapter(mainImageAdapter);
        recyclerView.addItemDecoration(new HeaderItemDecoration(this, mainImageAdapter));
        mHandleView.setOnTouchListener(this);

        onClickMethods();

        flashDrawable = R.drawable.ic_flash_off_black_24dp;

        if ((options.getPreSelectedUrls().size()) > options.getCount()) {
            int large = options.getPreSelectedUrls().size() - 1;
            int small = options.getCount();
            for (int i = large; i > (small - 1); i--) {
                options.getPreSelectedUrls().remove(i);
            }
        }
        DrawableCompat.setTint(selection_back.getDrawable(), colorPrimaryDark);
        updateImages();


        clickme.setActionListener(new CameraVideoButton.ActionListener() {
            @Override
            public void onStartRecord() {
                cameraView.captureVideo(new CameraKitEventCallback<CameraKitVideo>() {
                    @Override
                    public void callback(CameraKitVideo cameraKitVideo) {
                        Log.e("stopRecord", "stoprecord");

                        File f = new File(Environment.getExternalStorageDirectory().toString() + File.separator + "JoshSkill" + File.separator + "Media" + "/JoshApp/cached");
                        if (!f.exists()) {
                            f.mkdirs();
                        }
                        File file = new File(f.getAbsolutePath() + File.separator + "record.mp4");
                        try {
                            file.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        VideoTrimmerActivity.startTrimmerActivity(JoshCameraActivity.this, VIDEO_SELECT, Uri.fromFile(cameraKitVideo.getVideoFile()), file);

                    }
                });
                flash.animate()
                        .translationY(0)
                        .setDuration(500)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                flash.setVisibility(View.GONE);
                            }
                        });

                front.animate()
                        .translationY(0)
                        .setDuration(500)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                front.setVisibility(View.GONE);
                            }
                        });
                topbar.animate()
                        .translationY(1)
                        .setDuration(500)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                topbar.setVisibility(View.GONE);
                            }
                        });

                instantRecyclerView.animate()
                        .translationY(0)
                        .setDuration(500)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                instantRecyclerView.setVisibility(View.GONE);
                            }
                        });



                if (timer!=null) {
                    timer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    duration += 1000;
                                    tv_record_view.setText(String.format(Locale.getDefault(), "%02d:%02d",
                                            TimeUnit.MILLISECONDS.toMinutes(duration),
                                            TimeUnit.MILLISECONDS.toSeconds(duration)));

                                }
                            });
                        }
                    }, 1000, 1000);
                }
                tv_record_view.setVisibility(View.VISIBLE);


            }

            @Override
            public void onEndRecord() {
                cameraView.stopVideo();
                uiHandler.removeCallbacksAndMessages(null);


            }

            @Override
            public void onDurationTooShortError() {

            }

            @Override
            public void onSingleTap() {
                if (selectionList.size() >= options.getCount()) {
                    //   Toast.makeText(JoshCameraActivity.this,String.format(getResources().getString(R.string.cannot_click_image_pix), "" + options.getCount()), Toast.LENGTH_LONG).show();
                    return;
                }

                final ObjectAnimator oj = ObjectAnimator.ofFloat(cameraView, "alpha", 1f, 0f, 0f, 1f);
                oj.setStartDelay(200l);
                oj.setDuration(800l);
                oj.start();
                Log.e("click time", "--------------------------------");
                cameraView.captureImage(new CameraKitEventCallback<CameraKitImage>() {
                    @Override
                    public void callback(CameraKitImage cameraKitImage) {

                       // Bitmap b = Utility.rotateBitmap(cameraKitImage.getBitmap());
                        Utility.vibe(JoshCameraActivity.this, 50);
                        File photo = Utility.writeImage(cameraKitImage.getBitmap(), options.getPath(), options.getImageQuality(),
                                options.getWidth(), options.getHeight());
                        Img img = new Img("", "", photo.getAbsolutePath(), "");
                        selectionList.add(img);
                        Utility.scanPhoto(JoshCameraActivity.this, photo);
                        //b.recycle();
                       // b = null;
                        Log.e("click time", "--------------------------------2");
                        returnObjects();
                    }
                });

            }
        });
    }

    private void onClickMethods() {

        findViewById(R.id.selection_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Toast.makeText(JoshCameraActivity.this, "fin", Toast.LENGTH_SHORT).show();
                //Log.e("Hello", "onclick");
                returnObjects();
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(JoshCameraActivity.this, "fin", Toast.LENGTH_SHORT).show();
                //Log.e("Hello", "onclick");
                returnObjects();
            }
        });
        selection_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
        selection_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                topbar.setBackgroundColor(colorPrimaryDark);
                selection_count.setText(getResources().getString(R.string.pix_tap_to_select));
                img_count.setText(String.valueOf(selectionList.size()));
                DrawableCompat.setTint(selection_back.getDrawable(), Color.parseColor("#ffffff"));
                LongSelection = true;
                selection_check.setVisibility(View.GONE);
            }
        });
        final ImageView iv = (ImageView) flash.getChildAt(0);
        flash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int height = flash.getHeight();
                iv.animate()
                        .translationY(height)
                        .setDuration(100)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                iv.setTranslationY(-(height / 2));
                                if (flashDrawable == R.drawable.ic_flash_auto_black_24dp) {
                                    flashDrawable = R.drawable.ic_flash_off_black_24dp;
                                    iv.setImageResource(flashDrawable);
                                    cameraView.setFlash(CameraKit.Constants.FLASH_OFF);
                                } else if (flashDrawable == R.drawable.ic_flash_off_black_24dp) {
                                    flashDrawable = R.drawable.ic_flash_on_black_24dp;
                                    iv.setImageResource(flashDrawable);
                                    cameraView.setFlash(CameraKit.Constants.FLASH_TORCH);

                                } else {
                                    flashDrawable = R.drawable.ic_flash_auto_black_24dp;
                                    iv.setImageResource(flashDrawable);
                                    cameraView.setFlash(CameraKit.Constants.FLASH_AUTO);

                                }
                                // fotoapparat.focus();
                                iv.animate().translationY(0).setDuration(50).setListener(null).start();
                            }
                        })
                        .start();
            }
        });

        front.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final ObjectAnimator oa1 = ObjectAnimator.ofFloat(front, "scaleX", 1f, 0f).setDuration(150);
                final ObjectAnimator oa2 = ObjectAnimator.ofFloat(front, "scaleX", 0f, 1f).setDuration(150);
                oa1.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        front.setImageResource(R.drawable.ic_photo_camera);
                        oa2.start();
                    }
                });
                oa1.start();
                if (options.isFrontfacing()) {
                    options.setFrontfacing(false);
                    cameraView.setFacing(CameraKit.Constants.FACING_BACK);
                    flash.setVisibility(View.VISIBLE);

                } else {
                    options.setFrontfacing(true);
                    cameraView.setFacing(CameraKit.Constants.FACING_FRONT);
                    flash.setVisibility(View.GONE);
                }
            }
        });
    }

    private void updateImages() {
        mainImageAdapter.clearList();
        Cursor cursor = Utility.getCursor(JoshCameraActivity.this);
        if (cursor == null) {
            return;
        }
        ArrayList<Img> INSTANTLIST = new ArrayList<>();
        String header = "";
        int limit = 100;
        if (cursor.getCount() < limit) {
            limit = cursor.getCount() - 1;
        }
        int date = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
        int data = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
        int contentUrl = cursor.getColumnIndex(MediaStore.Images.Media._ID);
        Calendar calendar;
        int pos = 0;
        for (int i = 0; i < limit; i++) {
            cursor.moveToNext();
            Uri path = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    "" + cursor.getInt(contentUrl));
            calendar = Calendar.getInstance();
            calendar.setTimeInMillis(cursor.getLong(date));
            String dateDifference = Utility.getDateDifference(JoshCameraActivity.this, calendar);
            if (!header.equalsIgnoreCase("" + dateDifference)) {
                header = "" + dateDifference;
                pos += 1;
                INSTANTLIST.add(new Img("" + dateDifference, "", "", ""));
            }
            Img img = new Img("" + header, "" + path, cursor.getString(data), "" + pos);
            img.setPosition(pos);
            if (options.getPreSelectedUrls().contains(img.getUrl())) {
                img.setSelected(true);
                selectionList.add(img);
            }
            pos += 1;
            INSTANTLIST.add(img);
        }
        if (selectionList.size() > 0) {
            LongSelection = true;
            sendButton.setVisibility(View.VISIBLE);
            Animation anim = new ScaleAnimation(
                    0f, 1f, // Start and end values for the X axis scaling
                    0f, 1f, // Start and end values for the Y axis scaling
                    Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                    Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
            anim.setFillAfter(true); // Needed to keep the result of the animation
            anim.setDuration(300);
            sendButton.startAnimation(anim);
            selection_check.setVisibility(View.GONE);
            topbar.setBackgroundColor(colorPrimaryDark);
            selection_count.setText(
                    getResources().getString(R.string.pix_selected) + " " + selectionList.size());
            img_count.setText(String.valueOf(selectionList.size()));
            DrawableCompat.setTint(selection_back.getDrawable(), Color.parseColor("#ffffff"));
        }
        mainImageAdapter.addImageList(INSTANTLIST);
        initaliseadapter.addImageList(INSTANTLIST);
        ImageFetcher imageFetcher = new ImageFetcher(JoshCameraActivity.this) {
            @Override
            protected void onPostExecute(ImageFetcher.ModelList imgs) {
                super.onPostExecute(imgs);
                mainImageAdapter.addImageList(imgs.getLIST());
                initaliseadapter.addImageList(imgs.getLIST());
                selectionList.addAll(imgs.getSelection());
                if (selectionList.size() > 0) {
                    LongSelection = true;
                    sendButton.setVisibility(View.VISIBLE);
                    Animation anim = new ScaleAnimation(
                            0f, 1f, // Start and end values for the X axis scaling
                            0f, 1f, // Start and end values for the Y axis scaling
                            Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                            Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
                    anim.setFillAfter(true); // Needed to keep the result of the animation
                    anim.setDuration(300);
                    sendButton.startAnimation(anim);
                    selection_check.setVisibility(View.GONE);
                    topbar.setBackgroundColor(colorPrimaryDark);
                    selection_count.setText(
                            getResources().getString(R.string.pix_selected) + " " + selectionList.size());
                    img_count.setText(String.valueOf(selectionList.size()));
                    DrawableCompat.setTint(selection_back.getDrawable(), Color.parseColor("#ffffff"));
                }
            }
        };
        imageFetcher.setStartingCount(pos);
        imageFetcher.header = header;
        imageFetcher.setPreSelectedUrls(options.getPreSelectedUrls());
        imageFetcher.execute(Utility.getCursor(JoshCameraActivity.this));
        cursor.close();
        setBottomSheetBehavior();
    }

    private void setBottomSheetBehavior() {
        View bottomSheet = findViewById(R.id.bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setPeekHeight((int) (Utility.convertDpToPixel(194, this)));
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                Utility.manipulateVisibility(JoshCameraActivity.this, slideOffset,
                        instantRecyclerView, recyclerView, status_bar_bg,
                        topbar, bottomButtons, sendButton, LongSelection);
                if (slideOffset == 1) {
                    Utility.showScrollbar(mScrollbar, JoshCameraActivity.this);
                    mainImageAdapter.notifyDataSetChanged();
                    mViewHeight = mScrollbar.getMeasuredHeight();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            setViewPositions(getScrollProportion(recyclerView));
                        }
                    });
                    sendButton.setVisibility(View.GONE);
                    //  fotoapparat.stop();
                } else if (slideOffset == 0) {
                    initaliseadapter.notifyDataSetChanged();
                    hideScrollbar();
                    img_count.setText(String.valueOf(selectionList.size()));
                    cameraView.start();
                }
            }
        });
    }

    private void scaleUpAnimation() {
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(clickme, "scaleX", 0f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(clickme, "scaleY", 0f);
        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);
        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.play(scaleDownX).with(scaleDownY);

        scaleDownX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {

                View p = (View) clickme.getParent();
                p.invalidate();
            }
        });
        scaleDown.start();
    }

    private void scaleDownAnimation() {
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(clickme, "scaleX", 1f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(clickme, "scaleY", 1f);
        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);
        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.play(scaleDownX).with(scaleDownY);

        scaleDownX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {

                View p = (View) clickme.getParent();
                p.invalidate();
            }
        });
        scaleDown.start();
    }

    private float getScrollProportion(RecyclerView recyclerView) {
        final int verticalScrollOffset = recyclerView.computeVerticalScrollOffset();
        final int verticalScrollRange = recyclerView.computeVerticalScrollRange();
        final float rangeDiff = verticalScrollRange - mViewHeight;
        float proportion = (float) verticalScrollOffset / (rangeDiff > 0 ? rangeDiff : 1f);
        return mViewHeight * proportion;
    }

    private void setViewPositions(float y) {
        int handleY = Utility.getValueInRange(0, (int) (mViewHeight - mHandleView.getHeight()),
                (int) (y - mHandleView.getHeight() / 2));
        mBubbleView.setY(handleY + Utility.convertDpToPixel((56), JoshCameraActivity.this));
        mHandleView.setY(handleY);
    }

    private void setRecyclerViewPosition(float y) {
        if (recyclerView != null && recyclerView.getAdapter() != null) {
            int itemCount = recyclerView.getAdapter().getItemCount();
            float proportion;

            if (mHandleView.getY() == 0) {
                proportion = 0f;
            } else if (mHandleView.getY() + mHandleView.getHeight() >= mViewHeight - sTrackSnapRange) {
                proportion = 1f;
            } else {
                proportion = y / mViewHeight;
            }

            int scrolledItemCount = Math.round(proportion * itemCount);
            int targetPos = Utility.getValueInRange(0, itemCount - 1, scrolledItemCount);
            recyclerView.getLayoutManager().scrollToPosition(targetPos);

            if (mainImageAdapter != null) {
                String text = mainImageAdapter.getSectionMonthYearText(targetPos);
                mBubbleView.setText(text);
                if (text.equalsIgnoreCase("")) {
                    mBubbleView.setVisibility(View.GONE);
                }
            }
        }
    }

    private void showBubble() {
        if (!Utility.isViewVisible(mBubbleView)) {
            mBubbleView.setVisibility(View.VISIBLE);
            mBubbleView.setAlpha(0f);
            mBubbleAnimator = mBubbleView
                    .animate()
                    .alpha(1f)
                    .setDuration(sBubbleAnimDuration)
                    .setListener(new AnimatorListenerAdapter() {
                        // adapter required for new alpha value to stick
                    });
            mBubbleAnimator.start();
        }
    }

    private void hideBubble() {
        if (Utility.isViewVisible(mBubbleView)) {
            mBubbleAnimator = mBubbleView.animate().alpha(0f)
                    .setDuration(sBubbleAnimDuration)
                    .setListener(new AnimatorListenerAdapter() {

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mBubbleView.setVisibility(View.GONE);
                            mBubbleAnimator = null;
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            super.onAnimationCancel(animation);
                            mBubbleView.setVisibility(View.GONE);
                            mBubbleAnimator = null;
                        }
                    });
            mBubbleAnimator.start();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getX() < mHandleView.getX() - ViewCompat.getPaddingStart(mHandleView)) {
                    return false;
                }
                mHandleView.setSelected(true);
                handler.removeCallbacks(mScrollbarHider);
                Utility.cancelAnimation(mScrollbarAnimator);
                Utility.cancelAnimation(mBubbleAnimator);

                if (!Utility.isViewVisible(mScrollbar) && (recyclerView.computeVerticalScrollRange()
                        - mViewHeight > 0)) {
                    mScrollbarAnimator = Utility.showScrollbar(mScrollbar, JoshCameraActivity.this);
                }

                if (mainImageAdapter != null) {
                    showBubble();
                }

                if (mFastScrollStateChangeListener != null) {
                    mFastScrollStateChangeListener.onFastScrollStart(this);
                }
            case MotionEvent.ACTION_MOVE:
                final float y = event.getRawY();
             /*   String text = mainImageAdapter.getSectionText(recyclerView.getVerticalScrollbarPosition()).trim();
                mBubbleView.setText("hello------>"+text+"<--");
                if (text.equalsIgnoreCase("")) {
                    mBubbleView.setVisibility(View.GONE);
                }
                Log.e("hello"," -->> "+ mBubbleView.getText());*/
                setViewPositions(y - TOPBAR_HEIGHT);
                setRecyclerViewPosition(y);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mHandleView.setSelected(false);
                if (mHideScrollbar) {
                    handler.postDelayed(mScrollbarHider, sScrollbarHideDelay);
                }
                hideBubble();
                if (mFastScrollStateChangeListener != null) {
                    mFastScrollStateChangeListener.onFastScrollStop(this);
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onBackPressed() {

        if (selectionList.size() > 0) {
            for (Img img : selectionList) {
                options.setPreSelectedUrls(new ArrayList<String>());
                mainImageAdapter.getItemList().get(img.getPosition()).setSelected(false);
                mainImageAdapter.notifyItemChanged(img.getPosition());
                initaliseadapter.getItemList().get(img.getPosition()).setSelected(false);
                initaliseadapter.notifyItemChanged(img.getPosition());
            }
            LongSelection = false;
            if (options.getCount() > 1) {
                selection_check.setVisibility(View.VISIBLE);
            }
            DrawableCompat.setTint(selection_back.getDrawable(), colorPrimaryDark);
            topbar.setBackgroundColor(Color.parseColor("#ffffff"));
            Animation anim = new ScaleAnimation(
                    1f, 0f, // Start and end values for the X axis scaling
                    1f, 0f, // Start and end values for the Y axis scaling
                    Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                    Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
            anim.setFillAfter(true); // Needed to keep the result of the animation
            anim.setDuration(300);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    sendButton.setVisibility(View.GONE);
                    sendButton.clearAnimation();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            sendButton.startAnimation(anim);
            selectionList.clear();
        } else if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler2.removeCallbacks(runnable);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {

            if (requestCode == IMAGE_SELECT) {
                final ArrayList<String> returnValue = data.getStringArrayListExtra(IMAGE_RESULTS);
                Intent resultIntent = new Intent();
                resultIntent.putStringArrayListExtra(IMAGE_RESULTS, returnValue);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } else if (requestCode == VIDEO_SELECT) {
                final String returnValue = data.getStringExtra("video_uri");
                Intent resultIntent = new Intent();
                resultIntent.putExtra(VIDEO_RESULTS, returnValue);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        }

    }
}
