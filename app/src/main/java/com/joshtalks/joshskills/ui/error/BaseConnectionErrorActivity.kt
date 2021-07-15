package com.joshtalks.joshskills.ui.error

import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.WebRtcMiddlewareActivity
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.ConnectionErrorRetryEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

abstract class BaseConnectionErrorActivity : WebRtcMiddlewareActivity() {
    protected val compositeDisposable = CompositeDisposable()
    protected var internetAvailableFlag: Boolean = true

    override fun onResume() {
        super.onResume()
        //observeNetwork()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    abstract fun isInternetAvailable(isInternetAvailable: Boolean)

    abstract fun onRetry()

    private fun observeNetwork() {
        compositeDisposable.add(
            ReactiveNetwork.observeNetworkConnectivity(applicationContext)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { connectivity ->
                    isInternetAvailable(connectivity.available())

                }
        )

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(ConnectionErrorRetryEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        onRetry()
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
    }

    fun isApiFalied(isApiSuccess: Boolean, container: FrameLayout, stringId:Int?=null) {
        if(isApiSuccess){
            if (container.isVisible){
                container.visibility = View.GONE
                supportFragmentManager.popBackStack()
                onRetry()
            }
        } else {
            if (container.isVisible.not()){
                container.visibility = View.VISIBLE
                supportFragmentManager
                    .beginTransaction()
                    .add(
                        container.id,
                        ConnectionErrorDialogFragment.newInstance(getString(stringId?: R.string.connection_error)),
                        ConnectionErrorDialogFragment.TAG
                    )
                    .commitAllowingStateLoss()
            }
        }
    }
}
