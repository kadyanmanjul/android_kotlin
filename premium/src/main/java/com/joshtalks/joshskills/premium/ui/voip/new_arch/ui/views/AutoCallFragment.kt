package com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.base.BaseFragment
import com.joshtalks.joshskills.premium.databinding.FragmentAutoCallBinding

class AutoCallFragment : BaseFragment() {
    private lateinit var binding: FragmentAutoCallBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_auto_call, container, false)
        return binding.root
    }

    override fun initViewBinding() {
        TODO("Not yet implemented")
    }

    override fun initViewState() {
        TODO("Not yet implemented")
    }

    override fun setArguments() {
        TODO("Not yet implemented")
    }

}