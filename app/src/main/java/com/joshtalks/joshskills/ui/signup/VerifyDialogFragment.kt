package com.joshtalks.joshskills.ui.signup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ARG_PHONE_NUMBER
import com.joshtalks.joshskills.databinding.FragmentVerifyPhoneBinding

class VerifyDialogFragment : DialogFragment() {


    private lateinit var verifyPhoneBinding: FragmentVerifyPhoneBinding
    private lateinit var phoneNumber: String
    private lateinit var callback: VerifyDialogFragmentListener


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            bundle.getString(ARG_PHONE_NUMBER)?.let {
                phoneNumber = it
            }

        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as VerifyDialogFragmentListener

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        verifyPhoneBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_verify_phone, container, false)
        verifyPhoneBinding.lifecycleOwner = this
        verifyPhoneBinding.handler = this
        return verifyPhoneBinding.root
    }

    fun editThisNumber() {
        dismiss()
        callback.edit()

    }

    fun okWithThisNumber() {
        dismiss()
        callback.ok()
    }


    companion object {
        @JvmStatic
        fun newInstance(phoneNumber: String) =
            VerifyDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHONE_NUMBER, phoneNumber)
                }
            }
    }


}

interface VerifyDialogFragmentListener {
    fun edit()
    fun ok()
}