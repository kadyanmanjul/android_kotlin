package com.joshtalks.joshskills.base

import android.app.ProgressDialog
import android.content.Context
import android.os.Message
import androidx.lifecycle.ViewModel

open class BaseViewModel : ViewModel() {
    protected var message = Message()

    protected var singleLiveEvent = EventLiveData

    lateinit var progressDialog : ProgressDialog

    fun showProgressDialog(context: Context, msg: String) {
        progressDialog = ProgressDialog(context)
        progressDialog.setCancelable(true)
        progressDialog.setMessage(msg)
        progressDialog.show()
    }

    fun dismissProgressDialog() = progressDialog.dismiss()
}