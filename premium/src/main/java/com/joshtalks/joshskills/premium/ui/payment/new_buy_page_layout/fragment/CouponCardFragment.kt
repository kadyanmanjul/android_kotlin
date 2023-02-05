package com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.premium.base.BaseFragment
import com.joshtalks.joshskills.premium.databinding.FragmentCoupanCardBinding
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.COUPON_APPLY_POP_UP_SHOW_AND_BACK
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.viewmodel.BuyPageViewModel
import com.joshtalks.joshskills.premium.ui.special_practice.utils.COUPON
import com.joshtalks.joshskills.premium.ui.special_practice.utils.REMOVE

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
            vm.applyEnteredCoupon(binding.enteredAmountTv.text.toString(), COUPON_APPLY_POP_UP_SHOW_AND_BACK, 1)
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