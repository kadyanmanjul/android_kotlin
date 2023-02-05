package com.joshtalks.joshskills.premium.ui.errorState

import android.os.Bundle
import android.view.*
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.databinding.ErrorStateBinding

/**
This Dialog is created to show Error State.
 */

class ErrorStateDialog(
    @DrawableRes private val icon: Int,
    private val errorCode: String,
    private val errorTitle: String,
    private val errorSubtitle: String,
    private val onActionClick: () -> Unit = {}
) : DialogFragment() {

    private lateinit var binding: ErrorStateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BaseBottomSheetDialogBlank)
        changeDialogConfiguration()
    }

    private fun changeDialogConfiguration() {
        val params: WindowManager.LayoutParams? = dialog?.window?.attributes
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        params?.height = WindowManager.LayoutParams.WRAP_CONTENT
        params?.gravity = Gravity.BOTTOM
        dialog?.window?.attributes = params
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ErrorStateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            errorImg.setImageResource(icon)
            errorCode.text = this@ErrorStateDialog.errorCode
            errorTitle.text = this@ErrorStateDialog.errorTitle
            errorSubTitle.text = this@ErrorStateDialog.errorSubtitle
            actionButton.setOnClickListener {
                dismiss()
                onActionClick.invoke()
            }
        }
    }

    companion object {
        const val TAG = "ErrorStateDialog"

        fun showFullScreen(
            @DrawableRes icon: Int,
            errorCode: String = "",
            errorTitle: String,
            errorSubtitle: String,
            fragmentManager: FragmentManager,
            onActionClick: () -> Unit = {}
        ) {
            ErrorStateDialog(
                icon = icon,
                errorCode = errorCode,
                errorTitle = errorTitle,
                errorSubtitle = errorSubtitle,
                onActionClick = onActionClick
            ).show(fragmentManager, TAG)
        }

        fun showBelowToolbar(
            @DrawableRes icon: Int,
            errorCode: String = "",
            errorTitle: String,
            errorSubtitle: String,
            fragmentManager: FragmentManager,
            @IdRes container: Int,
            onActionClick: () -> Unit = {}
        ) {
            fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_down)
                .add(
                    container, ErrorStateDialog(
                    icon = icon,
                    errorCode = errorCode,
                    errorTitle = errorTitle,
                    errorSubtitle = errorSubtitle,
                    onActionClick
                ), TAG
            )
                .commit()
        }
    }

}