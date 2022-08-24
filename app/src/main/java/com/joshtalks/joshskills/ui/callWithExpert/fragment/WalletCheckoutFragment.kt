package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentWalletCheckoutBinding
import com.joshtalks.joshskills.ui.callWithExpert.constant.AMOUNT
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.WalletCheckoutViewModel

class WalletCheckoutFragment : Fragment() {

    private lateinit var binding: FragmentWalletCheckoutBinding

    private val viewModel by lazy {
        ViewModelProvider(this)[WalletCheckoutViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletCheckoutBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = this.viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.updateAddedAmount(requireArguments().getString(AMOUNT, ""))
        with(binding){
        }
    }
}