package com.joshtalks.joshskills.core.service.downloader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.android.exoplayer2.offline.ActionFile;
import com.google.android.exoplayer2.offline.DownloadAction;
import com.google.android.exoplayer2.offline.DownloadHelper;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.offline.ProgressiveDownloadHelper;
import com.google.android.exoplayer2.offline.StreamKey;
import com.google.android.exoplayer2.offline.TrackKey;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.util.Util;
import com.joshtalks.joshskills.core.service.listeners.DownloadListener;
import com.joshtalks.joshskills.repository.local.entity.ChatModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

public class DownloadTracker implements DownloadManager.Listener {

    private static final String TAG = DownloadTracker.class.getSimpleName();

    private final Context context;
    private final CopyOnWriteArraySet<DownloadListener> listeners;
    private final HashMap<Uri, DownloadAction> trackedDownloadStates;
    private final ActionFile actionFile;
    private final Handler actionFileWriteHandler;

    public DownloadTracker(
            Context context,
            DataSource.Factory dataSourceFactory,
            File actionFile,
            DownloadAction.Deserializer... deserializers) {

        this.context = context.getApplicationContext();
        this.actionFile = new ActionFile(actionFile);
        listeners = new CopyOnWriteArraySet<>();
        trackedDownloadStates = new HashMap<>();

        HandlerThread actionFileWriteThread = new HandlerThread("DownloadTracker");
        actionFileWriteThread.start();
        actionFileWriteHandler = new Handler(actionFileWriteThread.getLooper());
        loadTrackedActions(deserializers.length > 0 ? deserializers : DownloadAction.getDefaultDeserializers());
    }

    public void addListener(DownloadListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DownloadListener listener) {
        listeners.remove(listener);
    }

    public boolean isDownloaded(Uri uri) {
        return trackedDownloadStates.containsKey(uri);
    }

    public List<StreamKey> getOfflineStreamKeys(Uri uri) {
        if (!trackedDownloadStates.containsKey(uri)) {
            return Collections.emptyList();
        }
        return trackedDownloadStates.get(uri).getKeys();
    }


  /*  public void delete(PostCard card) {
        Uri uri = toUri(card.getVideoUrl());

     //   DownloadedPostsDao.getInstance().delete(card);

        if (!isDownloaded(uri)) {
            for (DownloadListener listener : listeners) {
                listener.onDownloadFailed("Video not found!");
            }

            return;
        }

        DownloadAction removeAction = MediaPlayerManager.getInstance().getDownloadHelper(uri)
                .getRemoveAction(Util.getUtf8Bytes(card.toJson()));
        startServiceWithAction(removeAction);

    }*/

    public void download(String url, ChatModel message) {
        Uri uri = toUri(url);
        // DownloadedPostsDao.getInstance().add(card);

        if (isDownloaded(uri)) {

            for (DownloadListener listener : listeners) {
                listener.onDownloadFailed("Video is already in downloads!");
            }
            return;
        }
        StartDownloadDialogHelper helper = new StartDownloadDialogHelper(MediaPlayerManager.getInstance().getDownloadHelper(uri), message);
        helper.prepare();
    }

    private Uri toUri(String url) {
        return Uri.parse(url);
    }

    @Override
    public void onInitialized(DownloadManager downloadManager) {
    }

    @Override
    public void onTaskStateChanged(DownloadManager downloadManager, DownloadManager.TaskState taskState) {
        DownloadAction action = taskState.action;
        Uri uri = action.uri;
        if ((action.isRemoveAction && taskState.state == DownloadManager.TaskState.STATE_COMPLETED)
                || (!action.isRemoveAction && taskState.state == DownloadManager.TaskState.STATE_FAILED)) {
            // A download has been removed, or has failed. Stop tracking it.
            if (trackedDownloadStates.remove(uri) != null) {
                handleTrackedDownloadStatesChanged();
            }
        }
        Log.e("status", "" + taskState.state);
    }

    @Override
    public void onIdle(DownloadManager downloadManager) {

        // Do nothing.
    }

    private void loadTrackedActions(DownloadAction.Deserializer[] deserializers) {
        try {
            DownloadAction[] allActions = actionFile.load(deserializers);
            for (DownloadAction action : allActions) {
                trackedDownloadStates.put(action.uri, action);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void handleTrackedDownloadStatesChanged() {
        for (DownloadListener listener : listeners) {
            listener.onDownloadsChanged();
        }
        final DownloadAction[] actions = trackedDownloadStates.values().toArray(new DownloadAction[0]);
        actionFileWriteHandler.post(
                () -> {
                    try {
                        actionFile.store(actions);
                    } catch (IOException e) {
                        e.printStackTrace();

                    }
                });
    }

    private void startDownload(DownloadAction action) {
        if (trackedDownloadStates.containsKey(action.uri)) {
            return;
        }

        for (DownloadListener listener : listeners) {
            listener.onDownloadStarted();
        }

        trackedDownloadStates.put(action.uri, action);
        handleTrackedDownloadStatesChanged();
        startServiceWithAction(action);
    }

    private void startServiceWithAction(DownloadAction action) {
        DownloadService.startWithAction(context, VideoDownloadService.class, action, true);
    }


    private final class StartDownloadDialogHelper implements DownloadHelper.Callback {

        private final DownloadHelper downloadHelper;
        private TrackKey trackKey;
        private ChatModel message;

        public StartDownloadDialogHelper(
                DownloadHelper downloadHelper, ChatModel message) {
            this.downloadHelper = downloadHelper;
            this.message = message;

        }

        public void prepare() {
            downloadHelper.prepare(this);

        }

        @Override
        public void onPrepared(DownloadHelper helper) {

            int max = -1;

            for (int i = 0; i < downloadHelper.getPeriodCount(); i++) {
                TrackGroupArray trackGroups = downloadHelper.getTrackGroups(i);
                for (int j = 0; j < trackGroups.length; j++) {
                    TrackGroup trackGroup = trackGroups.get(j);
                    for (int k = 0; k < trackGroup.length; k++) {
                        int trackSize = trackGroup.getFormat(k).width * trackGroup.getFormat(k).height;
                        if (trackSize > max) {
                            trackKey = new TrackKey(i, j, k);
                            max = trackSize;
                        }
                    }
                }
            }


            if (trackKey == null && !(downloadHelper instanceof ProgressiveDownloadHelper)) {
                for (DownloadListener listener : listeners) {
                    listener.onDownloadFailed("Failed to start download");
                }
            } else {
                ArrayList<TrackKey> keys = new ArrayList<>();
                keys.add(trackKey);
                DownloadAction downloadAction = downloadHelper.getDownloadAction(Util.getUtf8Bytes(message.toString()), keys);

                startDownload(downloadAction);

            }
            // progressDialog.dismiss();
        }

        @Override
        public void onPrepareError(DownloadHelper helper, IOException e) {
            for (DownloadListener listener : listeners) {
                listener.onDownloadFailed("Failed to start download");
            }
        }

    }
}