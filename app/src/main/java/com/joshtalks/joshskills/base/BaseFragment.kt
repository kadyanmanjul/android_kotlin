package com.joshtalks.joshskills.base

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.joshtalks.joshskills.track.TrackFragment

abstract class BaseFragment : TrackFragment() {
    protected var liveData = EventLiveData

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewBinding()
        setArguments()
        initViewState()
    }

    protected abstract fun initViewBinding()
    protected abstract fun initViewState()
    protected abstract fun setArguments()

    protected fun showToast(msg : String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }
}