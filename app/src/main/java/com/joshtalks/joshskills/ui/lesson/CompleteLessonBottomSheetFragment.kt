package com.joshtalks.joshskills.ui.lesson

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.databinding.CompleteLessonDialogBinding

class CompleteLessonBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: CompleteLessonDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CompleteLessonDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cross.setOnClickListener {
            dismissAllowingStateLoss()
            activity?.finish()
        }
    }

    companion object{
        @JvmStatic
        fun newInstance():CompleteLessonBottomSheetFragment{
            val fragment = CompleteLessonBottomSheetFragment()
            fragment.isCancelable = false
            return fragment
        }
    }
}