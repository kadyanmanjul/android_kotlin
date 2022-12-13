package com.joshtalks.joshskills.ui.errorState

import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ErrorStateBinding

/**
This Dialog is created to show Error State.
*/

class ErrorStateDialog(
    private val onActionClick: () -> Unit = {}
): DialogFragment() {

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
            actionButton.setOnClickListener {
                dismiss()
                onActionClick.invoke()
            }
        }
    }

    companion object {
        const val TAG = "ErrorStateDialog"

        fun show(fragmentManager: FragmentManager, onActionClick: () -> Unit = {}){
            ErrorStateDialog(onActionClick).show(fragmentManager, TAG)
        }
    }

}