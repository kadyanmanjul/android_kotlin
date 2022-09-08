package com.joshtalks.joshskills.ui.callWithExpert.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.ui.callWithExpert.fragment.PaymentLogsFragment
import com.joshtalks.joshskills.ui.callWithExpert.fragment.WalletTransactions
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.WalletTransactionViewModel

class ViewPagerAdapter(fragmentManager: FragmentManager,lifecycle: Lifecycle,val viewModel: WalletTransactionViewModel): FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0->{
                WalletTransactions(viewModel)
            }
            1->{
                PaymentLogsFragment(viewModel)
            }
            else -> {
                Fragment()
            }
        }
    }
}