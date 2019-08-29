package com.joshtalks.joshskills.core.custom_ui


import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.joshtalks.joshskills.R

class FullScreenProgressDialog : DialogFragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)
            // dialog.window!!.setWindowAnimations(R.style.AppTheme_Slide)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.progress_dialog_overlay, container, false)
        return view
    }

    companion object {
        val TAG = "progress_dialog"
        fun display(activity: FragmentActivity): FullScreenProgressDialog {
            val exampleDialog = FullScreenProgressDialog()
            exampleDialog.show(activity.supportFragmentManager, TAG)
            return exampleDialog
        }


        fun getDialog(activity: FragmentActivity) = FullScreenProgressDialog()

    }
}