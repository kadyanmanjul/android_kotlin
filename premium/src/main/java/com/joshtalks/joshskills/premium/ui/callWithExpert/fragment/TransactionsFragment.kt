package com.joshtalks.joshskills.premium.ui.callWithExpert.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.databinding.FragmentTransactionsBinding
import com.joshtalks.joshskills.premium.ui.callWithExpert.adapter.ViewPagerAdapter
import com.joshtalks.joshskills.premium.ui.callWithExpert.viewModel.WalletTransactionViewModel

class TransactionsFragment : Fragment() {

    private lateinit var binding: FragmentTransactionsBinding

    private val viewModel by lazy {
        ViewModelProvider(this)[WalletTransactionViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ViewPagerAdapter(childFragmentManager, lifecycle)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayoutTransaction, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = activity?.getString(R.string.wallet_transactions)
                }
                1 -> {
                    tab.text = activity?.getString(R.string.payment_logs)
                }
            }
        }.attach()
    }

    fun onRechargeClicked(v: View) {
        requireActivity().onBackPressed()
    }
}