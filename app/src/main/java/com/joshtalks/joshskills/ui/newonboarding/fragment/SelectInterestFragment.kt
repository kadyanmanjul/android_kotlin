package com.joshtalks.joshskills.ui.newonboarding.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.chip.Chip
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.IS_GUEST_ENROLLED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentSelectInterestBinding
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.inbox.COURSE_EXPLORER_CODE
import com.joshtalks.joshskills.ui.newonboarding.viewmodel.OnBoardViewModel
import kotlinx.android.synthetic.main.base_toolbar.view.iv_help
import kotlinx.android.synthetic.main.base_toolbar.view.text_message_title

class SelectInterestFragment : Fragment() {

    lateinit var binding: FragmentSelectInterestBinding
    private val interestSet: MutableSet<Int> = hashSetOf()
    lateinit var viewmodel: OnBoardViewModel
    private var maxSelection = 5
    private var minSelection = 3
    private var isRecommendationFragment = false


    companion object {
        const val TAG = "SelectInterestFragment"
        const val IS_RECOMMENDATION_FRAGMENT = "is_recommendation_fragment"
        fun newInstance(isRecommendationFragment: Boolean = false): SelectInterestFragment {
            val args = Bundle().apply {
                putBoolean(IS_RECOMMENDATION_FRAGMENT, isRecommendationFragment)
            }
            val fragment = SelectInterestFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isRecommendationFragment = it.getBoolean(IS_RECOMMENDATION_FRAGMENT)
        }
        viewmodel = ViewModelProvider(requireActivity()).get(OnBoardViewModel::class.java)
        maxSelection = VersionResponse.getInstance().maximumNumberOfInterests ?: 5
        minSelection = VersionResponse.getInstance().minimumNumberOfInterests ?: 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_interest, container, false)
        binding.handler = this
        binding.interestDescriptionTv.text = VersionResponse.getInstance().interestText ?: EMPTY

        binding.toolbar.iv_help.setOnClickListener { (requireActivity() as BaseActivity).openHelpActivity() }
        if (VersionResponse.getInstance().hasVersion())
            populateInterests()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewmodel.apiCallStatusLiveData.observe(viewLifecycleOwner, {
            if (isRecommendationFragment) {
                // new Activity ??
            } else {
                if (it == ApiCallStatus.SUCCESS) {
                    showBottomDialog()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        binding.toolbar.text_message_title.text = getString(R.string.select_interest)
    }

    fun onDoneClicked() {
        if (isRecommendationFragment) {
            viewmodel.postRecommendedInterests(interestSet.toList())
            CourseExploreActivity.startCourseExploreActivity(
                requireActivity(),
                COURSE_EXPLORER_CODE,
                null,
                state = BaseActivity.ActivityEnum.DeepLink
            )
            requireActivity().finish()
        } else {
            AppAnalytics.create(AnalyticsEvent.NEW_ONBOARDING_ENROLLED_WITH_INTERESTS.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam("version", VersionResponse.getInstance().version?.name.toString())
                .addParam("With number of tag", interestSet.size)
                .addParam("is_already-enrolled", PrefManager.getBoolValue(IS_GUEST_ENROLLED))
                .push()
            viewmodel.enrollMentorAgainstTags(interestSet.toList())
        }
    }

    private fun showBottomDialog() {
        (requireActivity() as BaseActivity).apply {
            replaceFragment(
                R.id.onboarding_container,
                SuccessfulEnrolledBottomSheet.newInstance(),
                SuccessfulEnrolledBottomSheet.TAG
            )
        }
    }


    private fun populateInterests() {
        if (isRecommendationFragment) {
            VersionResponse.getInstance().segmentTags?.forEach {
                val chip = LayoutInflater.from(context)
                    .inflate(R.layout.interest_chip_item, null, false) as Chip
                chip.text = it.name
                chip.tag = it.id
                chip.id = it.id!!
                chip.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (interestSet.size == maxSelection && isChecked) {
                        buttonView.isChecked = false
                        showToast(getString(R.string.interest_message, maxSelection.toString()))
                    } else {

                        if (isChecked)
                            interestSet.add(buttonView.id)
                        else
                            interestSet.remove(buttonView.id)
                        binding.interestBtn.isEnabled = interestSet.size >= minSelection

                        binding.selectedInterestTv.text =
                            getString(R.string.interest_count, interestSet.size, maxSelection)
                    }
                }
                binding.interestCg.addView(chip)
            }
        } else {
            VersionResponse.getInstance().courseInterestTags?.forEach {
                val chip = LayoutInflater.from(context)
                    .inflate(R.layout.interest_chip_item, null, false) as Chip
                chip.text = it.name
                chip.tag = it.id
                chip.id = it.id!!
                chip.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (interestSet.size == maxSelection && isChecked) {
                        buttonView.isChecked = false
                        showToast(getString(R.string.interest_message, maxSelection.toString()))
                    } else {

                        if (isChecked)
                            interestSet.add(buttonView.id)
                        else
                            interestSet.remove(buttonView.id)
                        binding.interestBtn.isEnabled = interestSet.size >= minSelection

                        binding.selectedInterestTv.text =
                            getString(R.string.interest_count, interestSet.size, maxSelection)
                    }
                }
                binding.interestCg.addView(chip)
            }
        }
    }
}
