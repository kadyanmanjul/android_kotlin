package com.joshtalks.joshskills.core

import android.os.Handler
import android.os.Looper
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.android.exoplayer2.util.Util
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.google.gson.Gson
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.service.SignUpNetworkService
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
import com.joshtalks.joshskills.repository.service.MediaDUNetworkService
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.HttpUrlConnectionDownloader
import com.tonyodev.fetch2core.Downloader
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import com.vanniktech.emoji.EmojiManager
import java.text.DateFormat
import com.vanniktech.emoji.google.GoogleEmojiProvider
import okhttp3.OkHttpClient
import java.lang.reflect.Modifier
import com.google.gson.JsonParseException
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonDeserializer
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.core.service.video_download.DownloadTracker
import com.joshtalks.joshskills.core.service.video_download.VideoDownloadController
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.service.ChatNetworkService
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.TimeUnit


val KEY_AUTHORIZATION = "Authorization"
val KEY_APP_VERSION_CODE = "app-version-code"
val KEY_APP_VERSION_NAME = "app-version-name"
val REMOTE_CONFIG_PREFIX = "josh_param_"

const val SERVER_URL = "http://skills.joshtalks.org"
//const val SERVER_URL = "http://192.168.14.60:8000"

internal class AppObjectController {

    companion object {

        @JvmStatic
        var INSTANCE: AppObjectController =
            AppObjectController()


        @JvmStatic
        lateinit var joshApplication: JoshApplication
            private set
        @JvmStatic
        lateinit var appDatabase: AppDatabase
            private set

        @JvmStatic
        lateinit var gsonMapper: Gson
            private set

        @JvmStatic
        lateinit var gsonMapperForLocal: Gson
            private set

        @JvmStatic
        lateinit var retrofit: Retrofit

        @JvmStatic
        lateinit var signUpNetworkService: SignUpNetworkService


        @JvmStatic
        lateinit var chatNetworkService: ChatNetworkService

        @JvmStatic
        lateinit var mediaDUNetworkService: MediaDUNetworkService

        @JvmStatic
        lateinit var fetch: Fetch

        @JvmStatic
        var uiHandler: Handler = Handler(Looper.getMainLooper())


        @JvmStatic
        var screenWidth: Int = 0

        @JvmStatic
        var screenHeight: Int = 0

        @JvmStatic
        lateinit var okHttpClient: OkHttpClient

        @JvmStatic
        lateinit var userAgent: String

        @JvmStatic
        lateinit var videoDownloadTracker: DownloadTracker

        /* @JvmStatic
        val videoDownloadListener = HashMap<String, DownloadTracker.Listener>()
*/

        fun init(context: JoshApplication): AppObjectController {
            joshApplication = context;
            appDatabase = AppDatabase.getDatabase(context)!!
            com.joshtalks.joshskills.core.ActivityLifecycleCallback.register(joshApplication)
            ActivityLifecycleCallback.register(joshApplication)

            gsonMapper = GsonBuilder()
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .registerTypeAdapter(Date::class.java, object : JsonDeserializer<Date> {
                    @Throws(JsonParseException::class)
                    override fun deserialize(
                        json: JsonElement,
                        typeOfT: Type,
                        context: JsonDeserializationContext
                    ): Date {
                        return Date(json.asJsonPrimitive.asLong * 1000)
                    }
                })
                .excludeFieldsWithModifiers(
                    Modifier.TRANSIENT
                )
                .setDateFormat(DateFormat.LONG)
                .setPrettyPrinting()
                .create()

            gsonMapperForLocal = GsonBuilder()
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .excludeFieldsWithModifiers(
                    Modifier.TRANSIENT
                )
                .setDateFormat(DateFormat.LONG)
                .setPrettyPrinting()
                .create()


            val builder = OkHttpClient().newBuilder()
            if (BuildConfig.DEBUG) {
                builder.addNetworkInterceptor(StethoInterceptor())
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                builder.addInterceptor(logging)
            }
            builder.addInterceptor(object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val original = chain.request()
                    val newRequest: Request.Builder = original.newBuilder()
                    if (User.getInstance() != null && User.getInstance().token != null) {
                        newRequest.addHeader(
                            KEY_AUTHORIZATION,
                            "Bearer " + (User.getInstance().token?.accessToken ?: "")
                        )
                            .addHeader(KEY_APP_VERSION_NAME, BuildConfig.VERSION_NAME)
                            .addHeader(KEY_APP_VERSION_CODE, BuildConfig.VERSION_CODE.toString())
                    }
                    return chain.proceed(newRequest.build())
                }

            })
            okHttpClient = builder.build()

            retrofit = Retrofit.Builder()
                .baseUrl(SERVER_URL)
                .client(builder.build())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create(gsonMapper))
                .build()


            signUpNetworkService = retrofit.create(SignUpNetworkService::class.java)
            chatNetworkService = retrofit.create(ChatNetworkService::class.java)


            val mediaOkhttpBuilder = OkHttpClient().newBuilder()
            mediaOkhttpBuilder.connectTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .retryOnConnectionFailure(true)
            if (BuildConfig.DEBUG) {
                mediaOkhttpBuilder.addNetworkInterceptor(StethoInterceptor())
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                mediaOkhttpBuilder.addInterceptor(logging)
            }

            mediaDUNetworkService = Retrofit.Builder()
                .baseUrl(SERVER_URL)
                .client(mediaOkhttpBuilder.build())
                .build().create(MediaDUNetworkService::class.java)


            EmojiManager.install(GoogleEmojiProvider())


            val fetchConfiguration = FetchConfiguration.Builder(context)
                .enableRetryOnNetworkGain(true)
                .setDownloadConcurrentLimit(10)
                .enableLogging(true)
                .setAutoRetryMaxAttempts(5)
                .setHttpDownloader(HttpUrlConnectionDownloader(Downloader.FileDownloaderType.PARALLEL))
                .setHttpDownloader(getOkHttpDownloader())
                .build()
            Fetch.setDefaultInstanceConfiguration(fetchConfiguration)

            fetch = Fetch.getInstance(fetchConfiguration)
            videoDownloadTracker= VideoDownloadController.getInstance().downloadTracker
            initExoPlayer()


            return INSTANCE
        }

        private fun initExoPlayer() {
            userAgent =
                Util.getUserAgent(joshApplication, joshApplication.getString(R.string.app_name))

        }

        fun clearDownloadMangerCallback() {
            try {
                DownloadUtils.objectFetchListener.forEach { (key, value) ->

                    fetch.removeListener(value)
                    DownloadUtils.objectFetchListener.remove(key)
                }
            } catch (ex: Exception) {

            }

        }

        private fun getOkHttpDownloader(): OkHttpDownloader {
            val okHttpClient = OkHttpClient.Builder().build()
            return OkHttpDownloader(
                okHttpClient,
                Downloader.FileDownloaderType.PARALLEL
            )
        }

        fun addVideoCallback(id: String) {
            /* val videoDownloadCallback = object : DownloadTracker.Listener {
                 override fun onDownloadsChanged(download: com.google.android.exoplayer2.offline.Download?) {
                     download?.let {
                         var downloadStatus: DOWNLOAD_STATUS = DOWNLOAD_STATUS.NOT_START

                         if (it.state == com.google.android.exoplayer2.offline.Download.STATE_COMPLETED) {
                             downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
                         } else if (it.state == com.google.android.exoplayer2.offline.Download.STATE_DOWNLOADING || it.state == com.google.android.exoplayer2.offline.Download.STATE_QUEUED) {
                             videoNotUploadFlagUpdate()
                             downloadStatus = DOWNLOAD_STATUS.DOWNLOADING
                         } else if (it.state == com.google.android.exoplayer2.offline.Download.STATE_REMOVING) {
                             downloadStatus = DOWNLOAD_STATUS.FAILED
                         } else if (it.state == com.google.android.exoplayer2.offline.Download.STATE_FAILED) {
                             downloadStatus = DOWNLOAD_STATUS.FAILED
                         } else if (it.state == com.google.android.exoplayer2.offline.Download.STATE_STOPPED) {
                             downloadStatus = DOWNLOAD_STATUS.FAILED
                         }
                         DatabaseUtils.updateVideoDownload(download.request.id, downloadStatus)
                     }
                 }

                 override fun onDownloadRemoved(download: com.google.android.exoplayer2.offline.Download?) {
                 }

             }
             VideoDownloadController.getInstance().downloadTracker.addListener(videoDownloadCallback)
             videoDownloadListener[id] = videoDownloadCallback
 */
        }

        fun videoNotUploadFlagUpdate() {
            val timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    DatabaseUtils.updateAllVideoStatusWhichIsDownloading()
                }
            }, 0, 30 * 60 * 1000);

        }
    }
}
