package com.joshtalks.joshskills.ui.signup

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import coil.api.load
import coil.transform.CircleCropTransformation
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.SignUpStep1FragmentBinding
import kotlinx.android.synthetic.main.sign_up_step1_fragment.*


class SignUpStep1Fragment : Fragment() {

    companion object {
        fun newInstance() = SignUpStep1Fragment()
    }

    private lateinit var signUpStep1FragmentBinding: SignUpStep1FragmentBinding

    private lateinit var viewModel:SignUpViewModel
    /*private val viewModel: SignUpViewModel by lazy {
        ViewModelProviders.of(this).get(SignUpStep1Fragment::class.java.name,SignUpViewModel::class.java)
    }*/

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        signUpStep1FragmentBinding =
            DataBindingUtil.inflate(inflater, R.layout.sign_up_step1_fragment, container, false)
        signUpStep1FragmentBinding.lifecycleOwner = this
        signUpStep1FragmentBinding.signUpViewModel = viewModel

        return signUpStep1FragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {ViewModelProviders.of(this)[SignUpViewModel::class.java]} ?: throw Exception("Invalid Activity")

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }






}
