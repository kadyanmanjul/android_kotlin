package com.joshtalks.joshskills.premium.ui.special_practice.base

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Toast
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.base.EventLiveData
import com.joshtalks.joshskills.premium.core.CoreJoshActivity

abstract class BaseKFactorActivity : CoreJoshActivity() {
    protected var event = EventLiveData

    var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setIntentExtras()
        initViewBinding()
        onCreated()
        initViewState()
    }

    protected abstract fun setIntentExtras()
    protected abstract fun initViewBinding()
    protected abstract fun onCreated()
    protected abstract fun initViewState()

    fun showProgressDialog(msg: String) {
        progressDialog = ProgressDialog(this, R.style.AlertDialogStyle)
        progressDialog?.setCancelable(false)
        progressDialog?.setMessage(msg)
        progressDialog?.show()
    }

    fun dismissProgressDialog() = progressDialog?.dismiss()

    protected fun showToast(msg : String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}