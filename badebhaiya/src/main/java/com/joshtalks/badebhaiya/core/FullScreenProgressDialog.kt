package com.joshtalks.badebhaiya.core

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.joshtalks.badebhaiya.R

class FullScreenProgressDialog : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window?.setLayout(width, height)
            dialog.setCanceledOnTouchOutside(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.progress_dialog_overlay, container, false)
        isCancelable = true
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return rootView
    }

    companion object {
        fun showProgressBar(activity: FragmentActivity) {
            try {
                val fm = activity.supportFragmentManager
                val dialog = fm.findFragmentByTag(FullScreenProgressDialog::class.java.name)
                if (dialog != null && dialog is FullScreenProgressDialog) {
                    return
                }
                val dialogFragment = FullScreenProgressDialog()
                dialogFragment.show(fm, FullScreenProgressDialog::class.java.name)
            } catch (ignored: Exception) {
                ignored.printStackTrace()
            }
        }

        fun hideProgressBar(activity: FragmentActivity) {
            try {
                val fm = activity.supportFragmentManager
                val dialog = fm.findFragmentByTag(FullScreenProgressDialog::class.java.name)
                if (dialog != null && dialog is FullScreenProgressDialog) {
                    dialog.dismiss()
                }
            } catch (ignored: Exception) {
                ignored.printStackTrace()
            }
        }
    }
}