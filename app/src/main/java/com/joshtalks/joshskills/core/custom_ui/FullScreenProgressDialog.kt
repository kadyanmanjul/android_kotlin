package com.joshtalks.joshskills.core.custom_ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.fragment.app.DialogFragment
import com.joshtalks.joshskills.R

class FullScreenProgressDialog : DialogFragment() {

    var mDialog: Dialog? = null
    var mouse: View? = null
    var background: View? = null
    var color = 0
    private var isClickCancelAble = true


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (mDialog == null) {
            mDialog = Dialog(activity!!, R.style.full_dialog)
            mDialog!!.setContentView(R.layout.progress_dialog_overlay)
            mDialog!!.setCanceledOnTouchOutside(isClickCancelAble)
            mDialog!!.window?.setGravity(Gravity.CENTER)
            val view = mDialog!!.window?.decorView
            background = view?.findViewById(R.id.background)
            if (color != 0) {
                this.background?.setBackgroundColor(color)
            }
        }
        return mDialog!!

    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        mDialog = null
        System.gc()
    }
}