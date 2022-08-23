package com.joshtalks.joshskills.ui.callWithExpert.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentExpertListBinding
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.ExpertListViewModel

class ExpertListFragment:Fragment() {
    private lateinit var binding: FragmentExpertListBinding
    val expertListViewModel by lazy {
        ViewModelProvider(requireActivity())[ExpertListViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExpertListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.vm = expertListViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        expertListViewModel.getListOfExpert()
        requireActivity().findViewById<TextView>(R.id.iv_earn).setOnClickListener {
            findNavController().navigate(R.id.action_expertListFragment_to_walletFragment)
        }
    }

}