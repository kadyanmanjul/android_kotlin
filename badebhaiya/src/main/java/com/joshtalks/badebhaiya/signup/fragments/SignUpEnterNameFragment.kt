package com.joshtalks.badebhaiya.signup.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.isValidFullNumber
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.databinding.FragmentSignupEnterNameBinding
import com.joshtalks.badebhaiya.signup.viewmodel.SignUpViewModel
import kotlinx.android.synthetic.main.fragment_signup_enter_phone.*

class SignUpEnterNameFragment: Fragment() {
    private lateinit var binding: FragmentSignupEnterNameBinding
    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
    }
    companion object {
        fun newInstance() = SignUpEnterNameFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_signup_enter_name, container, false)
        binding.handler = this
        binding.etFirstName.addTextChangedListener {
            if (binding.etFirstName.text.isNullOrEmpty().not()) {
                btnNext.setBackgroundResource(R.drawable.change_button_selector)
            }
            else
                btnNext.setBackgroundResource(R.drawable.next_button_selector)

        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addObservers()
    }

    private fun addObservers() {
        binding.apply {
            etFirstName.setText(viewModel.firstName)
            etLastName.setText(viewModel.lastName)
        }
    }

    fun submitProfile(view: View) {
        if (binding.etFirstName.text.isNullOrEmpty() || binding.etLastName.text.isNullOrEmpty()) {
            showToast(getString(R.string.empty_name))
            return
        }
        startProgress()
        val requestMap = mutableMapOf<String, String?>()
        requestMap["first_name"] = "${binding.etFirstName.text} ${binding.etLastName.text}"
        viewModel.completeProfile(requestMap)
    }

    private fun startProgress() {
        binding.btnNext.showProgress {
            buttonTextRes = R.string.plz_wait
            progressColors = intArrayOf(ContextCompat.getColor(requireContext(), R.color.white))
            gravity = DrawableButton.GRAVITY_CENTER
            progressRadiusRes = R.dimen.dp8
            progressStrokeRes = R.dimen.dp2
            textMarginRes = R.dimen.dp8

        }
        binding.btnNext.isEnabled = false
    }

    private fun hideProgress() {
        binding.btnNext.isEnabled = true
        binding.btnNext.hideProgress(R.string.next)
    }
}