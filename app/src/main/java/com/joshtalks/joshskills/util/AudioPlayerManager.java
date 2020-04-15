package com.joshtalks.joshskills.util;

import android.app.Service;
import android.content.Context;
import android.net.Uri;

import androidx.work.impl.foreground.SystemForegroundDispatcher;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class AudioPlayerManager {
    volatile private static AudioPlayerManager manager;
    volatile private static SimpleExoPlayer exoPlayer;
    volatile private static Context context;
    volatile private static String AUDIO_TAG = "";
    volatile private static String LAST_ID = "";

    private volatile static List<WeakReference<ExoPlayer.EventListener>> weakReferenceArrayList = new ArrayList<>();


    public static AudioPlayerManager getInstance(Context context) {
        if (manager == null) {
            manager = new AudioPlayerManager();
            TrackSelector trackSelector = new DefaultTrackSelector();
            exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
            AudioPlayerManager.context = context;


        }
        return manager;
    }


    public void play(Uri uri, ExoPlayer.EventListener eventListener, String audioId) {
        if (uri == null) {
            return;
        }

        if (getAudioTag(uri).equalsIgnoreCase(AUDIO_TAG)) {
            if (exoPlayer.getPlaybackState() == Player.STATE_READY && exoPlayer.getPlayWhenReady()) {
                exoPlayer.setPlayWhenReady(false);
                return;
            } else if (exoPlayer.getPlaybackState() == Player.STATE_ENDED && exoPlayer.getPlayWhenReady()) {
                exoPlayer.setPlayWhenReady(false);
                exoPlayer.seekTo(0);
                // EngagementNetworkHelper.engageAudioApi(new AudioEngage(new ArrayList<>(), audioId, exoPlayer.getDuration()));
                return;
            } else {

            }
        } else {
            if (LAST_ID != null) {
                // EngagementNetworkHelper.engageAudioApi(new AudioEngage(new ArrayList<>(), LAST_ID, exoPlayer.getCurrentPosition()));
            }
            exoPlayer.seekTo(0);
            exoPlayer.setPlayWhenReady(false);
            if (weakReferenceArrayList != null && weakReferenceArrayList.size() > 0) {
                for (WeakReference<ExoPlayer.EventListener> weakReference : weakReferenceArrayList) {
                    weakReference.get().onPlayerStateChanged(false, Player.STATE_ENDED);
                }
            }
            weakReferenceArrayList.clear();
        }
        LAST_ID = audioId;
        weakReferenceArrayList.add(new WeakReference<>(eventListener));
        AUDIO_TAG = uri.getPathSegments().get(uri.getPathSegments().size() - 1);
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "exoplayer2example"), null);
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        MediaSource audioSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null, "TT");
        exoPlayer.addListener(eventListener);
        exoPlayer.prepare(audioSource);
        exoPlayer.setPlayWhenReady(true);
        weakReferenceArrayList.add(new WeakReference<>(eventListener));


    }


    String getAudioTag(Uri uri) {
        return uri.getPathSegments().get(uri.getPathSegments().size() - 1);
    }


    public void pause() {
        exoPlayer.setPlayWhenReady(false);
        if (getDuration() == getCurrentPosition()) {
            seekTo(0);
        }
    }

    public int getDuration() {
        return (int) (exoPlayer.getDuration());
    }

    public void seekTo(int i) {
        exoPlayer.seekTo(i);
    }

    public int getCurrentPosition() {
        return (int) exoPlayer.getCurrentPosition();
    }
}
/*

package com.joshtalks.joshskills;

        import android.app.Notification;
        import android.app.NotificationManager;
        import android.app.Service;
        import android.content.Context;
        import android.content.Intent;
        import android.content.res.Configuration;
        import android.os.Build;
        import android.os.Handler;
        import android.os.IBinder;
        import android.os.Looper;

        import androidx.annotation.MainThread;
        import androidx.annotation.NonNull;
        import androidx.annotation.Nullable;
        import androidx.lifecycle.Lifecycle;
        import androidx.lifecycle.LifecycleService;
        import androidx.work.Logger;
        import androidx.work.impl.foreground.SystemForegroundDispatcher;

        import java.io.FileDescriptor;
        import java.io.PrintWriter;

public class Temp extends LifecycleService {

    private static final String TAG ="";

    @Nullable
    private static Temp sForegroundService = null;

    private Handler mHandler;
    private boolean mIsShutdown;
    // Synthetic access
    NotificationManager mNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        sForegroundService = this;
        initializeDispatcher();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (mIsShutdown) {

            // Create a new dispatcher to setup a new lifecycle.
            initializeDispatcher();
            // Set mIsShutdown to false, to correctly accept new commands.
            mIsShutdown = false;
        }

        if (intent != null) {

        }

        // If the service were to crash, we want all unacknowledged Intents to get redelivered.
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @MainThread
    private void initializeDispatcher() {
        mHandler = new Handler(Looper.getMainLooper());
        mNotificationManager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

    }

    */
/**
 * Stops the foreground {@link Service} by asking {@link SystemForegroundDispatcher} to
 * handle a stop request.
 *
 * @return The current instance of {@link androidx.work.impl.foreground.SystemForegroundService}.
 *//*

    public void stopForegroundService() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    public Temp() {
        super();
    }

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return super.getLifecycle();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(fd, writer, args);
    }

    */
/* @MainThread
     @Override
     public void stop() {
         mIsShutdown = true;
         Logger.get().debug(TAG, "All commands completed.");
         // No need to pass in startId; stopSelf() translates to stopSelf(-1) which is a hard stop
         // of all startCommands. This is the behavior we want.
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             stopForeground(true);
         }
         sForegroundService = null;
         stopSelf();
     }
 *//*

    public void startForeground(
            final int notificationId,
            final int notificationType,
            @NonNull final Notification notification) {

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(notificationId, notification, notificationType);
                } else {
                    startForeground(notificationId, notification);
                }
            }
        });
    }

   */
/* @Override
    public void notify(final int notificationId, @NonNull final Notification notification) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mNotificationManager.notify(notificationId, notification);
            }
        });
    }

    @Override
    public void cancelNotification(final int notificationId) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mNotificationManager.cancel(notificationId);
            }
        });
    }*//*


 */
/**
 * @return The current instance of {@link androidx.work.impl.foreground.SystemForegroundService}.
 *//*

    @Nullable
    public static Temp getInstance() {
        return sForegroundService;
    }
}
*/
