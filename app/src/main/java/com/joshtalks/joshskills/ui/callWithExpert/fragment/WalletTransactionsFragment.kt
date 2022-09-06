package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentWalletTransactionsBinding
import com.joshtalks.joshskills.ui.callWithExpert.adapter.TransactionsAdapter
import com.joshtalks.joshskills.ui.callWithExpert.model.cardDetails

class WalletTransactions : Fragment() {

    private lateinit var binding:FragmentWalletTransactionsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= FragmentWalletTransactionsBinding.inflate(inflater, container, false)
        val list = mutableListOf<cardDetails>()
        list.add(cardDetails("est","test","testt","-21","test"))
        list.add(cardDetails("est","test","testt","+21","test"))
        list.add(cardDetails("est","test","testt","-21","test"))
        binding.rvHistory.adapter = TransactionsAdapter(list)
        return binding.root
    }
}