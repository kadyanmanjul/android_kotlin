package com.joshtalks.joshskills.ui.signup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ARG_PHONE_NUMBER
import com.joshtalks.joshskills.databinding.FragmentSignUpStep3Binding
import kotlinx.android.synthetic.main.fragment_sign_up_step3.*

class SignUpStep3Fragment : Fragment() {
    private lateinit var phoneNumber: String

    private lateinit var signUpStep3Binding: FragmentSignUpStep3Binding

    private lateinit var viewModel:SignUpViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            phoneNumber = it.getString(ARG_PHONE_NUMBER).toString()
        }
        viewModel = activity?.run {ViewModelProviders.of(this)[SignUpViewModel::class.java]} ?: throw Exception("Invalid Activity")

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        signUpStep3Binding = DataBindingUtil.inflate(inflater, R.layout.fragment_sign_up_step3, container, false)
        signUpStep3Binding.lifecycleOwner = this
        signUpStep3Binding.signUpViewModel = viewModel
        return signUpStep3Binding.root;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        otp_view.requestFocus()
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

    }

    override fun onDetach() {
        super.onDetach()
    }



    companion object {
        @JvmStatic
        fun newInstance(phoneNumber: String) =
            SignUpStep3Fragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHONE_NUMBER, phoneNumber)
                }
            }
    }
}
