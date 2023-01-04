package com.joshtalks.joshskills.buypage.new_buy_page_layout.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.buypage.databinding.FragmentCoupanCardBinding
import com.joshtalks.joshskills.buypage.new_buy_page_layout.viewmodel.BuyPageViewModel
import com.joshtalks.joshskills.common.base.BaseFragment
import com.joshtalks.joshskills.common.ui.special_practice.utils.*

class CouponCardFragment : BaseFragment() {

    lateinit var binding: FragmentCoupanCardBinding

    private val vm by lazy {
        ViewModelProvider(requireActivity())[BuyPageViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCoupanCardBinding.inflate(inflater, container, false)
        binding.vm = vm
        binding.executePendingBindings()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addObserver()
    }

    override fun initViewBinding() {

    }

    override fun initViewState() {
        liveData.observe(this) {
            when (it.what) {

            }
        }

        binding.proceedBtn.setOnClickListener {
            vm.applyEnteredCoupon(binding.enteredAmountTv.text.toString(), 0)
        }
    }

    override fun setArguments() {

    }

    fun addObserver() {
        vm.getValidCouponList(COUPON, Integer.parseInt(vm.testId), isCouponApplyOrRemove = REMOVE)
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CouponCardFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}