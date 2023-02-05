package com.joshtalks.joshskills.premium.base

import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.fragment.app.DialogFragment
import com.joshtalks.joshskills.premium.R

open class BaseDialogFragment(private val isBackGroundTransparent: Boolean = false) : DialogFragment() {

    @Keep
    public class BaseDialogFragment(){}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isBackGroundTransparent){
            setStyle(STYLE_NORMAL, R.style.dialog_transparent_theme)
        } else {
            setStyle(STYLE_NORMAL, R.style.dialog_theme)
        }
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