package com.joshtalks.joshskills.ui.nps

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.databinding.FragmentNetPromotorScoreBinding
import com.joshtalks.joshskills.repository.local.entity.NPSEventModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.local.model.nps.NPSQuestionModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


const val NPS_FOR = "nps_for"
const val NPS_QUESTION_LIST = "nps_question_list"
const val MIN_RATING = 0
const val MAX_RATING = 10
const val FACE_0_3 = R.drawable.ic_0_3_face
const val FACE_4_6 = R.drawable.ic_4_6_face
const val FACE_7_10 = R.drawable.ic_7_10_face


enum class NPSProcessStatus {
    NONE, RATING_CHOOSE, EXTRA_INFO, DONE
}

class NetPromoterScoreFragment : BottomSheetDialogFragment(),
    ScoreListAdapter.OnRatingSelectListener {

    companion object {
        @JvmStatic
        fun newInstance(
            npsModel: NPSEventModel,
            npsQuestionModelList: List<NPSQuestionModel>
        ) =
            NetPromoterScoreFragment()
                .apply {
                    arguments = Bundle().apply {
                        putParcelable(NPS_FOR, npsModel)
                        putParcelableArrayList(NPS_QUESTION_LIST, ArrayList(npsQuestionModelList))
                    }
                }
    }

    private lateinit var binding: FragmentNetPromotorScoreBinding
    private lateinit var viewModel: NPSViewModel
    private var npsQuestionModelList: List<NPSQuestionModel>? = null
    private var status: NPSProcessStatus = NPSProcessStatus.NONE
    private var npsModel: NPSEventModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(NPSViewModel::class.java)
        arguments?.let {
            npsQuestionModelList = it.getParcelableArrayList(NPS_QUESTION_LIST)
            npsModel = it.getParcelable(NPS_FOR)
        }
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BaseBottomSheetDialog)
        AppAnalytics.create(AnalyticsEvent.NPS_INITIATED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam("event name", npsModel?.eventName)
            .addParam("event from", npsModel?.event?.name)
            .addParam("event id", npsModel?.eventId)
            .push()
        NPSEventModel.removeCurrentNPA()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_net_promotor_score,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.handler = this

        val baseDialog = dialog
        if (baseDialog is BottomSheetDialog) {
            val behavior: BottomSheetBehavior<*> = baseDialog.behavior
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }
            })

        }
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ar = npsQuestionModelList?.get(0)?.question?.split("~")
        binding.tvTitle.text = ar?.getOrElse(0) { EMPTY }
        binding.tvQuestion.text = ar?.getOrElse(1) { EMPTY }
        if (User.getInstance().firstName.isNullOrEmpty().not()) {
            binding.tvUserName.text = User.getInstance().firstName
        }
        initView()

        viewModel.apiCallStatusLiveData.observe(viewLifecycleOwner, Observer {
            when (it) {
                ApiCallStatus.SUCCESS -> {
                    status = NPSProcessStatus.DONE
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(1200)
                        binding.groupSuccess.visibility = View.VISIBLE
                        binding.groupRoot1.visibility = View.INVISIBLE
                        binding.btnSubmit.isEnabled = true
                        binding.btnSubmit.hideProgress(getString(R.string.continue_label))
                        if (User.getInstance().firstName.isNullOrEmpty()) {
                            binding.tvUserName.visibility = View.GONE
                        }
                        AppAnalytics.create(AnalyticsEvent.NPS_FEEDBACK_SUBMITTED.NAME)
                            .addBasicParam()
                            .addUserDetails()
                            .addParam("event name", npsModel?.eventName)
                            .addParam("event from", npsModel?.event?.name)
                            .addParam("event id", npsModel?.eventId)
                            .addParam("rating", viewModel.selectedRating)
                            .addParam("Feedback", binding.editText.text?.toString())
                            .addParam(
                                "Feedback filled",
                                binding.editText.text?.isNotEmpty() ?: false
                            )
                            .addParam("GAID", PrefManager.getStringValue(USER_UNIQUE_ID))
                            .addParam("Mentor_Id", Mentor.getInstance().getId())
                            .push()
                    }
                }
                ApiCallStatus.RETRY -> {
                    binding.btnSubmit.isEnabled = true
                    binding.btnSubmit.hideProgress(getString(R.string.submit))
                }
                else -> {
                    skip()
                }
            }
        })
    }


    private fun initView() {
        val layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        layoutManager.isSmoothScrollbarEnabled = true
        binding.ratingOptionRv.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    requireContext(),
                    2f
                )
            )
        )
        binding.ratingOptionRv.itemAnimator = null
        binding.ratingOptionRv.layoutManager = layoutManager
        binding.ratingOptionRv.adapter = ScoreListAdapter(this)
        bindProgressButton(binding.btnSubmit)
    }

    override fun onSelectRating(rating: Int) {
        viewModel.selectedRating = rating
        binding.ivRatingView.visibility = View.VISIBLE
        when (rating) {
            in 0..3 -> {
                binding.ivRatingView.setImageResource(FACE_0_3)
            }
            in 4..6 -> {
                binding.ivRatingView.setImageResource(FACE_4_6)
            }
            else -> {
                binding.ivRatingView.setImageResource(FACE_7_10)
            }
        }
        when (rating) {
            MIN_RATING -> {
                binding.minRatingView.visibility = View.VISIBLE
                binding.maxRatingView.visibility = View.INVISIBLE
            }
            MAX_RATING -> {
                binding.minRatingView.visibility = View.INVISIBLE
                binding.maxRatingView.visibility = View.VISIBLE
            }
            else -> {
                binding.minRatingView.visibility = View.INVISIBLE
                binding.maxRatingView.visibility = View.INVISIBLE
            }
        }
        enableButton()
        status = NPSProcessStatus.RATING_CHOOSE
    }

    private fun enableButton() {
        binding.btnSubmit.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                requireContext(),
                R.color.highlight_btn_color
            )
        )
    }

    private fun changeDialogConfiguration() {
        val params: WindowManager.LayoutParams? = dialog?.window?.attributes
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        params?.height = WindowManager.LayoutParams.MATCH_PARENT
        params?.gravity = Gravity.CENTER
        dialog?.window?.attributes = params
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    fun submitFeedback() {
        when (status) {
            NPSProcessStatus.RATING_CHOOSE -> {
                binding.minRatingView.visibility = View.INVISIBLE
                binding.maxRatingView.visibility = View.INVISIBLE
                binding.group1.visibility = View.GONE
                binding.group2.visibility = View.VISIBLE
                binding.tvQuestion.text = npsQuestionModelList?.get(0)?.subQuestion
                changeDialogConfiguration()
                status = NPSProcessStatus.EXTRA_INFO
                binding.btnSubmit.text = getString(R.string.submit)
                binding.ivSeparator.rotation = 180F
                binding.editText.requestFocus()
                binding.editText.isPressed = true
                binding.tvQuestion.visibility = View.GONE
                // showKeyBoard(requireActivity(), binding.editText)
                AppAnalytics.create(AnalyticsEvent.NPS_SCORE_SUBMITTED.NAME)
                    .addBasicParam()
                    .addUserDetails()
                    .addParam("event name", npsModel?.eventName)
                    .addParam("event from", npsModel?.event?.name)
                    .addParam("event id", npsModel?.eventId)
                    .push()
            }
            NPSProcessStatus.EXTRA_INFO -> {
                hideKeyboard(requireActivity(), binding.editText)
                startProgress()
                val eventName = npsQuestionModelList?.getOrNull(0)?.eventName
                viewModel.submitNPS(eventName, binding.editText.text.toString())
            }
            else -> {
                skip()
            }
        }
    }

    private fun startProgress() {
        binding.btnSubmit.showProgress {
            buttonTextRes = R.string.plz_wait
            progressColors = intArrayOf(Color.WHITE)
            gravity = DrawableButton.GRAVITY_TEXT_END
            progressRadiusRes = R.dimen.dp8
            progressStrokeRes = R.dimen.dp2
            textMarginRes = R.dimen.dp8
        }
        binding.btnSubmit.isEnabled = false
    }

    fun cancel() {
        AppAnalytics.create(AnalyticsEvent.NPS_IGNORE.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam("event name", npsModel?.eventName)
            .addParam("event from", npsModel?.event?.name)
            .addParam("event id", npsModel?.eventId)
            .push()
        skip()
    }

    fun skip() {
        try {
            val baseDialog = dialog
            if (baseDialog is BottomSheetDialog) {
                val behavior: BottomSheetBehavior<*> = baseDialog.behavior
                behavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        } catch (ex: Exception) {
            dismissAllowingStateLoss()
            ex.printStackTrace()
        }
    }


}