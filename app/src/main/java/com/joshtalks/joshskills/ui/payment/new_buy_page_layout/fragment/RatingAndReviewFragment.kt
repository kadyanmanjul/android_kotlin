package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.databinding.FragmentRatingAndReviewBinding
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.viewmodel.BuyPageViewModel
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.viewmodel.RatingAndReviewViewModel

class RatingAndReviewFragment : Fragment() {

    private val viewModel by lazy {
        ViewModelProvider(requireActivity())[RatingAndReviewViewModel::class.java]
    }

    private val buyPageViewModel by lazy {
        ViewModelProvider(requireActivity())[BuyPageViewModel::class.java]
    }

    lateinit var binding: FragmentRatingAndReviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentRatingAndReviewBinding.inflate(inflater,container,false)
        binding.vm = viewModel
        binding.executePendingBindings()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getRatingAndReviews(buyPageViewModel.testId)
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RatingAndReviewFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}