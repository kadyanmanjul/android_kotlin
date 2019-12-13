package com.joshtalks.joshskills.ui.signup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentSignUpStep2Binding

class SignUpStep2Fragment : Fragment() {


    private lateinit var signUpStep2Binding: FragmentSignUpStep2Binding

    private lateinit var viewModel:SignUpViewModel


    override fun onAttach(context: Context) {
        super.onAttach(context)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {ViewModelProviders.of(this)[SignUpViewModel::class.java]} ?: throw Exception("Invalid Activity")

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        signUpStep2Binding = DataBindingUtil.inflate(inflater, R.layout.fragment_sign_up_step2, container, false)
        signUpStep2Binding.lifecycleOwner = this
        signUpStep2Binding.signUpViewModel = viewModel
       // signUpStep2Binding.us
        //signUpStep2Binding.phoneNumberObservable1=viewModel.phoneNumberObservable
       // signUpStep2Binding.textWatcher=viewModel.phoneNumberTextWatcher

        return signUpStep2Binding.root;
    }


    companion object {

        @JvmStatic
        fun newInstance() = SignUpStep2Fragment()

    }
}
