package com.joshtalks.joshskills.ui.special_practice

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.databinding.FragmentRecordVideoBinding

class RecordVideoFragment : CoreJoshFragment() {
    private lateinit var binding: FragmentRecordVideoBinding

    companion object {
        fun newInstance(): RecordVideoFragment {
            return RecordVideoFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecordVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recordVideoBtn.setOnClickListener {

        }
    }

}