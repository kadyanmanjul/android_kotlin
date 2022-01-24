package com.joshtalks.joshskills.base

import android.app.ProgressDialog
import android.content.Context
import android.os.Message
import androidx.lifecycle.ViewModel
import com.joshtalks.joshskills.R

open class BaseViewModel : ViewModel() {
    protected var message = Message()

    protected var singleLiveEvent = EventLiveData

    var progressDialog: ProgressDialog? = null

    fun showProgressDialog(context: Context, msg: String) {
        progressDialog = ProgressDialog(context, R.style.AlertDialogStyle)
        progressDialog?.setCancelable(false)
        progressDialog?.setMessage(msg)
        progressDialog?.show()
    }

    fun dismissProgressDialog() = progressDialog?.dismiss()
}