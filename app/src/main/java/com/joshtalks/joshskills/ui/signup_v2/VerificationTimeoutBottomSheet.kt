package com.joshtalks.joshskills.ui.signup_v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.VerificationVia
import com.joshtalks.joshskills.core.interfaces.OnSelectVerificationMethodListener
import com.joshtalks.joshskills.databinding.BottomSheetVerificationTimeoutBinding

class VerificationTimeoutBottomSheet : BottomSheetDialogFragment() {

    companion object {
        @JvmStatic
        fun newInstance() = VerificationTimeoutBottomSheet()
    }

    private lateinit var binding: BottomSheetVerificationTimeoutBinding
    private var listener: OnSelectVerificationMethodListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BaseBottomSheetDialog)
        parentFragment?.run {
            listener = this as OnSelectVerificationMethodListener
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.bottom_sheet_verification_timeout,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.handler = this

        val baseDialog = dialog
        if (baseDialog is BottomSheetDialog) {
            val behavior: BottomSheetBehavior<*> = baseDialog.behavior
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }
            })
        }
        return binding.root
    }

    fun flashCallVerify() {
        listener?.onSelect(VerificationVia.FLASH_CALL)
        cancel()
    }

    fun smsVerify() {
        listener?.onSelect(VerificationVia.SMS)
        cancel()
    }

    fun cancel() {
        try {
            val baseDialog = dialog
            if (baseDialog is BottomSheetDialog) {
                val behavior: BottomSheetBehavior<*> = baseDialog.behavior
                behavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        } catch (ex: Exception) {
            dismissAllowingStateLoss()
            ex.printStackTrace()
        }
    }
}

