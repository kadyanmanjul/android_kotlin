package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.constants.GET_UPGRADE_DETAILS
import com.joshtalks.joshskills.databinding.FragmentExpertCallUpgradeBinding
import com.joshtalks.joshskills.ui.callWithExpert.model.ExpertUpgradeDetails
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.CallWithExpertViewModel

class ExpertCallUpgradeFragment : BaseFragment() {

    private lateinit var binding: FragmentExpertCallUpgradeBinding

    private val viewModel by lazy {
        ViewModelProvider(requireActivity())[CallWithExpertViewModel::class.java]
    }

    override fun initViewBinding() {
        binding.rechargeToTalkBtn.setOnClickListener {
            findNavController().navigate(R.id.action_upgrade_to_wallet)
        }
    }

    override fun initViewState() {
        liveData.observe(viewLifecycleOwner) {
            when (it.what) {
                GET_UPGRADE_DETAILS -> setUIData(it.obj as ExpertUpgradeDetails)
            }
        }
    }

    override fun setArguments() {
//        TODO("Not yet implemented")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExpertCallUpgradeBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.vm = viewModel
        viewModel.getExpertUpgradeDetails()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().findViewById<TextView>(R.id.iv_earn).setOnClickListener {
            findNavController().navigate(R.id.action_upgrade_to_wallet)
        }
    }

    private fun setUIData(data: ExpertUpgradeDetails) {
        binding.upgradeCardText.text = data.upgradeText
        binding.expertCallUpgrade.text = "Upgrade for ₹${data.amount}"
        binding.walletBalanceTxt.text = "You have ${viewModel.creditsCount.value ?: 0} "

        data.features.forEach {
            val view = getCourseDescriptionList(it)
            if (view != null) {
                binding.featureLlLyt.addView(view)
            }
        }
    }

    private fun getCourseDescriptionList(feature: String): View? {
        val featureListInflate = context?.getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val featureCard = featureListInflate.inflate(R.layout.layout_expert_feature, null, true)

        featureCard?.findViewById<TextView>(R.id.feature_text)?.text =
            HtmlCompat.fromHtml(feature, HtmlCompat.FROM_HTML_MODE_LEGACY)
        return featureCard
    }
}