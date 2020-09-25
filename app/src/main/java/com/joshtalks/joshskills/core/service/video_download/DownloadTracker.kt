package com.joshtalks.joshskills.core.service.video_download

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.offline.*
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController.Companion.gsonMapper
import com.joshtalks.joshskills.core.PrefManager.getStringValue
import com.joshtalks.joshskills.core.SELECTED_QUALITY
import com.joshtalks.joshskills.core.videoplayer.VideoQualityTrack
import com.joshtalks.joshskills.messaging.RxBus2.publish
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.MediaProgressEventBus
import java.io.IOException
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Tracks media that has been downloaded.
 */
class DownloadTracker internal constructor(
    context: Context, dataSourceFactory: DataSource.Factory, downloadManager: DownloadManager
) {
    private val context: Context
    private val dataSourceFactory: DataSource.Factory
    private val listeners: CopyOnWriteArraySet<Listener>
    private val downloads: HashMap<Uri, Download>
    private val downloadIndex: DownloadIndex
    private var startDownloadDialogHelper: StartDownloadDialogHelper? = null
    private fun loadDownloads() {
        try {
            downloadIndex.getDownloads().use { loadedDownloads ->
                while (loadedDownloads.moveToNext()) {
                    val download = loadedDownloads.download
                    downloads[download.request.uri] = download
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to query downloads", e)
        }
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun isDownloaded(uri: Uri): Boolean {
        val download = downloads[uri]
        return download != null && download.state != Download.STATE_FAILED
    }

    fun downloadMediaSize(url: String?): Long {
        return VideoDownloadController.getInstance().getDownloadedUrlSize(url)
    }

    fun getDownloadRequest(uri: Uri): DownloadRequest? {
        val download = downloads[uri]
        return if (download != null && download.state != Download.STATE_FAILED) download.request else null
    }

    fun download(
        chatObj: ChatModel?,
        uri: Uri,
        renderersFactory: RenderersFactory
    ) {
        val download = downloads[uri]
        if (startDownloadDialogHelper != null) {
            startDownloadDialogHelper!!.release()
        }
        startDownloadDialogHelper =
            StartDownloadDialogHelper(getDownloadHelper(context, uri, renderersFactory), chatObj)
    }

    private fun getDownloadHelper(
        context: Context,
        uri: Uri, renderersFactory: RenderersFactory
    ): DownloadHelper {
        val type = Util.inferContentType(uri, null)
        return when (type) {
            C.TYPE_DASH -> DownloadHelper.forDash(context, uri, dataSourceFactory, renderersFactory)
            C.TYPE_SS -> DownloadHelper.forSmoothStreaming(
                context,
                uri,
                dataSourceFactory,
                renderersFactory
            )
            C.TYPE_HLS -> DownloadHelper.forHls(context, uri, dataSourceFactory, renderersFactory)
            C.TYPE_OTHER -> DownloadHelper.forProgressive(context, uri)
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }

    fun cancelDownload(
        uri: Uri
    ) {
        val download = downloads[uri]
        if (download != null) {
            DownloadService.sendRemoveDownload(
                context,
                VideoDownloadService::class.java,
                download.request.id,  /* foreground= */
                true
            )
        }
    }

    fun removeDownload(
        uri: Uri
    ) {
        val download = downloads[uri]
        if (download != null) {
            DownloadService.sendRemoveDownload(
                context,
                VideoDownloadService::class.java,
                download.request.id,  /* foreground= */
                true
            )
        }
    }

    interface Listener {
        fun onDownloadsChanged(download: Download?)
        fun onDownloadRemoved(download: Download?)
        fun onError(key: String?, ex: Exception?)
    }

    private inner class DownloadManagerListener : DownloadManager.Listener {
        override fun onDownloadChanged(downloadManager: DownloadManager, download: Download) {
            Log.e("download_state", "" + download.state)
            downloads[download.request.uri] = download
            for (listener in listeners) {
                listener.onDownloadsChanged(download)
            }
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            downloads.remove(download.request.uri)
            for (listener in listeners) {
                listener.onDownloadRemoved(download)
            }
        }
    }

    private inner class StartDownloadDialogHelper(
        private val downloadHelper: DownloadHelper,
        private val chatObj: ChatModel?
    ) : DownloadHelper.Callback {
        fun release() {
            downloadHelper.release()
        }

        override fun onPrepared(helper: DownloadHelper) {
            try {
                if (helper.periodCount == 0) {
                    Log.d(TAG, "No periods found. Downloading entire stream.")
                    startDownload()
                    downloadHelper.release()
                    return
                }
                val mappedTrackInfo = downloadHelper.getMappedTrackInfo(0)
                if (!willHaveContent(mappedTrackInfo)) {
                    Log.d(TAG, "No dialog content. Downloading entire stream.")
                    startDownload()
                    downloadHelper.release()
                    return
                }
                val listOfVideoQualityTrack = ArrayList<VideoQualityTrack>()
                val trackGroups = mappedTrackInfo.getTrackGroups(0)[0]
                for (x in 0 until trackGroups.length) {
                    val currentQuality = "" + trackGroups.getFormat(x).height + "p"
                    listOfVideoQualityTrack.add(
                        VideoQualityTrack(
                            x,
                            trackGroups.getFormat(x).height,
                            currentQuality, false
                        )
                    )
                }
                var qualityIndex =
                    context.resources.getStringArray(R.array.resolutions).size - 1 - listOf(
                        *context.resources.getStringArray(
                            R.array.resolutions
                        )
                    )
                        .indexOf(
                            getStringValue(SELECTED_QUALITY, false)
                        )

                val trackSelectionFactory: TrackSelection.Factory =
                    AdaptiveTrackSelection.Factory()
                val trackSelector = DefaultTrackSelector(context, trackSelectionFactory)
                trackSelector.parameters.buildUpon()
                    .setForceLowestBitrate(true)
                    .setForceHighestSupportedBitrate(false)
                    .setAllowAudioMixedChannelCountAdaptiveness(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedSampleRateAdaptiveness(true)
                    .build()
                val parametersBuilder = trackSelector.buildUponParameters()
                // parametersBuilder?.setRendererDisabled(0, false)
                val trackGroupsList = mappedTrackInfo.getTrackGroups(0)
                listOfVideoQualityTrack.sortBy { it.quality }

//                    while (trackGroupsList.length <= qualityIndex) --qualityIndex

                if (trackGroupsList[0].length <= qualityIndex)
                    qualityIndex = trackGroupsList[0].length - 1

                val selectionOverride = SelectionOverride(0, qualityIndex)

                parametersBuilder.setSelectionOverride(0, trackGroupsList, selectionOverride)
                trackSelector.parameters = parametersBuilder.build()
                downloadHelper.clearTrackSelections(0)
                downloadHelper.addTrackSelectionForSingleRenderer(
                    0,
                    0,
                    parametersBuilder.build(),
                    Arrays.asList(selectionOverride)
                )
                /*for (int periodIndex = 0; periodIndex < downloadHelper.getPeriodCount(); periodIndex++) {
                    downloadHelper.clearTrackSelections(periodIndex);
                    for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
                        TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory();
                        DefaultTrackSelector trackSelector = new DefaultTrackSelector(context, trackSelectionFactory);
                        DefaultTrackSelector.Parameters currentParameters = trackSelector.getParameters();
                        DefaultTrackSelector.Parameters newParameters = currentParameters
                                .buildUpon()
                                .setForceLowestBitrate(true)
                                .setForceHighestSupportedBitrate(false)
                                // .setMaxVideoSizeSd()
                                .build();
                        trackSelector.setParameters(newParameters);
                        downloadHelper.addTrackSelectionForSingleRenderer(periodIndex, i, newParameters, getOverrides(i));
                    }
                }*/
                val downloadRequest = buildDownloadRequest()
                if (downloadRequest.streamKeys.isEmpty()) {
                    // All tracks were deselected in the dialog. Don't start the download.
                    return
                }
                startDownload(downloadRequest)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onPrepareError(helper: DownloadHelper, e: IOException) {
            Log.e("error", "err")
            for (listener in listeners) {
                listener.onError(gsonMapper.toJson(chatObj), e)
            }
            publish(
                MediaProgressEventBus(
                    Download.STATE_STOPPED, gsonMapper.toJson(
                        chatObj
                    ), 0f
                )
            )
            e.printStackTrace()
        }

        /**
         * Returns whether a track selection dialog will have content to display if initialized with the
         * specified [MappedTrackInfo].
         */
        fun willHaveContent(mappedTrackInfo: MappedTrackInfo?): Boolean {
            for (i in 0 until mappedTrackInfo!!.rendererCount) {
                if (showTabForRenderer(mappedTrackInfo, i)) {
                    return true
                }
            }
            return false
        }

        private fun startDownload(downloadRequest: DownloadRequest = buildDownloadRequest()) {
            DownloadService.sendAddDownload(
                context, VideoDownloadService::class.java, downloadRequest, true
            )
        }

        private fun buildDownloadRequest(): DownloadRequest {
            return downloadHelper.getDownloadRequest(
                Util.getUtf8Bytes(
                    gsonMapper.toJson(
                        chatObj
                    )
                )
            )
        }

        fun getOverrides(pos: Int): List<SelectionOverride> {
            return if (pos == 0) {
                val overrideList: MutableList<SelectionOverride> =
                    ArrayList()
                overrideList.add(SelectionOverride(0, 0))
                overrideList
            } else {
                emptyList()
            }
        }

        private fun showTabForRenderer(
            mappedTrackInfo: MappedTrackInfo?,
            rendererIndex: Int
        ): Boolean {
            val trackGroupArray = mappedTrackInfo!!.getTrackGroups(rendererIndex)
            if (trackGroupArray.length == 0) {
                return false
            }
            val trackType = mappedTrackInfo.getRendererType(rendererIndex)
            return isSupportedTrackType(trackType)
        }

        private fun isSupportedTrackType(trackType: Int): Boolean {
            return when (trackType) {
                C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO, C.TRACK_TYPE_TEXT -> true
                else -> false
            }
        }

        /**
         * Returns whether a track selection dialog will have content to display if initialized with the
         * specified [DefaultTrackSelector] in its current state.
         */
        fun willHaveContent(trackSelector: DefaultTrackSelector): Boolean {
            val mappedTrackInfo = trackSelector.currentMappedTrackInfo
            return mappedTrackInfo != null && willHaveContent(mappedTrackInfo)
        }

        init {
            downloadHelper.prepare(this)
        }
    }

    companion object {
        private const val TAG = "DownloadTracker"
    }

    init {
        this.context = context.applicationContext
        this.dataSourceFactory = dataSourceFactory
        listeners = CopyOnWriteArraySet()
        downloads = HashMap()
        downloadIndex = downloadManager.downloadIndex
        downloadManager.addListener(DownloadManagerListener())
        loadDownloads()
    }
}