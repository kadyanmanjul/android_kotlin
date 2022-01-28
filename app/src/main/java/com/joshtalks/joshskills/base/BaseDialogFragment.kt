package com.joshtalks.joshskills.base

import android.os.Bundle
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.joshtalks.joshskills.R

open class BaseDialogFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.dialog_theme)
    }

    override fun onStart() {
        super.onStart()

        val d = dialog
        d?.let {
            val width = ViewGroup.LayoutParams.WRAP_CONTENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            d.window?.setLayout(width, height)
        }

    }
}