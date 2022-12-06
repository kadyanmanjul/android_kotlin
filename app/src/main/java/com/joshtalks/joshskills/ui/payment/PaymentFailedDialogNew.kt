package com.joshtalks.joshskills.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.joshtalks.joshskills.databinding.DialogPaymentFailedNewBinding
import com.joshtalks.joshskills.ui.paymentManager.PaymentManager

class PaymentFailedDialogNew : DialogFragment() {
    lateinit var binding: DialogPaymentFailedNewBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogPaymentFailedNewBinding.inflate(inflater, container, false)
        binding.executePendingBindings()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnTryAgain.setOnClickListener {
            if (isPaymentManagerObjInit()) {
                paymentManagerObj.getJuspayPayload()
                    ?.let { it1 -> paymentManagerObj.makePaymentForTryAgain(it1) }
                dismiss()
            } else {
                dismiss()
                onCancelClicked.invoke()
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
            onCancelClicked.invoke()
        }
    }

    companion object {
        lateinit var paymentManagerObj: PaymentManager
        lateinit var onCancelClicked: () -> Unit

        @JvmStatic
        fun newInstance(paymentManager: PaymentManager, onCancelClick: () -> Unit = ({})) =
            PaymentFailedDialogNew().apply {
                paymentManagerObj = paymentManager
                onCancelClicked = onCancelClick
                arguments = Bundle().apply {

                }
            }

        fun isPaymentManagerObjInit(): Boolean {
            return this::paymentManagerObj.isInitialized
        }
    }
}