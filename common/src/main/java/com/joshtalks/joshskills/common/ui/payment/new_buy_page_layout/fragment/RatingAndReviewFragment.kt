package com.joshtalks.joshskills.common.ui.payment.new_buy_page_layout.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.common.databinding.FragmentRatingAndReviewBinding
import com.joshtalks.joshskills.common.ui.payment.new_buy_page_layout.viewmodel.RatingAndReviewViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

class RatingAndReviewFragment : Fragment() {

    private val viewModel by lazy {
        ViewModelProvider(requireActivity())[RatingAndReviewViewModel::class.java]
    }

    lateinit var binding: FragmentRatingAndReviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            viewModel.testId = it.getInt("TEST_ID")
        }
        viewModel.fetchReviews()

        lifecycleScope.launchWhenStarted {
            viewModel.reviewLiveData.distinctUntilChanged().collectLatest {
                viewModel.ratingAndReviewAdapter.submitData(it)
            }
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

    companion object {

        @JvmStatic
        fun newInstance(testId: Int) = RatingAndReviewFragment().apply {
            arguments = Bundle().apply {
                putInt("TEST_ID", testId)
            }
        }
    }
}