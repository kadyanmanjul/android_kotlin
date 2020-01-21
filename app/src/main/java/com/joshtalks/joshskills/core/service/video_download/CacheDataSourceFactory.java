package com.joshtalks.joshskills.core.service.video_download;

import android.content.Context;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSink;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;
import com.joshtalks.joshskills.R;

import java.io.File;


public class CacheDataSourceFactory implements DataSource.Factory {

    private static SimpleCache simpleCache;
    private final Context context;
    private final DefaultDataSourceFactory defaultDatasourceFactory;
    private final long maxFileSize, maxCacheSize;

    public CacheDataSourceFactory(Context context, long maxCacheSize, long maxFileSize) {
        super();

        this.context = context;
        this.maxCacheSize = maxCacheSize;
        this.maxFileSize = maxFileSize;
        String userAgent = Util.getUserAgent(context, context.getString(R.string.app_name));

        defaultDatasourceFactory = new DefaultDataSourceFactory(this.context, userAgent);
    }

    @Override
    public DataSource createDataSource() {
        LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(maxCacheSize);

        if (simpleCache == null)
            simpleCache = new SimpleCache(new File(context.getCacheDir(), "media"), evictor);

        return new CacheDataSource(simpleCache,
                defaultDatasourceFactory.createDataSource(),
                new FileDataSource(),
                new CacheDataSink(simpleCache, maxFileSize),
                CacheDataSource.FLAG_BLOCK_ON_CACHE
                        | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                null);
    }


}
/*
public class CacheDataSourceFactory implements DataSource.Factory {

    private final DefaultDataSourceFactory defaultDatasourceFactory;
    public static final int PLAYER_MAX_FILE_SIZE = 50 * 1024 * 1024;

    public CacheDataSourceFactory(Context context) {
        super();
        defaultDatasourceFactory = new DefaultDataSourceFactory(context, VideoDownloadController.getInstance().getUserAgent());
    }

    @Override
    public DataSource createDataSource() {


        return new CacheDataSource(VideoDownloadController.getInstance().getDownloadCache(),
                defaultDatasourceFactory.createDataSource(),
                new FileDataSource(),
                new CacheDataSink(VideoDownloadController.getInstance().getDownloadCache(), PLAYER_MAX_FILE_SIZE),
                CacheDataSource.FLAG_BLOCK_ON_CACHE
                        | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                null);
    }


}*/
