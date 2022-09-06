package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.databinding.FragmentTransactionsBinding
import com.joshtalks.joshskills.ui.callWithExpert.adapter.ViewPagerAdapter
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.WalletViewModel

class TransactionsFragment : Fragment() {

    private lateinit var binding:FragmentTransactionsBinding

    private val viewModel by lazy {
        ViewModelProvider(this)[WalletViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTransactionsBinding.inflate(inflater,container,false)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = viewModel
        return  binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = activity?.supportFragmentManager?.let { ViewPagerAdapter(it,lifecycle) }
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayoutTransaction,binding.viewPager){tab,position->
            when(position){
                0->{
                    tab.text = "Wallet Transactions"
                }
                1->{
                    tab.text = "Payment Logs"
                }
            }
        }.attach()
    }
    fun onRechargeClicked(v:View){
        requireActivity().onBackPressed()
    }
}