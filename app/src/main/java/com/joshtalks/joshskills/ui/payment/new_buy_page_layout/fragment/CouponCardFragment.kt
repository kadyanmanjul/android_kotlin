package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.databinding.FragmentCoupanCardBinding
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.BuyCourseFeatureModel
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.CourseDetailsList
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.viewmodel.BuyPageViewModel
import com.joshtalks.joshskills.ui.special_practice.utils.*

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
        binding.vm  = vm
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
            vm.applyEnteredCoupon(binding.enteredAmountTv.text.toString())
        }
    }

    override fun setArguments() {

    }

    fun addObserver(){
        vm.getValidCouponList(COUPON,Integer.parseInt(vm.testId))
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