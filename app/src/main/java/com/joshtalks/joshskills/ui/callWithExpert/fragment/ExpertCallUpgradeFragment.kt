package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.databinding.FragmentExpertCallUpgradeBinding

class ExpertCallUpgradeFragment : BaseFragment() {

    private lateinit var binding: FragmentExpertCallUpgradeBinding

    override fun initViewBinding() {
//        TODO("Not yet implemented")
    }

    override fun initViewState() {
//        TODO("Not yet implemented")
    }

    override fun setArguments() {
//        TODO("Not yet implemented")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExpertCallUpgradeBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().findViewById<TextView>(R.id.iv_earn).setOnClickListener {
            findNavController().navigate(R.id.action_expertCallUpgrade_to_walletFragment)
        }
    }

}