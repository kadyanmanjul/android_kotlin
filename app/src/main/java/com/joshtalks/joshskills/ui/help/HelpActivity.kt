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
                    compliantScreen(it.typeOfHelpModel)
                } else {
                    Utils.call(this@HelpActivity, it.typeOfHelpModel.mobile)
                    AppAnalytics.create(AnalyticsEvent.CALL_HELPLINE.NAME).push()

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
