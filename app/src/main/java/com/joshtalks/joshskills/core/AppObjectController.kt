package com.joshtalks.joshskills.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.load.MultiTransformation
import com.clevertap.android.sdk.ActivityLifecycleCallback
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventsLogger
import com.flurry.android.FlurryAgent
import com.flurry.android.FlurryPerformance
import com.freshchat.consumer.sdk.Freshchat
import com.freshchat.consumer.sdk.FreshchatConfig
import com.freshchat.consumer.sdk.FreshchatNotificationConfig
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.datetimeutils.DateTimeUtils
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.core.service.video_download.DownloadTracker
import com.joshtalks.joshskills.core.service.video_download.VideoDownloadController
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.service.ChatNetworkService
import com.joshtalks.joshskills.repository.service.CommonNetworkService
import com.joshtalks.joshskills.repository.service.MediaDUNetworkService
import com.joshtalks.joshskills.repository.service.SignUpNetworkService
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.ui.view_holders.IMAGE_SIZE
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import com.newrelic.agent.android.FeatureFlag
import com.newrelic.agent.android.NewRelic
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.HttpUrlConnectionDownloader
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2core.Downloader
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import com.uxcam.UXCam
import io.branch.referral.Branch
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import okhttp3.CertificatePinner
import okhttp3.CipherSuite
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.net.URL
import java.text.DateFormat
import java.util.Collections
import java.util.Date
import java.util.concurrent.TimeUnit

const val KEY_AUTHORIZATION = "Authorization"
const val KEY_APP_VERSION_CODE = "app-version-code"
const val KEY_APP_VERSION_NAME = "app-version-name"
const val KEY_APP_USER_AGENT = "HTTP_USER_AGENT"
const val KEY_APP_ACCEPT_LANGUAGE = "Accept-Language"

private const val JOSH_SKILLS_CACHE = "joshskills-cache"
private const val READ_TIMEOUT = 30L
private const val WRITE_TIMEOUT = 30L
private const val CONNECTION_TIMEOUT = 30L
private const val CALL_TIMEOUT = 60L


class AppObjectController {

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
            private set

        @JvmStatic
        lateinit var signUpNetworkService: SignUpNetworkService
            private set

        @JvmStatic
        lateinit var commonNetworkService: CommonNetworkService
            private set


        @JvmStatic
        lateinit var chatNetworkService: ChatNetworkService
            private set


        @JvmStatic
        lateinit var mediaDUNetworkService: MediaDUNetworkService
            private set


        @JvmStatic
        private var fetch: Fetch? = null

        @JvmStatic
        var uiHandler: Handler = Handler(Looper.getMainLooper())
            private set

        @JvmStatic
        var screenWidth: Int = 0

        @JvmStatic
        var screenHeight: Int = 0

        @JvmStatic
        lateinit var videoDownloadTracker: DownloadTracker
            private set

        @JvmStatic
        lateinit var multiTransformation: MultiTransformation<Bitmap>

        @JvmStatic
        lateinit var facebookEventLogger: AppEventsLogger
            private set

        @JvmStatic
        lateinit var firebaseAnalytics: FirebaseAnalytics
            private set

        @JvmStatic
        var freshChat: Freshchat? = null
            private set

        @JvmStatic
        var currentActivityClass: String? = null

        @JvmStatic
        var currentPlayingAudioObject: ChatModel? = null

        @JvmStatic
        var isSettingUpdate: Boolean = false

        fun initLibrary(context: Context): AppObjectController {
            joshApplication = context as JoshApplication
            appDatabase = AppDatabase.getDatabase(context)!!
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            firebaseAnalytics.setAnalyticsCollectionEnabled(true)
            initDebugService()
            Branch.getAutoInstance(context)
            initFirebaseRemoteConfig()
            configureCrashlytics()
            initFlurryAnalytics(context)
            initNewRelic(context)
            initFonts()
            WorkManagerAdmin.deviceIdGenerateWorker()
            WorkManagerAdmin.runMemoryManagementWorker()

            gsonMapper = GsonBuilder()
                .enableComplexMapKeySerialization()
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
                .serializeNulls()
                .setDateFormat(DateFormat.LONG)
                .setPrettyPrinting()
                .setLenient()
                .create()

            val builder = OkHttpClient().newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .callTimeout(CALL_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followSslRedirects(true)
                .addInterceptor(StatusCodeInterceptor())
                .addInterceptor(NewRelicHttpMetricsLogger())
                .addInterceptor(HeaderInterceptor())
                .hostnameVerifier { _, _ -> true }

            if (BuildConfig.DEBUG.not() && BuildConfig.FLAVOR != "prod") {
                builder.certificatePinner(
                    CertificatePinner.Builder()
                        .add(
                            getHostOfUrl(),
                            *getCertificatePins()
                        )
                        .build()
                )
                val spec: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .cipherSuites(
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256
                    )
                    .build()

                builder.connectionSpecs(Collections.singletonList(spec))
            }

            if (BuildConfig.DEBUG) {
                val logging =
                    HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
                        override fun log(message: String) {
                            Timber.tag("OkHttp").d(message)
                        }

                    }).apply {
                        level = HttpLoggingInterceptor.Level.BODY

                    }
                builder.addInterceptor(logging)
                builder.addNetworkInterceptor(getStethoInterceptor())
                builder.eventListener(PrintingEventListener())
            }
            retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(builder.build())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create(gsonMapper))
                .build()
            signUpNetworkService = retrofit.create(SignUpNetworkService::class.java)
            chatNetworkService = retrofit.create(ChatNetworkService::class.java)
            commonNetworkService = retrofit.create(CommonNetworkService::class.java)
            initObjectInThread(context)
            return INSTANCE
        }

        fun init(context: JoshApplication) {
            joshApplication = context
            com.joshtalks.joshskills.core.ActivityLifecycleCallback.register(joshApplication)
            ActivityLifecycleCallback.register(joshApplication)
            AppEventsLogger.activateApp(joshApplication)
            initUXCam()
            initFacebookService(joshApplication)
        }


        private fun initDebugService() {
            if (BuildConfig.DEBUG) {
                StrictMode.setVmPolicy(
                    StrictMode.VmPolicy.Builder()
                        .detectAll()
                        .penaltyLog()
                        .build()
                )
                StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy.Builder()
                        .detectAll()
                        .penaltyLog()
                        .build()
                )
                Timber.plant(Timber.DebugTree())
                Branch.enableLogging()
                Branch.enableTestMode()
            }
        }

        private fun initFacebookService(context: Context) {
            if (FacebookSdk.isInitialized().not()) {
                FacebookSdk.fullyInitialize()
            }
            FacebookSdk.setAutoLogAppEventsEnabled(false)
            FacebookSdk.setLimitEventAndDataUsage(context, true)
            facebookEventLogger = AppEventsLogger.newLogger(context)
            facebookEventLogger.flush()

            if (BuildConfig.DEBUG) {
                initStethoLibrary(context)
                FacebookSdk.setIsDebugEnabled(true)
                FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
                FacebookSdk.addLoggingBehavior(LoggingBehavior.CACHE)
                FacebookSdk.addLoggingBehavior(LoggingBehavior.DEVELOPER_ERRORS)
                FacebookSdk.addLoggingBehavior(LoggingBehavior.GRAPH_API_DEBUG_INFO)
                FacebookSdk.addLoggingBehavior(LoggingBehavior.GRAPH_API_DEBUG_WARNING)
                FacebookSdk.addLoggingBehavior(LoggingBehavior.REQUESTS)
                FacebookSdk.setCodelessDebugLogEnabled(true)
                FacebookSdk.setMonitorEnabled(true)
            }
        }

        private fun initFonts() {
            ViewPump.init(
                ViewPump.builder().addInterceptor(
                    CalligraphyInterceptor(
                        CalligraphyConfig.Builder()
                            .setDefaultFontPath("fonts/OpenSans-Regular.ttf")
                            .setFontAttrId(R.attr.fontPath)
                            .build()
                    )
                ).build()
            )
        }

        private fun getHostOfUrl(): String {
            val aURL = URL(BuildConfig.BASE_URL)
            return aURL.host
        }

        private fun initNewRelic(context: Context) {
            NewRelic.enableFeature(FeatureFlag.CrashReporting)
            NewRelic.enableFeature(FeatureFlag.DefaultInteractions)
            NewRelic.enableFeature(FeatureFlag.DistributedTracing)
            NewRelic.enableFeature(FeatureFlag.GestureInstrumentation)
            NewRelic.enableFeature(FeatureFlag.HttpResponseBodyCapture)
            NewRelic.enableFeature(FeatureFlag.HandledExceptions)
            NewRelic.enableFeature(FeatureFlag.NetworkErrorRequests)
            NewRelic.enableFeature(FeatureFlag.NetworkRequests)
            NewRelic.enableFeature(FeatureFlag.AnalyticsEvents)
            NewRelic.withApplicationToken(BuildConfig.NEW_RELIC_TOKEN)
                .withLocationServiceEnabled(true)
                // .withLogLevel(AgentLog.AUDIT)
                .start(
                    context
                )
        }

        private fun initFirebaseRemoteConfig() {
            val configSettingsBuilder = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(60 * 3600)
            getFirebaseRemoteConfig().setConfigSettingsAsync(configSettingsBuilder.build())
            getFirebaseRemoteConfig().setDefaultsAsync(R.xml.remote_config_defaults)
            getFirebaseRemoteConfig().fetchAndActivate()
        }

        private fun configureCrashlytics() {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        }

        private fun initFlurryAnalytics(context: Context) {
            FlurryAgent.Builder()
                .withDataSaleOptOut(false) //CCPA - the default value is false
                .withCaptureUncaughtExceptions(true)
                .withIncludeBackgroundSessionsInMetrics(true)
                .withLogLevel(Log.VERBOSE)
                .withPerformanceMetrics(FlurryPerformance.ALL)
                .build(context, BuildConfig.FLURRY_API_KEY)
        }

        fun initialiseFreshChat() {
            JoshSkillExecutors.BOUNDED.submit {
                try {
                    val config =
                        FreshchatConfig(
                            BuildConfig.FRESH_CHAT_APP_ID,
                            BuildConfig.FRESH_CHAT_APP_KEY
                        )
                    config.isCameraCaptureEnabled = true
                    config.isGallerySelectionEnabled = true
                    config.isResponseExpectationEnabled = true
                    config.domain = "https://msdk.in.freshchat.com"
                    freshChat = Freshchat.getInstance(joshApplication)
                    freshChat?.init(config)
                    val notificationConfig = FreshchatNotificationConfig()
                        .setImportance(NotificationManagerCompat.IMPORTANCE_MAX)
                    freshChat?.setNotificationConfig(notificationConfig)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        fun restoreUser(restoreId: String?) {
            if (restoreId.isNullOrBlank()) {
                freshChat?.identifyUser(PrefManager.getStringValue(USER_UNIQUE_ID), null)
            } else if (PrefManager.getBoolValue(FRESH_CHAT_ID_RESTORED).not()) {
                PrefManager.put(FRESH_CHAT_ID_RESTORED, true)
                freshChat?.identifyUser(PrefManager.getStringValue(USER_UNIQUE_ID), restoreId)
            }
        }

        fun getUnreadFreshchatMessages() {
            freshChat?.getUnreadCountAsync { _, unreadCount ->
                PrefManager.put(FRESH_CHAT_UNREAD_MESSAGES, unreadCount)
            }
        }

        fun clearDownloadMangerCallback() {
            try {
                DownloadUtils.objectFetchListener.forEach { (key, value) ->
                    fetch?.removeListener(value)
                    DownloadUtils.objectFetchListener.remove(key)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        fun getFirebaseRemoteConfig(): FirebaseRemoteConfig {
            return FirebaseRemoteConfig.getInstance()
        }

        private fun initUXCam() {
            UXCam.setAutomaticScreenNameTagging(true)
        }

        private fun getOkHttpDownloader(): OkHttpDownloader {
            val mediaOkhttpBuilder = OkHttpClient().newBuilder()
            mediaOkhttpBuilder.connectTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .followRedirects(true)
            val okHttpClient = mediaOkhttpBuilder.build()
            return OkHttpDownloader(
                okHttpClient,
                Downloader.FileDownloaderType.PARALLEL
            )
        }

        fun getFetchObject(): Fetch {
            if (fetch == null || fetch!!.isClosed) {
                val fetchConfiguration = FetchConfiguration.Builder(joshApplication)
                    .enableRetryOnNetworkGain(true)
                    .setDownloadConcurrentLimit(50)
                    .enableLogging(true)
                    .setAutoRetryMaxAttempts(1)
                    .setGlobalNetworkType(NetworkType.ALL)
                    .setHttpDownloader(HttpUrlConnectionDownloader(Downloader.FileDownloaderType.PARALLEL))
                    .setHttpDownloader(getOkHttpDownloader())
                    .setNamespace("JoshTalks")
                    .enableHashCheck(false)
                    .enableFileExistChecks(false)
                    .build()
                Fetch.setDefaultInstanceConfiguration(fetchConfiguration)
                fetch = Fetch.Impl.getInstance(fetchConfiguration)
            }
            return fetch as Fetch
        }

        fun createDefaultCacheDir(): String {
            val cache = File(joshApplication.cacheDir, JOSH_SKILLS_CACHE)
            if (!cache.exists()) {
                cache.mkdirs()
            }
            return getAppCachePath()
        }

        fun getAppCachePath(): String {
            return "${joshApplication.cacheDir}/${JOSH_SKILLS_CACHE}"
        }

        private fun initObjectInThread(context: Context) {
            Thread {
                val mediaOkhttpBuilder = OkHttpClient().newBuilder()
                mediaOkhttpBuilder.connectTimeout(45, TimeUnit.SECONDS)
                    .writeTimeout(45, TimeUnit.SECONDS)
                    .readTimeout(45, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .addInterceptor(StatusCodeInterceptor())

                if (BuildConfig.DEBUG) {
                    val logging =
                        HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
                            override fun log(message: String) {
                                Timber.tag("OkHttp").d(message)
                            }

                        }).apply {
                            level = HttpLoggingInterceptor.Level.BODY

                        }
                    mediaOkhttpBuilder.addInterceptor(logging)
                    mediaOkhttpBuilder.addNetworkInterceptor(getStethoInterceptor())
                }

                mediaOkhttpBuilder.addInterceptor(object : Interceptor {
                    override fun intercept(chain: Interceptor.Chain): Response {
                        val original = chain.request()
                        val newRequest: Request.Builder = original.newBuilder()
                        newRequest.addHeader("Connection", "close")
                        return chain.proceed(newRequest.build())
                    }
                })

                mediaDUNetworkService = Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .client(mediaOkhttpBuilder.build())
                    .build().create(MediaDUNetworkService::class.java)


                DateTimeUtils.setTimeZone("UTC")
                videoDownloadTracker = VideoDownloadController.getInstance().downloadTracker
                multiTransformation = MultiTransformation(
                    CropTransformation(
                        Utils.dpToPx(IMAGE_SIZE),
                        Utils.dpToPx(IMAGE_SIZE),
                        CropTransformation.CropType.CENTER
                    ),
                    RoundedCornersTransformation(
                        Utils.dpToPx(ROUND_CORNER),
                        0,
                        RoundedCornersTransformation.CornerType.ALL
                    )
                )
                InstallReferralUtil.installReferrer(context)
            }.start()
        }

        private fun getCertificatePins() =
            arrayOf(
                "sha256/dSlQHeoe4vMRu/nWoQvU9oQAMgtJ7ZJBjy8qeERS9BU=",
                "sha256/JSMzqOOrtyOT1kmau6zKhgT676hGgczD5VMdRMyJZFA=",
                "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=",
                "sha256/KwccWaCgrnaw6tsrrSO61FgLacNgG2MMLq8GE6+oP5I="
            )

        fun releaseInstance() {
            fetch = null
            freshChat = null
        }
    }
}

class HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val newRequest: Request.Builder = original.newBuilder()
        if (PrefManager.getStringValue(API_TOKEN).isNotEmpty()) {
            newRequest.addHeader(
                KEY_AUTHORIZATION, "JWT " + PrefManager.getStringValue(API_TOKEN)
            )
        }
        newRequest.addHeader(KEY_APP_VERSION_NAME, BuildConfig.VERSION_NAME)
            .addHeader(KEY_APP_VERSION_CODE, BuildConfig.VERSION_CODE.toString())
            .addHeader(
                KEY_APP_USER_AGENT,
                "APP_" + BuildConfig.VERSION_NAME + "_" + BuildConfig.VERSION_CODE.toString()
            )
            .addHeader(KEY_APP_ACCEPT_LANGUAGE, PrefManager.getStringValue(USER_LOCALE))
        return chain.proceed(newRequest.build())
    }
}


class StatusCodeInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code in 401..403) {
            if (Utils.isAppRunning(
                    AppObjectController.joshApplication,
                    AppObjectController.joshApplication.packageName
                )
            ) {
                PrefManager.logoutUser()
                if (JoshApplication.isAppVisible) {
                    val intent =
                        Intent(AppObjectController.joshApplication, SignUpActivity::class.java)
                    intent.apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("Flow", "StatusCodeInterceptor")
                    }
                    AppObjectController.joshApplication.startActivity(intent)
                }
            }
        }
        WorkManagerAdmin.userActiveStatusWorker(JoshApplication.isAppVisible)
        Timber.i("Status code: %s", response.code)
        return response
    }
}

class NewRelicHttpMetricsLogger : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        val start = System.nanoTime()

        try {
            val requestSize =
                if (null == request.body) 0 else request.body!!.contentLength()
            val response: Response = chain.proceed(request)
            val end = System.nanoTime()

            val responseSize =
                if (null == response.body) 0 else response.body!!.contentLength()
            NewRelic.noticeHttpTransaction(
                request.url.toString(),
                request.method,
                response.code,
                start,
                end,
                requestSize,
                responseSize
            )
            return response
        } catch (ex: HttpException) {
            LogException.catchException(ex)
            val end = System.nanoTime()
            NewRelic.noticeNetworkFailure(request.url.toString(), request.method, start, end, ex)
            return chain.proceed(request)
        }
    }
}

fun initStethoLibrary(context: Context) {
    val cls = Class.forName("com.facebook.stetho.Stetho")
    val m: Method = cls.getMethod("initializeWithDefaults", Context::class.java)
    m.invoke(null, context)
}

fun getStethoInterceptor(): Interceptor {
    val clazz = Class.forName("com.facebook.stetho.okhttp3.StethoInterceptor")
    val ctor: Constructor<*> = clazz.getConstructor()
    return ctor.newInstance() as Interceptor
}

