package com.joshtalks.joshskills.core.service.video_download;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.database.DatabaseProvider;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.offline.ActionFileUpgradeUtil;
import com.google.android.exoplayer2.offline.DefaultDownloadIndex;
import com.google.android.exoplayer2.offline.DefaultDownloaderFactory;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloadRequest;
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper;
import com.google.android.exoplayer2.offline.StreamKey;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;
import com.joshtalks.joshskills.R;
import com.joshtalks.joshskills.core.AppObjectController;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

public class VideoDownloadController {
    private static final String TAG = "DemoApplication";
    private static final String DOWNLOAD_ACTION_FILE = "actions";
    private static final String DOWNLOAD_TRACKER_ACTION_FILE = "tracked_actions";
    private static final String DOWNLOAD_CONTENT_DIRECTORY = "download_video_lecture";
    private static final MediaSourceFactory DASH_FACTORY =
            getMediaSourceFactory("com.google.android.exoplayer2.source.dash.DashMediaSource$Factory");
    private static final MediaSourceFactory SS_FACTORY =
            getMediaSourceFactory(
                    "com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource$Factory");
    private static final MediaSourceFactory HLS_FACTORY =
            getMediaSourceFactory("com.google.android.exoplayer2.source.hls.HlsMediaSource$Factory");
    volatile private static VideoDownloadController videoDownloadController;
    private volatile String userAgent;
    private volatile DatabaseProvider databaseProvider;
    private volatile File downloadDirectory;
    private volatile Cache downloadCache;
    private volatile DownloadManager downloadManager;
    private volatile DownloadTracker downloadTracker;


    private VideoDownloadController() {
        userAgent = Util.getUserAgent(AppObjectController.getJoshApplication(), AppObjectController.getJoshApplication().getString(R.string.app_name));
    }

    public static VideoDownloadController getInstance() {
        if (videoDownloadController == null) {
            videoDownloadController = new VideoDownloadController();
        }
        return videoDownloadController;
    }

    protected static CacheDataSourceFactory buildReadOnlyCacheDataSource(
            DataSource.Factory upstreamFactory, Cache cache) {
        return new CacheDataSourceFactory(
                cache,
                upstreamFactory,
                new FileDataSourceFactory(),
                null,
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                null);
    }

    /**
     * Utility method to create a MediaSource which only contains the tracks defined in {@code
     * downloadRequest}.
     *
     * @param downloadRequest   A {@link DownloadRequest}.
     * @param dataSourceFactory A factory for {@link DataSource}s to read the media.
     * @return A MediaSource which only contains the tracks defined in {@code downloadRequest}.
     */
    public static MediaSource createMediaSource(
            DownloadRequest downloadRequest, DataSource.Factory dataSourceFactory) {
        MediaSourceFactory factory;
        switch (downloadRequest.type) {
            case DownloadRequest.TYPE_DASH:
                factory = DASH_FACTORY;
                break;
            case DownloadRequest.TYPE_SS:
                factory = SS_FACTORY;
                break;
            case DownloadRequest.TYPE_HLS:
                factory = HLS_FACTORY;
                break;
            case DownloadRequest.TYPE_PROGRESSIVE:
                return new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(downloadRequest.uri);
            default:
                throw new IllegalStateException("Unsupported type: " + downloadRequest.type);
        }
        return factory.createMediaSource(
                downloadRequest.uri, dataSourceFactory, downloadRequest.streamKeys);
    }

    private static MediaSourceFactory getMediaSourceFactory(String className) {
        Constructor<?> constructor = null;
        Method setStreamKeysMethod = null;
        Method createMethod = null;
        try {
            // LINT.IfChange
            Class<?> factoryClazz = Class.forName(className);
            constructor = factoryClazz.getConstructor(DataSource.Factory.class);
            setStreamKeysMethod = factoryClazz.getMethod("setStreamKeys", List.class);
            createMethod = factoryClazz.getMethod("createMediaSource", Uri.class);
            // LINT.ThenChange(../../../../../../../../proguard-rules.txt)
        } catch (ClassNotFoundException e) {
            // Expected if the app was built without the respective module.
        } catch (NoSuchMethodException | SecurityException e) {
            // Something is wrong with the library or the proguard configuration.
            throw new IllegalStateException(e);
        }
        return new MediaSourceFactory(constructor, setStreamKeysMethod, createMethod);
    }

    public String getUserAgent() {
        return userAgent;
    }

    private DataSource.Factory buildDataSourceFactory() {
        DefaultDataSourceFactory upstreamFactory = new DefaultDataSourceFactory(AppObjectController.getJoshApplication(), buildHttpDataSourceFactory());
        return buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache());
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory() {
        return new DefaultHttpDataSourceFactory(userAgent);
    }

    private boolean useExtensionRenderers() {
        return false;
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

    public DownloadManager getDownloadManager() {
        initDownloadManager();
        return downloadManager;
    }

    public DownloadTracker getDownloadTracker() {
        initDownloadManager();
        return downloadTracker;
    }

    public synchronized Cache getDownloadCache() {
        if (downloadCache == null) {
            File downloadContentDirectory = new File(getDownloadDirectory(), DOWNLOAD_CONTENT_DIRECTORY);
            downloadCache =
                    new SimpleCache(downloadContentDirectory, new NoOpCacheEvictor(), getDatabaseProvider());
        }
        return downloadCache;
    }

    private synchronized void initDownloadManager() {
        try {
            if (downloadManager == null) {
                DefaultDownloadIndex downloadIndex = new DefaultDownloadIndex(getDatabaseProvider());
                upgradeActionFile(
                        DOWNLOAD_ACTION_FILE, downloadIndex, false);
                upgradeActionFile(
                        DOWNLOAD_TRACKER_ACTION_FILE, downloadIndex, true);
                DownloaderConstructorHelper downloaderConstructorHelper =
                        new DownloaderConstructorHelper(getDownloadCache(), buildHttpDataSourceFactory());
                downloadManager =
                        new DownloadManager(
                                AppObjectController.getJoshApplication(), downloadIndex, new DefaultDownloaderFactory(downloaderConstructorHelper));
                downloadManager.setMinRetryCount(5);
                downloadManager.setMaxParallelDownloads(10);
                downloadTracker =
                        new DownloadTracker(AppObjectController.getJoshApplication(), buildDataSourceFactory(), downloadManager);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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

    private DatabaseProvider getDatabaseProvider() {
        if (databaseProvider == null) {
            databaseProvider = new ExoDatabaseProvider(AppObjectController.getJoshApplication());
        }
        return databaseProvider;
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

    public MediaSource getMediaSource(String url) {
        return getMediaSource(Uri.parse(url));
    }

    public MediaSource getMediaSource(Uri uri) {
        int type = Util.inferContentType(uri, null);
        DownloadRequest downloadRequest = getDownloadTracker().getDownloadRequest(uri);
        if (downloadRequest != null) {
            return createMediaSource(downloadRequest, buildDataSourceFactory());
        }

        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(buildDataSourceFactory()).createMediaSource(uri);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(buildDataSourceFactory()).createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(buildDataSourceFactory()).createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(buildDataSourceFactory()).createMediaSource(uri);
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    private static final class MediaSourceFactory {
        @Nullable
        private final Constructor<?> constructor;
        @Nullable
        private final Method setStreamKeysMethod;
        @Nullable
        private final Method createMethod;

        public MediaSourceFactory(
                @Nullable Constructor<?> constructor,
                @Nullable Method setStreamKeysMethod,
                @Nullable Method createMethod) {
            this.constructor = constructor;
            this.setStreamKeysMethod = setStreamKeysMethod;
            this.createMethod = createMethod;
        }

        private MediaSource createMediaSource(
                Uri uri, DataSource.Factory dataSourceFactory, @Nullable List<StreamKey> streamKeys) {
            if (constructor == null || setStreamKeysMethod == null || createMethod == null) {
                throw new IllegalStateException("Module missing to create media source.");
            }
            try {
                Object factory = constructor.newInstance(dataSourceFactory);
                if (streamKeys != null) {
                    setStreamKeysMethod.invoke(factory, streamKeys);
                }
                return (MediaSource) Assertions.checkNotNull(createMethod.invoke(factory, uri));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to instantiate media source.", e);
            }
        }
    }
}

