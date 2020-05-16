package com.joshtalks.joshskills.ui.help

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.commit
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.HelpRequestEventBus
import com.joshtalks.joshskills.repository.server.TypeOfHelpModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class HelpActivity : CoreJoshActivity() {
    private var compositeDisposable = CompositeDisposable()
    private lateinit var  appAnalytics:AppAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        setToolbar()
        openListOfHelp()
        appAnalytics=AppAnalytics.create(AnalyticsEvent.HELP_INITIATED.NAME)
            .addBasicParam()
            .addUserDetails()

    }

    private fun setToolbar() {
        findViewById<View>(R.id.iv_help).visibility = View.GONE
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<AppCompatImageView>(R.id.iv_back).setOnClickListener {
            onBackPressed()
        }
    }


    private fun openListOfHelp() {
        supportFragmentManager.commit(true) {
            addToBackStack(HelpListFragment::class.java.name)
            add(
                R.id.container,
                HelpListFragment.newInstance(),
                HelpListFragment::class.java.name
            )
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 1) {
            appAnalytics.addParam(AnalyticsEvent.HELP_BACK_CLICKED.NAME,true).push()
            this@HelpActivity.finish()
            return
        }
        super.onBackPressed()

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
        compositeDisposable.add(
            RxBus2.listen(HelpRequestEventBus::class.java).subscribeOn(Schedulers.io()).observeOn(
                AndroidSchedulers.mainThread()
            ).subscribe {
                if (it.typeOfHelpModel.type == "form") {
                    appAnalytics.addParam(AnalyticsEvent.HELP_CATEGORY_CLICKED.NAME,AnalyticsEvent.HELP_COMPLAINT_FOAM.NAME)
                    compliantScreen(it.typeOfHelpModel)

                } else {
                    Utils.call(this@HelpActivity, it.typeOfHelpModel.mobile)
                    appAnalytics.addParam(AnalyticsEvent.HELP_CATEGORY_CLICKED.NAME,AnalyticsEvent.CLICK_HELPLINE_SELECTED.NAME)
                    appAnalytics.addParam(AnalyticsEvent.CALL_HELPLINE.NAME,"HelpLine Called ${it.typeOfHelpModel.mobile}")

                }
            })
    }

    private fun compliantScreen(typeOfHelpModel: TypeOfHelpModel) {
        supportFragmentManager.commit(true) {
            addToBackStack(ComplaintFragment::class.java.name)
            replace(
                R.id.container,
                ComplaintFragment.newInstance(typeOfHelpModel),
                ComplaintFragment::class.java.name
            )
        }
    }

}
