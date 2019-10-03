
package com.joshtalks.joshskills.core.service.video_download;


import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.offline.DownloadCursor;
import com.google.android.exoplayer2.offline.DownloadHelper;
import com.google.android.exoplayer2.offline.DownloadIndex;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloadRequest;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;
import com.joshtalks.joshskills.core.AppObjectController;
import com.joshtalks.joshskills.repository.local.entity.ChatModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Tracks media that has been downloaded.
 */
public class DownloadTracker {

    public interface Listener {


        void onDownloadsChanged(Download download);

        void onDownloadRemoved(Download download);
    }


    private static final String TAG = "DownloadTracker";

    private final Context context;
    private final DataSource.Factory dataSourceFactory;
    private final CopyOnWriteArraySet<Listener> listeners;
    private final HashMap<Uri, Download> downloads;
    private final DownloadIndex downloadIndex;

    @Nullable
    private StartDownloadDialogHelper startDownloadDialogHelper;

    public DownloadTracker(
            Context context, DataSource.Factory dataSourceFactory, DownloadManager downloadManager) {
        this.context = context.getApplicationContext();
        this.dataSourceFactory = dataSourceFactory;
        listeners = new CopyOnWriteArraySet<>();
        downloads = new HashMap<>();
        downloadIndex = downloadManager.getDownloadIndex();
        downloadManager.addListener(new DownloadManagerListener());
        loadDownloads();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public boolean isDownloaded(Uri uri) {
        Download download = downloads.get(uri);
        return download != null && download.state != Download.STATE_FAILED;
    }

    DownloadRequest getDownloadRequest(Uri uri) {
        Download download = downloads.get(uri);
        return download != null && download.state != Download.STATE_FAILED ? download.request : null;
    }

    public void download(ChatModel chatObj,
                         Uri uri,
                         RenderersFactory renderersFactory) {
        Download download = downloads.get(uri);
        if (download != null) {
            DownloadService.sendRemoveDownload(context, VideoDownloadService.class, download.request.id, /* foreground= */ false);
        } else {
            if (startDownloadDialogHelper != null) {
                startDownloadDialogHelper.release();
            }
            startDownloadDialogHelper = new StartDownloadDialogHelper(getDownloadHelper(uri, renderersFactory), chatObj);
        }
    }

    private void loadDownloads() {
        try (DownloadCursor loadedDownloads = downloadIndex.getDownloads()) {
            while (loadedDownloads.moveToNext()) {
                Download download = loadedDownloads.getDownload();
                downloads.put(download.request.uri, download);
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to query downloads", e);
        }
    }

    private DownloadHelper getDownloadHelper(
            Uri uri, RenderersFactory renderersFactory) {
        int type = Util.inferContentType(uri, null);
        switch (type) {
            case C.TYPE_DASH:
                return DownloadHelper.forDash(uri, dataSourceFactory, renderersFactory);
            case C.TYPE_SS:
                return DownloadHelper.forSmoothStreaming(uri, dataSourceFactory, renderersFactory);
            case C.TYPE_HLS:
                return DownloadHelper.forHls(uri, dataSourceFactory, renderersFactory);
            case C.TYPE_OTHER:
                return DownloadHelper.forProgressive(uri);
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    private class DownloadManagerListener implements DownloadManager.Listener {

        @Override
        public void onDownloadChanged(DownloadManager downloadManager, Download download) {
            Log.e("downloadstate", "" + download.state);
            downloads.put(download.request.uri, download);
            for (Listener listener : listeners) {
                listener.onDownloadsChanged(download);
            }
        }

        @Override
        public void onDownloadRemoved(DownloadManager downloadManager, Download download) {
            downloads.remove(download.request.uri);
            for (Listener listener : listeners) {
                listener.onDownloadRemoved(download);
            }
        }

    }

    private final class StartDownloadDialogHelper implements DownloadHelper.Callback {
        private final DownloadHelper downloadHelper;
        private final ChatModel chatObj;

        public StartDownloadDialogHelper(DownloadHelper downloadHelper, ChatModel chatObj) {
            this.downloadHelper = downloadHelper;
            this.chatObj = chatObj;
            downloadHelper.prepare(this);
        }

        public void release() {
            downloadHelper.release();
        }

        @Override
        public void onPrepared(DownloadHelper helper) {
            if (helper.getPeriodCount() == 0) {
                Log.d(TAG, "No periods found. Downloading entire stream.");
                startDownload();
                downloadHelper.release();
                return;
            }
            MappedTrackInfo mappedTrackInfo = downloadHelper.getMappedTrackInfo(0);
            if (!willHaveContent(mappedTrackInfo)) {
                Log.d(TAG, "No dialog content. Downloading entire stream.");
                startDownload();
                downloadHelper.release();
                return;
            }

            for (int periodIndex = 0; periodIndex < downloadHelper.getPeriodCount(); periodIndex++) {
                downloadHelper.clearTrackSelections(periodIndex);
                for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
                    downloadHelper.addTrackSelectionForSingleRenderer(periodIndex, i, DownloadHelper.DEFAULT_TRACK_SELECTOR_PARAMETERS, getOverrides(i));
                }
            }
            DownloadRequest downloadRequest = buildDownloadRequest();
            if (downloadRequest.streamKeys.isEmpty()) {
                // All tracks were deselected in the dialog. Don't start the download.
                return;
            }
            startDownload(downloadRequest);

        }

        @Override
        public void onPrepareError(DownloadHelper helper, IOException e) {
          /*  Toast.makeText(
                    context.getApplicationContext(), R.string.download_start_error, Toast.LENGTH_LONG)
                    .show();*/
            Log.e(TAG, "Failed to start download", e);
        }

        /**
         * Returns whether a track selection dialog will have content to display if initialized with the
         * specified {@link DefaultTrackSelector} in its current state.
         */
        public boolean willHaveContent(DefaultTrackSelector trackSelector) {
            MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
            return mappedTrackInfo != null && willHaveContent(mappedTrackInfo);
        }

        /**
         * Returns whether a track selection dialog will have content to display if initialized with the
         * specified {@link MappedTrackInfo}.
         */
        boolean willHaveContent(MappedTrackInfo mappedTrackInfo) {
            for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
                if (showTabForRenderer(mappedTrackInfo, i)) {
                    return true;
                }
            }
            return false;
        }

        private boolean showTabForRenderer(MappedTrackInfo mappedTrackInfo, int rendererIndex) {
            TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex);
            if (trackGroupArray.length == 0) {
                return false;
            }
            int trackType = mappedTrackInfo.getRendererType(rendererIndex);
            return isSupportedTrackType(trackType);
        }

        private boolean isSupportedTrackType(int trackType) {
            switch (trackType) {
                case C.TRACK_TYPE_VIDEO:
                case C.TRACK_TYPE_AUDIO:
                case C.TRACK_TYPE_TEXT:
                    return true;
                default:
                    return false;
            }
        }


        List<DefaultTrackSelector.SelectionOverride> getOverrides(int pos) {
            if (pos == 0) {
                List<DefaultTrackSelector.SelectionOverride> overrideList = new ArrayList<>();
                overrideList.add(new DefaultTrackSelector.SelectionOverride(0, 0));
                return overrideList;
            } else {
                return Collections.emptyList();
            }
        }

        private void startDownload() {
            startDownload(buildDownloadRequest());
        }

        private void startDownload(DownloadRequest downloadRequest) {
            DownloadService.sendAddDownload(
                    context, VideoDownloadService.class, downloadRequest, false);
        }

        private DownloadRequest buildDownloadRequest() {
            return downloadHelper.getDownloadRequest(Util.getUtf8Bytes(AppObjectController.getGsonMapper().toJson(chatObj)));
        }
    }
}
