package com.joshtalks.joshskills.ui.lesson

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.databinding.PurchaseCourseDialogBinding

class PurchaseDialog: BottomSheetDialogFragment()  {

    private lateinit var binding: PurchaseCourseDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = PurchaseCourseDialogBinding.inflate(inflater,container,false)
        return binding.root
    }
}