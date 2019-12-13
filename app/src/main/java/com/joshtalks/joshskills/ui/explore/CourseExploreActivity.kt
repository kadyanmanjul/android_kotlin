package com.joshtalks.joshskills.ui.explore

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.browser.customtabs.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.chrome.CustomTabsHelper
import com.joshtalks.joshskills.core.chrome.ServiceConnection
import com.joshtalks.joshskills.core.chrome.ServiceConnectionCallback
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.databinding.ActivityCourseExploreBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.CourseExploreModel
import com.joshtalks.joshskills.repository.local.model.ScreenEngagementModel
import com.joshtalks.joshskills.ui.inbox.REGISTER_NEW_COURSE_CODE
import com.joshtalks.joshskills.ui.view_holders.CourseExplorerViewHolder
import com.r0adkll.slidr.Slidr
import com.vanniktech.emoji.Utils
import io.reactivex.disposables.CompositeDisposable


const val COURSE_EXPLORER_SCREEN_NAME = "Course Explorer"
const val USER_COURSES = "user_courses"

class CourseExploreActivity : CoreJoshActivity(), ServiceConnectionCallback {
    private var compositeDisposable = CompositeDisposable()
    private lateinit var courseExploreBinding: ActivityCourseExploreBinding
    var screenEngagementModel: ScreenEngagementModel =
        ScreenEngagementModel(COURSE_EXPLORER_SCREEN_NAME)
    private var mCustomTabsSession: CustomTabsSession? = null
    private var mClient: CustomTabsClient? = null
    private var mConnection: CustomTabsServiceConnection? = null
    private var mPackageNameToBind: String? = null


    companion object {
        fun startCourseExploreActivity(
            context: Activity,
            requestCode: Int,
            list: MutableSet<InboxEntity>
        ) {
            val intent = Intent(context, CourseExploreActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            intent.putExtra(USER_COURSES, ArrayList(list))
            context.startActivityForResult(intent, requestCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }
        super.onCreate(savedInstanceState)
        courseExploreBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_course_explore)
        courseExploreBinding.lifecycleOwner = this
        initActivityAnimation()
        initRV()
        initView()
        loadCourses()
    }

    private fun initView() {
        val titleView = findViewById<AppCompatTextView>(R.id.text_message_title)
        titleView.text = getString(R.string.discover)
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            this@CourseExploreActivity.finish()
        }
    }

    private fun initActivityAnimation() {
        val primary = ContextCompat.getColor(applicationContext, R.color.colorPrimaryDark)
        val secondary = ContextCompat.getColor(applicationContext, R.color.colorPrimary)
        Slidr.attach(this, primary, secondary)
    }

    private fun initRV() {
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        courseExploreBinding.recyclerView.builder
            .setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
        courseExploreBinding.recyclerView.itemAnimator = null
        courseExploreBinding.recyclerView.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    this,
                    8f
                )
            )
        )
    }

    private fun loadCourses() {
        try {
            val typeToken = object : TypeToken<List<CourseExploreModel>>() {}.type
            val quickRepliesModelList =
                AppObjectController.gsonMapperForLocal.fromJson<List<CourseExploreModel>>(
                    AppObjectController.getFirebaseRemoteConfig().getString("course_explorer_list"),
                    typeToken
                )
            @Suppress("UNCHECKED_CAST") val list: ArrayList<InboxEntity>? =
                intent.getSerializableExtra(USER_COURSES) as ArrayList<InboxEntity>

            quickRepliesModelList.sortedWith(compareBy { it.order }).filter { it.active }
                .forEach { objQR ->
                    list?.let {
                        val entity: InboxEntity? = it.find { it.courseId == objQR.course_id }
                        if (entity != null) {
                            return@forEach
                        }
                    }
                    courseExploreBinding.recyclerView.addView(CourseExplorerViewHolder(objQR))
                }
        } catch (ex: Exception) {
        }
    }


    override fun onResume() {
        super.onResume()
        Runtime.getRuntime().gc()
        addObserver()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    private fun addObserver() {
        compositeDisposable.add(RxBus2.listen(CourseExploreModel::class.java).subscribe {

            openWebBrowser(it.url)
/*
            PaymentActivity.startPaymentActivity(
                this, REGISTER_NEW_COURSE_CODE,
                it.url, it.name
            )*/
        })


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REGISTER_NEW_COURSE_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val resultIntent = Intent()
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    override fun onBackPressed() {
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
            .addParam("name", javaClass.simpleName)
            .push()
        super.onBackPressed()
    }


    override fun onStart() {
        super.onStart()
        screenEngagementModel.startTime = System.currentTimeMillis()
    }

    override fun onStop() {
        screenEngagementModel.endTime = System.currentTimeMillis()
        WorkMangerAdmin.screenAnalyticsWorker(screenEngagementModel)
        super.onStop()
    }


    private fun openWebBrowser(url: String) {

        bindCustomTabsService()
        val session: CustomTabsSession? = getSession()
        val builder = CustomTabsIntent.Builder(session)
        builder.setToolbarColor(
            ContextCompat.getColor(
                applicationContext,
                R.color.colorPrimaryDark
            )
        )
        builder.setShowTitle(true)
        prepareMenuItems(builder)
        prepareActionButton(builder)
        builder.setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left)
        builder.setExitAnimations(this, R.anim.slide_in_left, R.anim.slide_out_right)

       // builder.setCloseButtonIcon(com.joshtalks.joshskills.core.Utils.getBitmapFromDrawable(applicationContext,R.drawable.ic_cam_back))
        val customTabsIntent = builder.build()
        if (session != null) {
            CustomTabsHelper.addKeepAliveExtra(this, customTabsIntent.intent)
        } else {
            if (!TextUtils.isEmpty(mPackageNameToBind)) {
                customTabsIntent.intent.setPackage(mPackageNameToBind)
            }
        }
        customTabsIntent.launchUrl(this, Uri.parse(url))

    }

    private fun bindCustomTabsService() {
        if (mClient != null) return
        if (TextUtils.isEmpty(mPackageNameToBind)) {
            mPackageNameToBind = CustomTabsHelper.getPackageNameToUse(this)
            if (mPackageNameToBind == null) return
        }
        mConnection = ServiceConnection(this)
        val ok =
            CustomTabsClient.bindCustomTabsService(this, mPackageNameToBind, mConnection!!)
        if (ok.not()) {
            mConnection = null
        }
    }

    private fun prepareMenuItems(builder: CustomTabsIntent.Builder) {
        val menuIntent = Intent()
        menuIntent.setClass(applicationContext, this.javaClass)
        // Optional animation configuration when the user clicks menu items.
        val menuBundle = ActivityOptions.makeCustomAnimation(
            this, android.R.anim.slide_in_left,
            android.R.anim.slide_out_right
        ).toBundle()
        val pi = PendingIntent.getActivity(
            applicationContext, 0, menuIntent, 0,
            menuBundle
        )
     //   builder.addMenuItem("Menu entry 1", pi)
    }

    private fun prepareActionButton(builder: CustomTabsIntent.Builder) {

        val actionIntent = Intent(Intent.ACTION_DIAL).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val phoneNumber=AppObjectController.getFirebaseRemoteConfig().getString("helpline_number")
        actionIntent.data = Uri.parse("tel:$phoneNumber")

        val pi =
            PendingIntent.getActivity(this, 0, actionIntent, 0)
        val icon =com.joshtalks.joshskills.core.Utils.getBitmapFromDrawable(applicationContext,R.drawable.ic_local_phone)
        builder.setActionButton(icon, "Call helpline", pi, true)
    }

    private fun getSession(): CustomTabsSession? {
        if (mClient == null) {
            mCustomTabsSession = null
        } else if (mCustomTabsSession == null) {
            mCustomTabsSession =
                mClient?.newSession(NavigationCallback())
            SessionHelper.setCurrentSession(mCustomTabsSession)
        }
        return mCustomTabsSession
    }

    override fun onServiceConnected(client: CustomTabsClient) {
        mClient = client
        Log.e(
            "onServiceConnected",
            "onServiceConnected"
        )
        if (mClient != null)  mClient!!.warmup(0)

    }


    override fun onServiceDisconnected() {
        Log.e(
            "onServiceConnected",
            "onServiceDisconnected"
        )
        mClient = null
    }

    private class NavigationCallback : CustomTabsCallback() {
        override fun onNavigationEvent(
            navigationEvent: Int,
            extras: Bundle?
        ) {
            Log.e(
                "webview",
                "onNavigationEvent: Code = $navigationEvent"
            )
        }

        override fun extraCallback(callbackName: String, args: Bundle?) {
            Log.e("extraCallback","extraCallback")

            super.extraCallback(callbackName, args)
        }
    }



}
