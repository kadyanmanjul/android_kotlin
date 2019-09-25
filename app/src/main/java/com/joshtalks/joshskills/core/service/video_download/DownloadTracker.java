
package com.joshtalks.joshskills.core.service.video_download;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;
/*

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.offline.DownloadCursor;
import com.google.android.exoplayer2.offline.DownloadHelper;
import com.google.android.exoplayer2.offline.DownloadIndex;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloadRequest;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.offline.StreamKey;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

*/
/**
 * Tracks media that has been downloaded.
 *//*

public class DownloadTracker {

    */
/**
     * Listens for changes in the tracked downloads.
     *//*

    public interface Listener {

        */
/**
         * Called when the tracked downloads changed.
         *//*

        void onDownloadsChanged(Download download);

        void onDownloadRemoved(Download download);
    }

    private static final String TAG = "DownloadTracker";

    private final Context context;
    private final DataSource.Factory dataSourceFactory;
    private final CopyOnWriteArraySet<Listener> listeners;
    private final HashMap<Uri, Download> downloads;
    private final DownloadIndex downloadIndex;

    public List<StreamKey> getOfflineStreamKeys(Uri uri) {
        if (!downloads.containsKey(uri)) {
            return Collections.emptyList();
        }
        return Objects.requireNonNull(downloads.get(uri)).request.streamKeys;
    }


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

    @SuppressWarnings("unchecked")
    DownloadRequest getDownloadRequest(Uri uri) {
        Download download = downloads.get(uri);
        return download != null && download.state != Download.STATE_FAILED ? download.request : null;
    }

    public void toggleDownload(
            String tag,
            String name,
            Uri uri,
            String extension,
            RenderersFactory renderersFactory) {
       */
/* DownloadRequest downloadRequest = getDownloadRequest(uri);
        if (downloadRequest != null) {
            if (downloads.containsKey(downloadRequest.uri)) {
                VideoDownloadController.getInstance().getDownloadManager().resumeDownloads();
                return;
            }
        }*//*

        if (startDownloadDialogHelper != null) {
            startDownloadDialogHelper.release();
        }
        startDownloadDialogHelper = new StartDownloadDialogHelper(getDownloadHelper(uri, extension, renderersFactory), name, tag);
    }

    private void loadDownloads() {
        try (DownloadCursor loadedDownloads = downloadIndex.getDownloads()) {
            while (loadedDownloads.moveToNext()) {
                Download download = loadedDownloads.getDownload();
                downloads.put(download.request.uri, download);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to query downloads", e);
        }
    }

    private DownloadHelper getDownloadHelper(
            Uri uri, String extension, RenderersFactory renderersFactory) {
        int type = Util.inferContentType(uri, extension);
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

    private final class StartDownloadDialogHelper
            implements DownloadHelper.Callback {

        private final DownloadHelper downloadHelper;
        private final String name;
        private final String tag;

        StartDownloadDialogHelper(DownloadHelper downloadHelper, String name, String tag) {
            this.downloadHelper = downloadHelper;
            this.name = name;
            this.tag = tag;
            downloadHelper.prepare(this);
        }

        public void release() {
            downloadHelper.release();
        }

        // DownloadHelper.Callback implementation.

        @Override
        public void onPrepared(DownloadHelper helper) {
            if (helper.getPeriodCount() == 0) {
                Log.e(TAG, "No periods found. Downloading entire stream.");
                startDownload();
                downloadHelper.release();
                return;
            }

            */
/* periodIndex= *//*

            MappedTrackInfo mappedTrackInfo = downloadHelper.getMappedTrackInfo(*/
/* periodIndex= *//*
 0);
            for (int periodIndex = 0; periodIndex < downloadHelper.getPeriodCount(); periodIndex++) {
                downloadHelper.clearTrackSelections(periodIndex);
                for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
                    downloadHelper.addTrackSelectionForSingleRenderer(
                            periodIndex,
                            */
/* rendererIndex= *//*
 i,
                            DownloadHelper.DEFAULT_TRACK_SELECTOR_PARAMETERS,
                            Collections.emptyList());

                }
            }
            DownloadRequest downloadRequest = buildDownloadRequest();
            downloadRequest = downloadRequest.copyWithId(tag);
            if (downloadRequest.streamKeys.isEmpty()) {
                // All tracks were deselected in the dialog. Don't start the download.
                return;
            }
            startDownload(downloadRequest);
        }

        @Override
        public void onPrepareError(DownloadHelper helper, IOException e) {

            //Toast.makeText(context.getApplicationContext(), R.string.download_start_error, Toast.LENGTH_LONG) .show();
            Log.e(TAG, "Failed to start download", e);
        }


        private void startDownload() {
            startDownload(buildDownloadRequest());
        }

        private void startDownload(DownloadRequest downloadRequest) {
            DownloadService.sendAddDownload(
                    context, VideoDownloadService.class, downloadRequest, */
/* foreground= *//*
 false);
        }

        private DownloadRequest buildDownloadRequest() {
            return downloadHelper.getDownloadRequest(Util.getUtf8Bytes(name));
        }
    }
}
*/
