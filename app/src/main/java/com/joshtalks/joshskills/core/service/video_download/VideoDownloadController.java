package com.joshtalks.joshskills.core.service.video_download;

import android.annotation.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.database.*;
import com.google.android.exoplayer2.offline.*;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.*;
import com.google.android.exoplayer2.upstream.cache.*;
import com.google.android.exoplayer2.upstream.crypto.*;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;
import com.joshtalks.joshskills.R;
import com.joshtalks.joshskills.core.AppObjectController;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

public class VideoDownloadController {
    private static final String TAG = "ManjulVDC";
    private static final String DOWNLOAD_ACTION_FILE = "actions";
    private static final String DOWNLOAD_TRACKER_ACTION_FILE = "tracked_actions";
    private static final String DOWNLOAD_CONTENT_DIRECTORY = "download_video_lecture";
    private static volatile VideoDownloadController videoDownloadController;
    private final String userAgent;
    private volatile DatabaseProvider databaseProvider;
    private volatile File downloadDirectory;
    private volatile Cache downloadCache;
    private volatile DownloadManager downloadManager;
    private volatile DownloadTracker downloadTracker;

    private VideoDownloadController() {
        userAgent = Util.getUserAgent(AppObjectController.getJoshApplication(), AppObjectController.getJoshApplication().getString(R.string.app_name));
    }

    public static VideoDownloadController getInstance() {
        android.util.Log.d(TAG, "getInstance() called");
        if (videoDownloadController == null) {
            videoDownloadController = new VideoDownloadController();
        }
        return videoDownloadController;
    }

    /**
     * Utility method to create a MediaSource which only contains the tracks defined in {@code
     * downloadRequest}.
     *
     *
     * @param type
     * @param downloadRequest   A {@link DownloadRequest}.
     * @param dataSourceFactory A factory for {@link DataSource}s to read the media.
     * @return A MediaSource which only contains the tracks defined in {@code downloadRequest}.
     */
    public static MediaSource createMediaSource(
            int type, DownloadRequest downloadRequest, DataSource.Factory dataSourceFactory) {
        android.util.Log.d(TAG, "createMediaSource() called with: type = [" + type + "], downloadRequest = [" + downloadRequest + "], dataSourceFactory = [" + dataSourceFactory + "]");
        switch (type) {
            case C.CONTENT_TYPE_DASH:
                return new DashMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(downloadRequest.uri));
            case C.CONTENT_TYPE_SS:
                return new SsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(downloadRequest.uri));
            case C.CONTENT_TYPE_HLS:
                return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(downloadRequest.uri));
            case C.CONTENT_TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(downloadRequest.uri));
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    public String getUserAgent() {
        return userAgent;
    }

    public RenderersFactory buildRenderersFactory(boolean preferExtensionRenderer) {
        @DefaultRenderersFactory.ExtensionRendererMode
        int extensionRendererMode =
                useExtensionRenderers()
                        ? (preferExtensionRenderer
                        ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
        return new DefaultRenderersFactory(
                AppObjectController.getJoshApplication())
                .setExtensionRendererMode(extensionRendererMode);
    }

    private boolean useExtensionRenderers() {
        return false;
    }

    public DownloadManager getDownloadManager() {
        initDownloadManager();
        return downloadManager;
    }

    private synchronized void initDownloadManager() {
        try {
            if (downloadManager == null) {
                android.util.Log.d(TAG, "initDownloadManager() called null");
                DefaultDownloadIndex downloadIndex = new DefaultDownloadIndex(getDatabaseProvider());
                 upgradeActionFile(
                        DOWNLOAD_ACTION_FILE, downloadIndex, false);
                upgradeActionFile(
                        DOWNLOAD_TRACKER_ACTION_FILE, downloadIndex, true);
                downloadManager =
                        new DownloadManager(
                                AppObjectController.getJoshApplication(), downloadIndex, new DefaultDownloaderFactory(buildCacheDataSourceFactory(), Executors.newFixedThreadPool(/* nThreads= */ 6)));
                downloadManager.setMinRetryCount(5);
                downloadManager.setMaxParallelDownloads(10);
                downloadTracker =
                        new DownloadTracker(AppObjectController.getJoshApplication(), buildDataSourceFactory(), downloadManager);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG,"initDownloadManager ex"+ ex.getMessage());
        }
    }

    public CacheDataSource.Factory buildCacheDataSourceFactory() {
        DefaultDataSource.Factory upstreamFactory = new  DefaultDataSource.Factory(AppObjectController.getJoshApplication(), buildHttpDataSourceFactory());
        return new CacheDataSource.Factory()
                .setCache(getDownloadCache())
                .setCacheKeyFactory(CacheKeyFactory.DEFAULT)
                //.setCacheWriteDataSinkFactory(getCacheWriteDataSinkFactory(getDownloadCache()))
                //.setCacheReadDataSourceFactory(getCacheReadDataSourceFactory())
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    private static DataSource.Factory getCacheReadDataSourceFactory() {
        return () -> new AesCipherDataSource(Util.getUtf8Bytes("testKey:12345678"), new FileDataSource());
    }

    private static final int ENCRYPTION_BUFFER_SIZE = 10 * 1024;
    private static DataSink.Factory getCacheWriteDataSinkFactory(final Cache cache) {
        return () -> {
            CacheDataSink cacheSink =
                    new CacheDataSink(cache, CacheDataSink.DEFAULT_FRAGMENT_SIZE, CacheDataSink.DEFAULT_BUFFER_SIZE);
            return new AesCipherDataSink(
                    Util.getUtf8Bytes("testKey:12345678"), cacheSink, new byte[ENCRYPTION_BUFFER_SIZE]);
        };
    }

    private DataSource.Factory buildDataSourceFactory() {
        DefaultDataSource.Factory upstreamFactory = new DefaultDataSource.Factory(AppObjectController.getJoshApplication(), buildHttpDataSourceFactory());
        return buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache());
    }

    private void upgradeActionFile(
            String fileName, DefaultDownloadIndex downloadIndex, boolean addNewDownloadsAsCompleted) {
        try {
            ActionFileUpgradeUtil.upgradeAndDelete(
                    new File(getDownloadDirectory(), fileName),
                    null,
                    downloadIndex,
                    true,
                    addNewDownloadsAsCompleted);
        } catch (IOException e) {
            Log.e(TAG, "Failed to upgrade action file: " + fileName, e);
        }
    }

    protected static CacheDataSource.Factory buildReadOnlyCacheDataSource(
            DataSource.Factory upstreamFactory, Cache cache) {
        return new CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(upstreamFactory).setFlags(
                CacheDataSource.FLAG_BLOCK_ON_CACHE
                        | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory() {
        return new DefaultHttpDataSource.Factory().setUserAgent(userAgent).setAllowCrossProtocolRedirects(true);
    }

    public synchronized Cache getDownloadCache() {
        if (downloadCache == null) {
            File downloadContentDirectory = new File(getDownloadDirectory(), DOWNLOAD_CONTENT_DIRECTORY);
            downloadCache =
                    new SimpleCache(downloadContentDirectory, new NoOpCacheEvictor(), getDatabaseProvider());
        }
        return downloadCache;
    }

    private File getDownloadDirectory() {
        //downloadDirectory= AppDirectory.getVideoDownloadDirectory();
        if (downloadDirectory == null) {
            downloadDirectory = AppObjectController.getJoshApplication().getExternalFilesDir(null);
            if (downloadDirectory == null) {
                downloadDirectory = AppObjectController.getJoshApplication().getFilesDir();
            }
        }
        return downloadDirectory;
    }

    private DatabaseProvider getDatabaseProvider() {
        if (databaseProvider == null) {
            databaseProvider = new StandaloneDatabaseProvider(AppObjectController.getJoshApplication());
        }
        return databaseProvider;
    }


    public synchronized DownloadTracker getDownloadTracker() {
        initDownloadManager();
        return downloadTracker;
    }

    public MediaSource getMediaSource(String url) {
        return getMediaSource(Uri.parse(url));
    }

    public MediaSource getMediaSource(Uri uri) {
        try {
            if (uri == null) {
                return null;
            }
            int type = Util.inferContentType(uri, null);
            DownloadRequest downloadRequest = getDownloadTracker().getDownloadRequest(uri);
            if (downloadRequest != null) {
                android.util.Log.d(TAG, "getMediaSource() called with: uri = [" + uri + "]");
                return createMediaSource(type,downloadRequest, buildDataSourceFactory());
            }
            android.util.Log.d(TAG, "getMediaSource() called with: uri switch = [" + uri + "]" + " switch = [" + type + "]");

            switch (type) {
                case C.CONTENT_TYPE_DASH:
                    return new DashMediaSource.Factory(buildDataSourceFactory()).createMediaSource(MediaItem.fromUri(uri));
                case C.CONTENT_TYPE_SS:
                    return new SsMediaSource.Factory(buildDataSourceFactory()).createMediaSource(MediaItem.fromUri(uri));
                case C.CONTENT_TYPE_HLS:
                    return new HlsMediaSource.Factory(buildDataSourceFactory()).createMediaSource(MediaItem.fromUri(uri));
                case C.CONTENT_TYPE_OTHER:
                    return new ProgressiveMediaSource.Factory(buildDataSourceFactory()).createMediaSource(MediaItem.fromUri(uri));
                default:
                    throw new IllegalStateException("Unsupported type: " + type);
            }
        }catch (Exception ex){
            Log.e(TAG,"getMediaSource ex"+ ex.getMessage());
            return null;
        }
    }

    @SuppressLint("Range")
    public long getDownloadedUrlSize(String url) {
        long size = 0L;
        try {
            SQLiteDatabase db = getDatabaseProvider().getReadableDatabase();
            db.beginTransaction();
            Cursor cursor = db.rawQuery("select * from ExoPlayerDownloads where id='" + url + "'", null);
            if (cursor.moveToFirst()) {
                size = cursor.getLong(cursor.getColumnIndex("bytes_downloaded"));
            }
            cursor.close();
            db.endTransaction();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"getDownloadedUrlSize ex"+ e.getMessage());
        }
        return size;
    }
}
