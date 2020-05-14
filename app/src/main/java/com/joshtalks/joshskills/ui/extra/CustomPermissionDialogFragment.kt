package com.joshtalks.joshskills.ui.extra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R

class CustomPermissionDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_custom_permission_dialog, container, false)
    }

    companion object {
        fun newInstance(): CustomPermissionDialogFragment {
            return CustomPermissionDialogFragment()
        }
    }
}
