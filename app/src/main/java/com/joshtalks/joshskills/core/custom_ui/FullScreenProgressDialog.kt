package com.joshtalks.joshskills.core.custom_ui


import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R

class FullScreenProgressDialog : DialogFragment() {

    private var bgColor: String = "#FF000000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            savedInstanceState.getString(BG_COLOR)?.let {
                bgColor = it
            }
        }
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
        view.setBackgroundColor(Color.parseColor(bgColor))
        return view
    }

    companion object {

        val FULL_SIZE_KEY = "full_size"
        val BG_COLOR = "bg_color"

        @Volatile
        private var INSTANCE: FullScreenProgressDialog? = null


        @JvmStatic
        private fun newInstance() =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: FullScreenProgressDialog().also { INSTANCE = it }
            }


        fun display(
            activity: FragmentActivity,
            bgColor: String = "#80000000"
        ): FullScreenProgressDialog {
            INSTANCE = newInstance()
            INSTANCE?.apply {
                arguments = Bundle().apply {
                    putString(BG_COLOR, bgColor)
                }
            }
            INSTANCE?.show(activity.supportFragmentManager, "Dialog")
            return INSTANCE as FullScreenProgressDialog
        }

        fun hide() {
            INSTANCE?.dismiss()
        }


        fun getDialog(activity: FragmentActivity) = FullScreenProgressDialog()
    }
}
