package com.joshtalks.joshskills.ui.special_practice.base

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.track.TrackFragment

abstract class BaseKFactorFragment : TrackFragment() {
    protected var liveData = EventLiveData

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewBinding()
        setArguments()
        initViewState()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        })
    }

    protected abstract fun initViewBinding()
    protected abstract fun initViewState()
    protected abstract fun setArguments()
    abstract fun onBackPressed()

    protected fun showToast(msg : String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }
}