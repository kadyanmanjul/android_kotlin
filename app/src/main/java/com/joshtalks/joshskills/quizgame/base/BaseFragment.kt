package com.joshtalks.joshskills.quizgame.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.track.TrackFragment

abstract class BaseFragment : TrackFragment() {

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