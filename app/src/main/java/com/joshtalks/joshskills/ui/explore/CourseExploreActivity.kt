package com.joshtalks.joshskills.ui.explore

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.appbar.MaterialToolbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.databinding.ActivityCourseExploreBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.ScreenEngagementModel
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.ui.inbox.REGISTER_NEW_COURSE_CODE
import com.joshtalks.joshskills.ui.payment.PaymentActivity
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.ui.view_holders.CourseExplorerViewHolder
import com.vanniktech.emoji.Utils
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


const val COURSE_EXPLORER_SCREEN_NAME = "Course Explorer"
const val USER_COURSES = "user_courses"

class CourseExploreActivity : CoreJoshActivity() {
    private var compositeDisposable = CompositeDisposable()
    private lateinit var courseExploreBinding: ActivityCourseExploreBinding
    var screenEngagementModel: ScreenEngagementModel =
        ScreenEngagementModel(COURSE_EXPLORER_SCREEN_NAME)

    companion object {
        fun startCourseExploreActivity(
            context: Activity,
            requestCode: Int,
            list: MutableSet<InboxEntity>?, clearBackStack: Boolean = false
        ) {
            val intent = Intent(context, CourseExploreActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            list?.run {
                intent.putExtra(USER_COURSES, ArrayList(this))
            }
            if (clearBackStack) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivityForResult(intent, requestCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        courseExploreBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_course_explore)
        courseExploreBinding.lifecycleOwner = this
        //  initActivityAnimation()
        initRV()
        initView()
        loadCourses()
    }

    private fun initView() {
        val titleView = findViewById<AppCompatTextView>(R.id.text_message_title)
        titleView.text = getString(R.string.explorer_course)
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            onCancelResult()
        }
        findViewById<MaterialToolbar>(R.id.toolbar).inflateMenu(R.menu.logout_menu)
        findViewById<MaterialToolbar>(R.id.toolbar).setOnMenuItemClickListener {
            if (it?.itemId == R.id.menu_logout) {
                MaterialDialog(this@CourseExploreActivity).show {
                    message(R.string.logout_message)
                    positiveButton(R.string.ok) {
                        val intent =
                            Intent(AppObjectController.joshApplication, SignUpActivity::class.java)
                        intent.apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            PrefManager.clearUser()
                            AppObjectController.joshApplication.startActivity(intent)
                        }
                    }
                    negativeButton(R.string.cancel)
                }
            }
            return@setOnMenuItemClickListener true
        }

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

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: List<CourseExploreModel> =
                    AppObjectController.signUpNetworkService.explorerCourse()
                CoroutineScope(Dispatchers.Main).launch {
                    var list: ArrayList<InboxEntity>? = null
                    if (intent.hasExtra(USER_COURSES) && intent.getSerializableExtra(USER_COURSES) != null) {
                        list = intent.getSerializableExtra(USER_COURSES) as ArrayList<InboxEntity>?
                    }
                    response.forEach { courseExploreModel ->
                        list?.let { it ->
                            val entity: InboxEntity? =
                                it.find { it.courseId == courseExploreModel.course.toString() }
                            if (entity != null) {
                                return@forEach
                            }
                        }
                        courseExploreBinding.recyclerView.addView(
                            CourseExplorerViewHolder(
                                courseExploreModel
                            )
                        )
                    }
                    courseExploreBinding.progressBar.visibility = View.GONE
                }

            } catch (ex: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    courseExploreBinding.progressBar.visibility = View.GONE
                }
                ex.printStackTrace()
            }
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
            PaymentActivity.startPaymentActivity(this, REGISTER_NEW_COURSE_CODE, it)
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
        onCancelResult()
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

    private fun onCancelResult() {
        val resultIntent = Intent()
        setResult(Activity.RESULT_CANCELED, resultIntent)
        finish()
    }

}
