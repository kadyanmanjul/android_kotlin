package com.joshtalks.joshskills.ui.explore

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.databinding.ActivityCourseExploreBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.model.CourseExploreModel
import com.joshtalks.joshskills.ui.inbox.REGISTER_NEW_COURSE_CODE
import com.joshtalks.joshskills.ui.payment.PaymentActivity
import com.joshtalks.joshskills.ui.view_holders.CourseExplorerViewHolder
import com.r0adkll.slidr.Slidr
import io.reactivex.disposables.CompositeDisposable


class CourseExploreActivity : CoreJoshActivity() {
    private var compositeDisposable = CompositeDisposable()
    private lateinit var courseExploreBinding: ActivityCourseExploreBinding


    companion object {
        fun startCourseExploreActivity(context: Activity, requestCode: Int) {
            val intent = Intent(context, CourseExploreActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            context.startActivityForResult(intent, requestCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
        findViewById<View>(R.id.iv_back).visibility=View.VISIBLE
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

        /*courseExploreBinding.recyclerView.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    applicationContext,
                    4f
                )
            )
        )*/
    }

    private fun loadCourses() {
        val typeToken = object : TypeToken<List<CourseExploreModel>>() {}.type
        val quickRepliesModelList =
            AppObjectController.gsonMapperForLocal.fromJson<List<CourseExploreModel>>(
                AppObjectController.getFirebaseRemoteConfig().getString("course_explorer_list"),
                typeToken
            )
        quickRepliesModelList.sortedWith(compareBy { it.order }).filter { it.active }.forEach {
            courseExploreBinding.recyclerView.addView(CourseExplorerViewHolder(it))
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
            WorkMangerAdmin.buyNowEventWorker(it.name)
            PaymentActivity.startPaymentActivity(
                this, REGISTER_NEW_COURSE_CODE,
                it.url, it.name
            )
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
}
