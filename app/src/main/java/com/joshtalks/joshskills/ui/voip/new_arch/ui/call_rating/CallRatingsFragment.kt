package com.joshtalks.joshskills.ui.voip.new_arch.ui.call_rating

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.play.core.review.ReviewManagerFactory
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.CallRatingDialogBinding
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.ui.chat.CHAT_ROOM_ID
import com.joshtalks.joshskills.ui.lesson.LessonActivity
import com.joshtalks.joshskills.ui.lesson.lesson_completed.LessonCompletedActivity
import com.joshtalks.joshskills.ui.practise.PracticeViewModel
import com.joshtalks.joshskills.ui.video_player.IS_BATCH_CHANGED
import com.joshtalks.joshskills.ui.video_player.LAST_LESSON_INTERVAL
import com.joshtalks.joshskills.ui.voip.new_arch.ui.feedback.FeedbackDialogFragment
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class CallRatingsFragment : BottomSheetDialogFragment() {

    val CALLER_NAME = "caller_name"
    val CALL_DURATION = "call_duration"
    val AGORA_ID = "agora_id"
    val PROFILE_URL = "profile_url"
    val CALLER_MENTOR_ID = "caller_mentor_id"
    val AGORA_MENTOR_ID = "agora_mentor_id"
    var lesson: LessonModel? = null
    lateinit var binding: CallRatingDialogBinding
    var isBlockSelected = false
    var selectedRating = 0
    var prevSelectedRating = 0

    var callerName = EMPTY
    var callDuration = 0
    var agoraCallId = 0
    var callerProfileUrl: String = ""
    var callerMentorId = EMPTY
    var agoraMentorId = EMPTY
    private var checked = 0
    private var count = 0
    private var isRatingSubmittedCount = 0
    private var isRatingSubmittedCountBilkul = 0

    val vm: CallRatingsViewModel by lazy {
        ViewModelProvider(requireActivity())[CallRatingsViewModel::class.java]
    }
    private val practiceViewModel: PracticeViewModel by lazy {
        ViewModelProvider(this).get(PracticeViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = CallRatingDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        addListner()
        addObserver()
        saveFlagToSharedPref()
    }

    private fun saveFlagToSharedPref() {
        Log.d(TAG, "onDismiss: 10")
        PrefManager.put("DelayLessonCompletedActivity", true)
    }

    private fun initView() {
        val mArgs = arguments
        callerName = mArgs?.getString(CALLER_NAME).toString()
        callDuration = mArgs?.getInt(CALL_DURATION)!!
        agoraCallId = mArgs.getInt(AGORA_ID)
        callerProfileUrl = mArgs.getString(PROFILE_URL).toString()
        callerMentorId = mArgs.getString(CALLER_MENTOR_ID).toString()
        agoraMentorId = mArgs.getString(AGORA_MENTOR_ID).toString()

        binding.howCallTxt.text = getString(R.string.how_was_your_call_name, callerName)
        binding.callDurationText.text = getString(R.string.you_spoke_for_minutes, vm.getCallDurationString())
        binding.block.text = getString(R.string.block_caller, callerName)
        if (PrefManager.getBoolValue(IS_COURSE_BOUGHT).not()) {
            binding.cross.visibility = VISIBLE
        } else {
            binding.cross.visibility = GONE
        }
        binding.cImage.setImage(VoipPref.getLastProfileImage())

    }

    private fun addListner() {
        with(binding) {
            block.setOnClickListener {
                if (!isBlockSelected) {
                    if (binding.block.text.equals(getString(R.string.block_caller, callerName))) {
                        selectChange("block")
                    } else {
                        selectChange("fpp")
                    }

                } else {
                    if (binding.block.text.equals(getString(R.string.block_caller, callerName))) {
                        unSelectChange("block")

                    } else {
                        unSelectChange("fpp")
                    }
                }
            }
            ratingList.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
                val myAnim = AnimationUtils.loadAnimation(activity, R.anim.zoom_in)
                val interpolator = MyBounceInterpolator(0.8, 10.0)
                myAnim.interpolator = interpolator
                group.findViewById<RadioButton>(checkedId).startAnimation(myAnim)
                if (checked > 0) {
                    group.findViewById<RadioButton>(checked).setTextColor(resources.getColor(R.color.black))
                }
                selectedRating = group.findViewById<RadioButton>(checkedId).text.toString().toInt()
                group.findViewById<RadioButton>(checkedId).setTextColor(resources.getColor(R.color.white))
                if (selectedRating <= 6) {
                    if (prevSelectedRating in 9..10)
                        unSelectChange("block")

                    block.text = getString(R.string.block_caller, callerName)
                    block.visibility = VISIBLE
                    submit.visibility = VISIBLE
                } else if (selectedRating in 7..8) {
                    isBlockSelected = false
                    submitAutomatically(checkedId, group, myAnim)
                } else {
                    if (vm.ifDialogShow == 1 && PrefManager.getBoolValue(IS_COURSE_BOUGHT)) {
                        if (prevSelectedRating in 0..6)
                            unSelectChange("fpp")
                        block.text = resources.getText(R.string.send_fpp_text)
                        block.visibility = VISIBLE
                        submit.visibility = VISIBLE
                    } else {
                        isBlockSelected = false
                        submitAutomatically(checkedId, group, myAnim)
                    }
                }
                prevSelectedRating = selectedRating
                checked = checkedId
            })

            submit.setOnClickListener {
                var userAction: String? = null
                if (isBlockSelected) {
                    if (binding.block.text.contains(getString(R.string.block_caller, callerName))) {
                        userAction = "BLOCK_USER_SELECTED"
                        vm.blockUser(VoipPref.getLastRemoteUserAgoraId().toString())
                    } else {
                        userAction = "FPP_REQUEST_SELECTED"
                        vm.sendFppRequest(VoipPref.getLastRemoteUserMentorId())
                    }
                }
                if (selectedRating >= 0) {
                    CoroutineScope(Dispatchers.Main).launch {
                        showToast("Your feedback has been successfully submitted")
                    }
                    vm.submitCallRatings(
                        VoipPref.getLastCallId().toString().toString(),
                        selectedRating,
                        VoipPref.getLastRemoteUserAgoraId().toString(),
                        userAction
                    )
                }
                closeSheet()
            }
            cross.setOnClickListener {
                vm.submitCallRatings(
                    VoipPref.getLastCallId().toString(),
                    null,
                    VoipPref.getLastRemoteUserAgoraId().toString(),
                    null
                )
                closeSheet()
            }
        }
    }

    private fun submitAutomatically(checkedId: Int, group: RadioGroup, myAnim: Animation) {
        binding.block.visibility = GONE
        CoroutineScope(Dispatchers.Main).launch {
            showToast("Your feedback has been successfully submitted")
        }
        group.findViewById<RadioButton>(checkedId).startAnimation(myAnim)
        binding.submit.visibility = GONE
        vm.submitCallRatings(
            VoipPref.getLastCallId().toString(),
            selectedRating,
            VoipPref.getLastRemoteUserAgoraId().toString(),
            null
        )
        closeSheet()
    }

    private fun selectChange(s: String) {
        if (s == "fpp" && vm.ifDialogShow == 1 && PrefManager.getBoolValue(IS_COURSE_BOUGHT)) {
            binding.block.background = AppCompatResources.getDrawable(requireContext(), R.drawable.block_button_round_stroke_blue)
            binding.block.setTextColor(resources.getColor(R.color.colorPrimary))
        } else {
            binding.block.background =
                AppCompatResources.getDrawable(requireContext(), R.drawable.block_button_round_stroke_black)
            binding.block.setTextColor(Color.WHITE)
        }
        isBlockSelected = true
    }

    private fun unSelectChange(s: String) {
        binding.block.background = AppCompatResources.getDrawable(requireContext(), R.drawable.block_button_round_stroke)
        binding.block.setTextColor(resources.getColor(R.color.black_quiz))
        isBlockSelected = false
    }

    private fun closeSheet() {
        // 3-> Means Show in app review
        // 4-> Means don't Show in app review
        if (vm.ifGoogleInAppReviewShow == 3){
            if (PrefManager.getIntValue(IS_CUSTOM_RATING_AND_REVIEW_DIALOG_SHOWN) < 2  && PrefManager.getIntValue(IS_CUSTOM_RATING_AND_REVIEW_DIALOG_SHOWN_BILKUL) < 2 && selectedRating in 9..10 && PrefManager.getLongValue(ONE_WEEK_TIME_STAMP, false) < System.currentTimeMillis()) {
                dismissAllowingStateLoss()
                showCustomRatingAndReviewDialog(requireActivity())
                val timestamp = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, 7)
                }.time.time
                PrefManager.put(ONE_WEEK_TIME_STAMP, timestamp)
            } else {
                dismissAllowingStateLoss()
            }
        }
        when (vm.ifDialogShow) {
            0 -> {
                showFeedBackDialog()
                dismissAllowingStateLoss()
            }
            else -> {
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (PrefManager.getBoolValue("OpenLessonCompletedActivity")) {
            PrefManager.put("OpenLessonCompletedActivity", false)
            PrefManager.put("DelayLessonCompletedActivity", false)
            openLessonCompleteScreen(PrefManager.getLessonObject("lessonObject"))
        } else {
            PrefManager.put("DelayLessonCompletedActivity", false)
        }
    }

    private fun openLessonCompleteScreen(lesson: LessonModel?) {
        openLessonCompletedActivity.launch(
            lesson?.let {
                LessonCompletedActivity.getActivityIntent(
                    requireActivity(),
                    it
                )
            }
        )
    }

    var openLessonCompletedActivity: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK && result.data!!.hasExtra(
                    IS_BATCH_CHANGED
                )
            ) {
                requireActivity().setResult(
                    AppCompatActivity.RESULT_OK,
                    Intent().apply {
                        putExtra(IS_BATCH_CHANGED, false)
                        putExtra(LAST_LESSON_INTERVAL, lesson?.interval)
                        putExtra(LessonActivity.LAST_LESSON_STATUS, true)
                        putExtra(LESSON__CHAT_ID, lesson?.chatId)
                        putExtra(CHAT_ROOM_ID, lesson?.chatId)
                    }
                )
                requireActivity().finish()
            }
        }

    private fun showFeedBackDialog() {
        FeedbackDialogFragment.newInstance()
            .show(requireActivity().supportFragmentManager, "FeedBackDialogFragment")
    }

    private fun addObserver() {
        practiceViewModel.getPointsForVocabAndReading(null, channelName = VoipPref.getLastCallChannelName())

        practiceViewModel.pointsSnackBarText.observe(
            this
        ) {
            if (it.pointsList.isNullOrEmpty().not()) {
                PrefManager.put(
                    LESSON_COMPLETE_SNACKBAR_TEXT_STRING,
                    it.pointsList!!.last(),
                    false
                )

            }
        }

    }

    companion object {
        @JvmStatic
        fun newInstance(
            callerName: String,
            callDuration: Int,
            agoraCallId: Int,
            callerProfileUrl: String?,
            callerMentorId: String,
            agoraMentorId: String
        ): CallRatingsFragment {
            val fragment = CallRatingsFragment().apply {
                arguments = Bundle().apply {
                    putString(CALLER_NAME, callerName)
                    putInt(CALL_DURATION, callDuration)
                    putInt(AGORA_ID, agoraCallId)
                    putString(PROFILE_URL, callerProfileUrl)
                    putString(CALLER_MENTOR_ID, callerMentorId)
                    putString(AGORA_MENTOR_ID, agoraMentorId)
                }
            }
            fragment.isCancelable = false
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnKeyListener { dialogInterface, keyCode, keyEvent ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (count >= 3) {
                        vm.submitCallRatings(
                            VoipPref.getLastCallId().toString(),
                            null,
                            VoipPref.getLastRemoteUserAgoraId().toString(),
                            null
                        )
                        closeSheet()
                    }
                    count += 1
                }
                true
            }
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }

    private fun CircleImageView.setImage(url: String) {
        val requestOptions = RequestOptions().placeholder(R.drawable.ic_call_placeholder)
            .error(R.drawable.ic_call_placeholder)
            .format(DecodeFormat.PREFER_RGB_565)
            .disallowHardwareConfig().dontAnimate().encodeQuality(75)
        Glide.with(this)
            .load(url)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(requestOptions)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(this)
    }

    fun showInAppReview(context: Activity) {
        val manager = ReviewManagerFactory.create(context)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                val reviewInfo = request.result
                manager.launchReviewFlow(context, reviewInfo).addOnCompleteListener { result ->
                    if (!result.isSuccessful) {
                        showToast("Review failed")
                    }
                }
            } else {
                showToast(request.exception?.message ?: "")
            }
        }
    }

    fun showCustomRatingAndReviewDialog(context: Activity) {
        try {
            if (isAdded && activity != null) {
                val dialog = Dialog(context)
                dialog.setContentView(R.layout.custom_google_review_dialog)
                dialog.setCancelable(false)
                dialog.setCanceledOnTouchOutside(false)
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.window?.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                dialog.window?.setGravity(Gravity.CENTER)
                dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
                dialog.show()
                vm.saveImpression(SHOW_RATING_POP_UP)
                dialog.findViewById<Button>(R.id.btnNahi).setOnClickListener {
                    vm.saveImpression(IMPRESSION_NAHI_REVIEW)
                    isRatingSubmittedCount = PrefManager.getIntValue(IS_CUSTOM_RATING_AND_REVIEW_DIALOG_SHOWN)
                    isRatingSubmittedCount += 1
                    PrefManager.put(IS_CUSTOM_RATING_AND_REVIEW_DIALOG_SHOWN, isRatingSubmittedCount)
                    dialog.dismiss()
                }
                dialog.findViewById<Button>(R.id.btnHaBilkul).setOnClickListener {
                    vm.saveImpression(IMPRESSION_HA_BILKUL_REVIEW)
                    isRatingSubmittedCountBilkul = PrefManager.getIntValue(IS_CUSTOM_RATING_AND_REVIEW_DIALOG_SHOWN_BILKUL)
                    isRatingSubmittedCountBilkul += 1
                    PrefManager.put(IS_CUSTOM_RATING_AND_REVIEW_DIALOG_SHOWN_BILKUL, isRatingSubmittedCountBilkul)
                    dialog.dismiss()
                    showInAppReview(context)
                }
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
}