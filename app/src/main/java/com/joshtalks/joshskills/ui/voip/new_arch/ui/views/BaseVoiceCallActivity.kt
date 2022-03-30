package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

abstract class BaseVoiceCallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewBinding()
    }

    abstract fun initViewBinding()

    abstract fun onCreated()
}