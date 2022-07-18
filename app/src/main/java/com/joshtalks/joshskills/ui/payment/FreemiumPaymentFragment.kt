package com.joshtalks.joshskills.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.databinding.FragmentFreemiumPaymentBinding
import com.joshtalks.joshskills.databinding.ItemFreemiumFeatureBinding
import com.joshtalks.joshskills.repository.server.FreemiumPaymentFeature
import com.joshtalks.joshskills.ui.startcourse.TEST_ID

class FreemiumPaymentFragment : CoreJoshFragment() {
    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(FreeTrialPaymentViewModel::class.java)
    }
    private lateinit var binding: FragmentFreemiumPaymentBinding
    private lateinit var testId: String

    companion object {
        fun newInstance(testId: String): FreemiumPaymentFragment {
            val args = Bundle()
            args.putString(TEST_ID, testId)
            val fragment = FreemiumPaymentFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_freemium_payment, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        testId = arguments?.getString(TEST_ID, FREE_TRIAL_PAYMENT_TEST_ID) ?: FREE_TRIAL_PAYMENT_TEST_ID
        viewModel.getFreemiumPaymentDetails(testId)
        setObservers()
    }

    private fun setObservers() {
        viewModel.freemiumPaymentDetailsLiveData.observe(viewLifecycleOwner) {
            it?.let {
                viewModel.paymentButtonText.value = it.buttonText
                binding.data = it
            }
        }
    }
}

private class FeatureListAdapter :
    ListAdapter<FreemiumPaymentFeature, FeatureListAdapter.FeatureViewHolder>(object :
        DiffUtil.ItemCallback<FreemiumPaymentFeature>() {

        override fun areItemsTheSame(oldItem: FreemiumPaymentFeature, newItem: FreemiumPaymentFeature): Boolean {
            return oldItem.feature == newItem.feature
        }

        override fun areContentsTheSame(oldItem: FreemiumPaymentFeature, newItem: FreemiumPaymentFeature): Boolean {
            return oldItem == newItem
        }
    }) {

    inner class FeatureViewHolder(val binding: ItemFreemiumFeatureBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FreemiumPaymentFeature) {
            binding.item = item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder =
        FeatureViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_freemium_feature,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }
}

@BindingAdapter("featureList")
fun setFeatureList(recyclerView: RecyclerView, featureList: List<FreemiumPaymentFeature>?) {
    recyclerView.adapter = FeatureListAdapter().apply {
        submitList(featureList)
    }
}