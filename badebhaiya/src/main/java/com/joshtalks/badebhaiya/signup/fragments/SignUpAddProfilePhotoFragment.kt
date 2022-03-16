package com.joshtalks.badebhaiya.signup.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.databinding.FragmentSignupAddProfilePhotoBinding
import com.joshtalks.badebhaiya.signup.viewmodel.SignUpViewModel

class SignUpAddProfilePhotoFragment: Fragment() {
    private lateinit var binding: FragmentSignupAddProfilePhotoBinding
    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
    }
    companion object {
        fun newInstance() = SignUpAddProfilePhotoFragment
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_signup_add_profile_photo, container, false)
        binding.handler = viewModel
        return binding.root
    }
}