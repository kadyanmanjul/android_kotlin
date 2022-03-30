package com.joshtalks.joshskills.base

import android.os.Bundle
import android.os.Message
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity() {

    protected var event = EventLiveData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getArguments()
        initViewBinding()
        onCreated()
        initViewState()
    }

    protected abstract fun initViewBinding()
    protected abstract fun onCreated()
    protected abstract fun initViewState()
    protected open fun getArguments(){}

    protected fun showToast(msg : String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}