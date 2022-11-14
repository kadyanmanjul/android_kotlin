package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.os.Bundle
import android.os.UserManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseDialogFragment
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.FragmentRechargeSuccessBinding
import com.joshtalks.joshskills.repository.local.entity.User
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.callWithExpert.model.ExpertListModel
import com.joshtalks.joshskills.ui.callWithExpert.utils.WalletRechargePaymentManager
import com.joshtalks.joshskills.ui.callWithExpert.utils.gone
import com.joshtalks.joshskills.ui.callWithExpert.utils.toRupees
import com.joshtalks.joshskills.ui.callWithExpert.utils.visible

class RechargeSuccessFragment : BaseDialogFragment(isBackGroundTransparent = true) {

    private var amount: Int = 0
    private var isGifted: Boolean = false
    private lateinit var binding: FragmentRechargeSuccessBinding
    private var selectedUser :ExpertListModel?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedUser = WalletRechargePaymentManager.selectedExpertForCall
        arguments?.let {
            amount = it.getInt(AMOUNT)
            isGifted = it.getBoolean(IS_GIFTED)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRechargeSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textView.text = getString(R.string.added_to_your_account, amount.toRupees())
        if (isGifted) {
            binding.textContinueCall.visible()
            binding.textContinueCall.text = getString(R.string.wallet_gift_description, "${Mentor.getInstance().getUser()?.firstName},", amount.toString())
            binding.btnThanksAndOk.text = getString(R.string.thankyou_sir)
        } else {
            binding.textContinueCall.gone()
            binding.btnThanksAndOk.text = getString(R.string.ok)
        }

        binding.btnThanksAndOk.setOnClickListener {
            if (binding.btnThanksAndOk.text == getString(R.string.ok) && selectedUser!=null) {
                CallContinueDialog.open(requireActivity().supportFragmentManager)
            }else{
                if (binding.btnThanksAndOk.text != getString(R.string.thankyou_sir) && WalletRechargePaymentManager.isWalletOrUpgradePaymentType != "Upgrade") {
                    activity?.onBackPressed()
                }
                dismiss()
            }
            dismiss()
        }
    }

    companion object {
        const val TAG = ""
        const val AMOUNT = "amount"
        const val IS_GIFTED = "is_gifted"

        @JvmStatic
        fun newInstance(amount: Int, isGifted: Boolean = false) =
            RechargeSuccessFragment().apply {
                arguments = Bundle().apply {
                    putInt(AMOUNT, amount)
                    putBoolean(IS_GIFTED, isGifted)
                }
            }

        fun open(supportFragmentManager: FragmentManager, amount: Int, isGifted: Boolean = false, type: String = EMPTY){
            newInstance(amount, isGifted).show(supportFragmentManager, "RechargeSuccessFragment")
        }

    }
}