package com.joshtalks.joshskills.ui.userprofile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View.INVISIBLE
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.databinding.ActivityUserProfileBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.AwardItemClickedEventBus
import com.joshtalks.joshskills.repository.local.eventbus.DeleteProfilePicEventBus
import com.joshtalks.joshskills.repository.local.eventbus.SaveProfileClickedEvent
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.fpp.constants.IS_ACCEPTED
import com.joshtalks.joshskills.ui.fpp.constants.IS_REJECTED
import com.joshtalks.joshskills.ui.fpp.constants.SENT_REQUEST
import com.joshtalks.joshskills.ui.fpp.constants.REQUESTED
import com.joshtalks.joshskills.ui.fpp.constants.HAS_RECIEVED_REQUEST
import com.joshtalks.joshskills.ui.fpp.constants.IS_ALREADY_FPP
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_SEEN_PROFILE_ANIMATION
import com.joshtalks.joshskills.ui.payment.FreeTrialPaymentActivity
import com.joshtalks.joshskills.ui.points_history.PointsInfoActivity
import com.joshtalks.joshskills.ui.senior_student.SeniorStudentActivity
import com.joshtalks.joshskills.ui.userprofile.fragments.EditProfileFragment
import com.joshtalks.joshskills.ui.userprofile.fragments.EnrolledCoursesFragment
import com.joshtalks.joshskills.ui.userprofile.fragments.ProfileImageShowFragment
import com.joshtalks.joshskills.ui.userprofile.fragments.PreviousProfilePicsFragment
import com.joshtalks.joshskills.ui.userprofile.fragments.MyGroupsFragment
import com.joshtalks.joshskills.ui.userprofile.fragments.UserPicChooserFragment
import com.joshtalks.joshskills.ui.userprofile.models.UserProfileResponse
import com.joshtalks.joshskills.ui.userprofile.models.AwardCategory
import com.joshtalks.joshskills.ui.userprofile.models.FppDetails
import com.joshtalks.joshskills.ui.userprofile.models.GroupInfo
import com.joshtalks.joshskills.ui.userprofile.models.CourseEnrolled
import com.joshtalks.joshskills.ui.userprofile.models.Award
import com.joshtalks.joshskills.ui.userprofile.models.EnrolledCoursesList
import com.joshtalks.joshskills.ui.userprofile.models.GroupsList
import com.joshtalks.joshskills.ui.userprofile.utils.MY_GROUP
import com.joshtalks.joshskills.ui.userprofile.utils.ON_BACK_PRESS
import com.joshtalks.joshskills.ui.userprofile.utils.COURSE_LIST_DATA
import com.joshtalks.joshskills.ui.userprofile.utils.MY_GROUP_LIST_DATA
import com.joshtalks.joshskills.ui.userprofile.utils.USER_PROFILE_BACK_STACK
import com.joshtalks.joshskills.ui.userprofile.utils.COURSE
import com.joshtalks.joshskills.ui.userprofile.viewmodel.UserProfileViewModel
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import com.joshtalks.joshskills.ui.voip.favorite.FavoriteListActivity
import de.hdodenhof.circleimageview.CircleImageView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.base_toolbar.iv_back
import kotlinx.android.synthetic.main.base_toolbar.iv_help
import kotlinx.android.synthetic.main.base_toolbar.iv_edit
import kotlinx.android.synthetic.main.base_toolbar.iv_setting
import kotlinx.android.synthetic.main.base_toolbar.text_message_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.DecimalFormat
import java.util.Locale

const val FOR_BASIC_DETAILS = "For_Basic_Details"
const val FOR_REST = "For_Rest"
const val FOR_EDIT_SCREEN = "For_Edit_Screen"
const val TOOLTIP_USER_PROFILE_SCREEN = "TOOLTIP_USER_PROFILE_SCREEN_"

class UserProfileActivity : WebRtcMiddlewareActivity() {

    lateinit var binding: ActivityUserProfileBinding
    private var mentorId: String = EMPTY
    private var impressionId: String = EMPTY
    private var intervalType: String? = EMPTY
    private var previousPage: String? = EMPTY
    private val compositeDisposable = CompositeDisposable()
    private var awardCategory: List<AwardCategory>? = emptyList()
    private var isSeniorStudent: Boolean = false
    private var startTime = 0L
    private val TAG = "UserProfileActivity"
    private var isAnimationVisible = false
    private var userName: String? = null
    var isExpanded = true
    var isFirstTimeToGetProfileData = true
    var resp = StringBuilder()
    private val liveData = EventLiveData


    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    private val viewModel by lazy {
        ViewModelProvider(this).get(
            UserProfileViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_profile)
        binding.lifecycleOwner = this
        binding.handler = this
        mentorId = intent.getStringExtra(KEY_MENTOR_ID) ?: EMPTY
        intervalType = intent.getStringExtra(INTERVAL_TYPE)
        previousPage = intent.getStringExtra(PREVIOUS_PAGE)
        binding.txtFavouriteJoshTalk.movementMethod = LinkMovementMethod.getInstance()
        addObserver()
        startTime = System.currentTimeMillis()
        initToolbar()
        getProfileData(intervalType, previousPage)
        setOnClickListeners()
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
    }

    private fun setOnClickListeners() {
        binding.pointLayout.setOnClickListener {
            hideOverlayAnimation()
            openPointHistory(mentorId, intent.getStringExtra(CONVERSATION_ID))
        }

        binding.minutesLayout.setOnClickListener {
            openSpokenMinutesHistory(mentorId, intent.getStringExtra(CONVERSATION_ID))
        }

        binding.userPic.setOnClickListener {
            if (mentorId == Mentor.getInstance().getId()) {
                if (viewModel.getUserProfileUrl().isNullOrBlank().not()) {
                    ProfileImageShowFragment.newInstance(
                        mentorId,
                        false,
                        arrayOf(viewModel.getUserProfileUrl()!!),
                        0,
                        null
                    )
                        .show(supportFragmentManager, "ImageShow")
                } else {
                    openChooser()
                }
            } else {
                if (viewModel.getUserProfileUrl().isNullOrBlank().not()) {
                    ProfileImageShowFragment.newInstance(
                        mentorId,
                        false,
                        arrayOf(viewModel.getUserProfileUrl()!!),
                        0,
                        null
                    )
                        .show(supportFragmentManager, "ImageShow")
                }
            }
        }
        binding.editPic.setOnClickListener {
            if (mentorId == Mentor.getInstance().getId()) {
                openChooser()

            }
        }
        binding.labelViewMoreAwards.setOnClickListener {
            showAllAwards()
        }
        binding.awardsLayout.setOnClickListener {
            showAllAwards()
        }

        binding.labelViewMoreDp.setOnClickListener {
            openPreviousProfilePicsScreen()
        }
        binding.enrolledCoursesLayout.setOnClickListener {
            viewModel.userProfileSectionImpression(mentorId, "COURSE")
            openEnrolledCoursesScreen()
        }
        binding.labelViewMoreCourses.setOnClickListener {
            viewModel.userProfileSectionImpression(mentorId, "COURSE")
            openEnrolledCoursesScreen()
        }
        binding.previousProfilePicLayout.setOnClickListener {
            openPreviousProfilePicsScreen()
        }
        binding.labelViewMoreGroups.setOnClickListener {
            viewModel.userProfileSectionImpression(mentorId, "GROUP")
            openMyGroupsScreen()
        }
        binding.myGroupsLayout.setOnClickListener {
            viewModel.userProfileSectionImpression(mentorId, "GROUP")
            openMyGroupsScreen()
        }
        binding.fppListLayout.setOnClickListener {
            FavoriteListActivity.openFavoriteCallerActivity(
                this,
                CONVERSATION_ID,
                viewModel.isCourseBought.get().not()
            )
        }
        binding.txtUserHometown.setOnClickListener {
            if (mentorId == Mentor.getInstance().getId()) {
                binding.txtUserHometown.isClickable = true
                EditProfileFragment.newInstance(FOR_BASIC_DETAILS)
                    .show(supportFragmentManager, "EditProfile")
            }
        }
        binding.withoutEducation.setOnClickListener {
            if (mentorId == Mentor.getInstance().getId()) {
                binding.txtUserHometown.isClickable = true
                EditProfileFragment.newInstance(FOR_REST)
                    .show(supportFragmentManager, "EditProfile")
            }
        }
        binding.withoutFutureGoals.setOnClickListener {
            if (mentorId == Mentor.getInstance().getId()) {
                binding.txtUserHometown.isClickable = true
                EditProfileFragment.newInstance(FOR_BASIC_DETAILS)
                    .show(supportFragmentManager, "EditProfile")
            }

        }
        binding.userAge.setOnClickListener {
            if (mentorId == Mentor.getInstance().getId()) {
                binding.userAge.isClickable = true
                EditProfileFragment.newInstance(FOR_BASIC_DETAILS)
                    .show(supportFragmentManager, "EditProfile")
            }
        }
        binding.btnSentRequest.setOnClickListener {
            with(binding) {
                if (btnSentRequest.text == getString(R.string.requested)) {
                    btnSentRequest.setBackgroundColor(
                        ContextCompat.getColor(
                            AppObjectController.joshApplication,
                            R.color.colorAccent
                        )
                    )
                    btnSentRequest.text = getString(R.string.send_request)
                    btnSentRequest.setTextColor(
                        ContextCompat.getColor(
                            AppObjectController.joshApplication,
                            R.color.white
                        )
                    )
                    viewModel.deleteFppRequest(mentorId)
                } else {
                    btnSentRequest.setBackgroundColor(
                        ContextCompat.getColor(
                            AppObjectController.joshApplication,
                            R.color.not_now
                        )
                    )
                    btnSentRequest.text = "Requested"
                    btnSentRequest.setTextColor(
                        ContextCompat.getColor(
                            AppObjectController.joshApplication,
                            R.color.black_quiz
                        )
                    )
                    viewModel.sendFppRequest(mentorId)
                }

            }
        }
        binding.btnConfirmRequest.setOnClickListener {
            viewModel.confirmOrRejectFppRequest(mentorId, IS_ACCEPTED, "USER_PROFILE")
            binding.btnConfirmRequest.visibility = GONE
            binding.btnNotNowRequest.visibility = GONE
            binding.profileText.text = userName + " and you are now favorite practice partners"
            var layoutParams: RelativeLayout.LayoutParams =
                binding.profileText.layoutParams as RelativeLayout.LayoutParams
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE)
            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
            layoutParams.topMargin = resources.getDimension(R.dimen._11sdp).toInt()
            layoutParams.bottomMargin = resources.getDimension(R.dimen._11sdp).toInt()
            binding.profileText.layoutParams = layoutParams
            binding.btnConfirmOrNotNowCard.visibility = GONE
            binding.sentRequestCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.request_respond
                )
            )
        }

        binding.btnNotNowRequest.setOnClickListener {
            viewModel.confirmOrRejectFppRequest(mentorId, IS_REJECTED, "USER_PROFILE")
            binding.btnConfirmRequest.visibility = GONE
            binding.btnNotNowRequest.visibility = GONE
            var layoutParams: RelativeLayout.LayoutParams =
                binding.profileText.layoutParams as RelativeLayout.LayoutParams
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE)
            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE)
            layoutParams.topMargin = resources.getDimension(R.dimen._11sdp).toInt()
            layoutParams.bottomMargin = resources.getDimension(R.dimen._11sdp).toInt()
            binding.profileText.layoutParams = layoutParams
            binding.profileText.text = getString(R.string.profile_request_removed_text, userName)
            binding.btnConfirmOrNotNowCard.visibility = GONE
            binding.profileText.gravity = Gravity.CENTER_VERTICAL
        }
    }

    private fun initToolbar() {
        with(iv_back) {
            visibility = VISIBLE
            setOnClickListener {
                onBackPressed()
            }
        }
        with(iv_help) {
            if (mentorId == Mentor.getInstance().getId()) {
                visibility = GONE
            } else {
                visibility = VISIBLE
                setOnClickListener {
                    openHelpActivity()
                }
            }
        }
        with(iv_edit) {
            if (mentorId == Mentor.getInstance().getId()) {
                visibility = VISIBLE
                setOnClickListener {
                    EditProfileFragment.newInstance(FOR_EDIT_SCREEN)
                        .show(supportFragmentManager, "EditProfile")
                }
            } else {
                visibility = GONE
            }
        }
        with(iv_setting) {
//            if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE) && mentorId == Mentor.getInstance()
//                        .getId()) {
            visibility = View.VISIBLE
            setOnClickListener {
                openPopupMenu(it)
            }
//            }else{
//                visibility = View.GONE
//            }
        }
        if (mentorId == Mentor.getInstance().getId()) {
            binding.editPic.visibility = VISIBLE
        } else {
            binding.editPic.visibility = GONE
        }
    }

    private fun openPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view, R.style.setting_menu_style)
        popupMenu.inflate(R.menu.user_profile__menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_points_history -> {
                    openPointHistory(mentorId, intent.getStringExtra(CONVERSATION_ID))
                }
                R.id.minutes_points_history -> {
                    openSpokenMinutesHistory(mentorId, intent.getStringExtra(CONVERSATION_ID))
                }
                R.id.how_to_get_points -> {
                    startActivity(
                        Intent(this, PointsInfoActivity::class.java).apply {
                            putExtra(CONVERSATION_ID, intent.getStringExtra(CONVERSATION_ID))
                        }
                    )
                }
            }
            return@setOnMenuItemClickListener false
        }
        popupMenu.show()
    }

    fun openPreviousProfilePicsScreen() {
        PreviousProfilePicsFragment.newInstance(mentorId)
            .show(supportFragmentManager, "PreviousProfilePics")
    }

    fun openMyGroupsScreen() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val fragment = MyGroupsFragment()
            replace(R.id.user_root_container, fragment, MY_GROUP)
            addToBackStack(USER_PROFILE_BACK_STACK)
        }
    }

    fun openEnrolledCoursesScreen() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val fragment = EnrolledCoursesFragment()
            replace(R.id.user_root_container, fragment, COURSE)
            addToBackStack(USER_PROFILE_BACK_STACK)
        }
    }

    private fun addObserver() {
        viewModel.userData.observe(
            this
        ) {
            if (it.isCourseBought.not() &&
                it.expiryDate != null &&
                it.expiryDate.time < System.currentTimeMillis()
            ) {
                binding.freeTrialExpiryLayout.visibility = View.VISIBLE
            } else {
                binding.freeTrialExpiryLayout.visibility = View.GONE
            }
            it?.let {
                impressionId = it.userProfileImpressionId ?: EMPTY
                initView(it)
            }
        }

        viewModel.awardsList.observe(this) {
            if (this.isSeniorStudent) {
                binding.awardsLayout.visibility = VISIBLE
                binding.multiLineLl.visibility = VISIBLE
                binding.multiLineLl.removeAllViews()
                val layoutInflater =
                    AppObjectController.joshApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val view =
                    layoutInflater.inflate(R.layout.award_view_holder, binding.rootView, false)
                val title = view.findViewById(R.id.title) as AppCompatTextView
                title.text = getString(R.string.senior_student)
                setSeniorStudentAwardView(view!!)
                if (view != null) {
                    binding.multiLineLl.addView(view)
                }
            }
            if (it.isNullOrEmpty()) {
                binding.labelViewMoreAwards.visibility = View.GONE
                binding.awardsLayout.visibility = VISIBLE
                binding.multiLineLl.visibility = VISIBLE
                binding.awardsLayout.isClickable = false
                if (!isSeniorStudent) {
                    binding.noAwardText.visibility = View.VISIBLE
                    if (mentorId == Mentor.getInstance().getId()) {
                        binding.noAwardText.text =
                            getString(R.string.no_awards_me, resp.trim().split(" ")[0])
                    } else {
                        binding.noAwardText.text =
                            getString(R.string.no_awards_others, resp.trim().split(" ")[0])
                    }
                }

            } else {
                this.awardCategory = it
                binding.awardsLayout.visibility = View.VISIBLE
                binding.multiLineLl.visibility = View.VISIBLE
                if (checkIsAwardAchieved(it)) {
                    binding.labelViewMoreAwards.visibility = View.VISIBLE
                    it?.sortedBy { it.sortOrder }?.forEach { awardCategory ->
                        val view = getAwardLayoutItem(awardCategory)
                        if (view != null) {
                            binding.multiLineLl.addView(view)
                        }
                    }
                } else {
                    if (!isSeniorStudent) {
                        binding.noAwardText.visibility = View.VISIBLE
                        binding.labelViewMoreAwards.visibility = View.GONE
                        binding.awardsLayout.isClickable = false
                        if (mentorId == Mentor.getInstance().getId()) {
                            binding.noAwardText.text =
                                getString(R.string.no_awards_me, resp.trim().split(" ")[0])
                        } else {
                            binding.noAwardText.text =
                                getString(R.string.no_awards_others, resp.trim().split(" ")[0])
                        }
                    }
                }
            }
        }

        viewModel.userData.observe(this) {
            viewModel.isCourseBought.set(it.isCourseBought.not())
            if (it.isCourseBought.not()) {
                binding.sentRequestCard.visibility = GONE
            }
        }

        viewModel.fppRequest.observe(this) {
            when (it.requestStatus) {
                SENT_REQUEST -> {
                    binding.sentRequestCard.visibility = VISIBLE
                    binding.btnSentRequest.visibility = VISIBLE
                    binding.profileText.text = it.text
                }
                IS_ALREADY_FPP -> {
                }
                REQUESTED -> {
                    with(binding) {
                        sentRequestCard.visibility = VISIBLE
                        btnSentRequest.visibility = VISIBLE
                        profileText.text = it.text
                        btnSentRequest.backgroundTintList = ContextCompat.getColorStateList(
                            AppObjectController.joshApplication,
                            R.color.not_now
                        )
                        btnSentRequest.text = getString(R.string.requested)
                        btnSentRequest.setTextColor(
                            ContextCompat.getColor(
                                AppObjectController.joshApplication,
                                R.color.black_quiz
                            )
                        )
                    }
                }
                HAS_RECIEVED_REQUEST -> {
                    binding.sentRequestCard.visibility = VISIBLE
                    binding.btnConfirmRequest.visibility = VISIBLE
                    binding.btnNotNowRequest.visibility = VISIBLE
                    binding.profileText.text = it.text

                }
            }
        }

        viewModel.fppList.observe(this) {
            if (it.isNullOrEmpty()) {
                binding.fppListLayout.visibility = View.GONE
                binding.myFppLl.visibility = View.GONE
            } else {
                binding.fppListLayout.visibility = VISIBLE
                binding.myFppLl.visibility = VISIBLE
                binding.fppDp.text = getString(R.string.fpp_text)
                binding.viewAllFpp.visibility = VISIBLE
                binding.viewAllFpp.text = getString(R.string.see_all)
                binding.myFppLl.removeAllViews()
                var countFppList = 0
                it.forEach {
                    if (countFppList < 3) {
                        val view = getFppLayoutItem(it)
                        if (view != null) {
                            binding.myFppLl.addView(view)
                            countFppList++
                        }
                    }
                }
            }
        }

        viewModel.apiCallStatusLiveData.observe(this) {
            if (it == ApiCallStatus.SUCCESS) {
                binding.profileShimmer.visibility = GONE
                binding.profileContainer.visibility = VISIBLE
                binding.previousProfilePicLayout.visibility = VISIBLE
                binding.profileShimmer.stopShimmer()
                if (isFirstTimeToGetProfileData) {
                    isFirstTimeToGetProfileData = false
                    getOtherProfileData()
                }
            } else if (it == ApiCallStatus.START) {
                binding.profileShimmer.visibility = VISIBLE
                binding.profileContainer.visibility = GONE
                binding.previousProfilePicLayout.visibility = GONE
                binding.profileShimmer.startShimmer()
            }
        }
        viewModel.apiCallStatusForAwardsList.observe(this) {
            if (it == ApiCallStatus.SUCCESS) {
                binding.awardsShimmer.visibility = GONE
                binding.awardsLayout.visibility = VISIBLE
                binding.multiLineLl.visibility = VISIBLE
                binding.awardsShimmer.stopShimmer()
            } else if (it == ApiCallStatus.START) {
                binding.awardsShimmer.visibility = VISIBLE
                binding.awardsLayout.visibility = GONE
                binding.multiLineLl.visibility = GONE
                binding.awardsShimmer.startShimmer()
            }
        }


        if (viewModel.fetchingEnrolledCourseList.get()) {
            binding.coursesShimmer.visibility = VISIBLE
            binding.enrolledCoursesLayout.visibility = GONE
            binding.coursesShimmer.startShimmer()

        } else {
            binding.coursesShimmer.stopShimmer()
            binding.coursesShimmer.visibility = GONE
            binding.enrolledCoursesLayout.visibility = VISIBLE
        }


        if (viewModel.fetchingGroupList.get()) {
            binding.grpShimmer.visibility = VISIBLE
            binding.myGroupsLayout.visibility = GONE
            binding.grpShimmer.startShimmer()

        } else {
            binding.grpShimmer.visibility = GONE
            binding.myGroupsLayout.visibility = VISIBLE
            binding.grpShimmer.stopShimmer()
        }

        viewModel.userProfileUrl.observe(this) {
            if (mentorId == Mentor.getInstance().getId()) {
                if (it.isNullOrBlank()) {
                    binding.editPic.visibility = View.VISIBLE
                    binding.editPic.text = getString(R.string.add)
                } else {
                    binding.editPic.visibility = View.VISIBLE
                    binding.editPic.text = getString(R.string.edit_text)
                }
            }
            binding.userPic.post {
                binding.userPic.setUserImageOrInitials(
                    url = it,
                    viewModel.userData.value?.name ?: getRandomName(),
                    28,
                    isRound = true
                )
            }
        }
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(SaveProfileClickedEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    getProfileData(intervalType, previousPage)
                })
    }

    private fun getOtherProfileData() {
        viewModel.getFppStatusInProfile(mentorId)
        viewModel.getProfileAwards(mentorId)
        viewModel.getProfileCourses(mentorId)
        viewModel.getProfileGroups(mentorId)
    }

    private fun initView(userData: UserProfileResponse) {
        val resp = StringBuilder()
        userData.name?.split(" ")?.forEachIndexed { index, string ->
            if (index < 2) {
                resp.append(
                    string.lowercase(Locale.getDefault())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                    .append(" ")
            }
        }
        this.resp = resp
        text_message_title.text = resp
        binding.userName.text = resp
        userName = userData.name
        binding.userAge.text = userData.age.toString()
        if (userData.age == null || userData.age <= 1) {
            binding.userAge.text = "_________"
            binding.userAge.letterSpacing = 0.0F
            binding.userAge.setTextColor(ContextCompat.getColor(this, R.color.black))
        } else {
            binding.userAge.text = userData.age.toString()
            binding.txtUserHometown.letterSpacing = 0.05F
            binding.userAge.setTextColor(ContextCompat.getColor(this, R.color.grey_7A))
        }
        if (userData.hometown.isNullOrBlank()) {
            binding.txtUserHometown.text = "_________"
            binding.txtUserHometown.letterSpacing = 0.0F
            binding.txtUserHometown.setTextColor(ContextCompat.getColor(this, R.color.black))
        } else {
            binding.txtUserHometown.text = userData.hometown
            binding.txtUserHometown.letterSpacing = 0.05F
            binding.txtUserHometown.setTextColor(ContextCompat.getColor(this, R.color.grey_7A))
        }
        binding.joinedOn.text = userData.joinedOn
        if (userData.isOnline == true) {
            binding.onlineStatusIv.visibility = VISIBLE
        }
        if (!userData.futureGoals.isNullOrBlank()) {
            binding.seperatorBasicDetails.visibility = VISIBLE
            binding.txtLabelFutureGoals.visibility = VISIBLE
            binding.txtFutureGoals.visibility = VISIBLE
            binding.txtFutureGoals.text = userData.futureGoals
        } else {
            binding.seperatorBasicDetails.visibility = GONE
            binding.txtLabelFutureGoals.visibility = GONE
            binding.txtFutureGoals.visibility = GONE
        }
        if (!userData.favouriteJoshTalk.isNullOrBlank()) {
            binding.seperatorFavouriteJoshTalk.visibility = VISIBLE
            binding.labelFavouriteJoshTalk.visibility = VISIBLE
            binding.txtFavouriteJoshTalk.visibility = VISIBLE
            binding.txtFavouriteJoshTalk.text = userData.favouriteJoshTalk
        } else {
            binding.seperatorFavouriteJoshTalk.visibility = GONE
            binding.labelFavouriteJoshTalk.visibility = GONE
            binding.txtFavouriteJoshTalk.visibility = GONE
        }
        if (userData.futureGoals.isNullOrBlank() && userData.favouriteJoshTalk.isNullOrBlank() && mentorId == Mentor.getInstance()
                .getId()
        ) {
            binding.withoutFutureGoals.visibility = VISIBLE
        } else {
            binding.withoutFutureGoals.visibility = GONE
        }
        if (userData.educationDetails == null && userData.occupationDetails == null) {
            if (mentorId == Mentor.getInstance().getId()) {
                binding.educationOccupationLayout.visibility = GONE
                binding.withoutEducation.visibility = VISIBLE
            }
        } else {
            binding.withoutEducation.visibility = GONE
            binding.educationOccupationLayout.visibility = VISIBLE
            var occupationDetailsFlag: Boolean
            if (userData.occupationDetails!!.designation != null || userData.occupationDetails!!.company != null) {
                binding.txtLabelOccupation.visibility = VISIBLE
                occupationDetailsFlag = true
                if (userData.occupationDetails!!.designation != null) {
                    binding.txtOccupation.visibility = VISIBLE
                    binding.txtOccupation.text = userData.occupationDetails!!.designation
                } else {
                    binding.txtOccupation.visibility = GONE
                }
                if (userData.occupationDetails!!.company != null) {
                    binding.txtPlace.visibility = VISIBLE
                    binding.txtPlace.text = "at " + userData.occupationDetails!!.company
                    binding.txtPlace.setColorize("at")
                } else {
                    binding.txtPlace.visibility = GONE

                }
            } else {
                occupationDetailsFlag = false
                binding.txtLabelOccupation.visibility = GONE
                binding.txtOccupation.visibility = GONE
                binding.txtPlace.visibility = GONE
            }
            var educationDetailsFlag: Boolean

            if (userData.educationDetails!!.year != null || userData.educationDetails!!.degree != null || userData.educationDetails!!.college != null) {
                binding.labelEducation.visibility = VISIBLE
                educationDetailsFlag = true
                if (userData.educationDetails!!.degree != null) {
                    binding.txtDegree.visibility = VISIBLE
                    binding.txtDegree.text = userData.educationDetails!!.degree
                } else {
                    binding.txtDegree.visibility = GONE
                }
                if (userData.educationDetails!!.year != null) {
                    binding.txtDate.visibility = VISIBLE
                    if (userData.educationDetails!!.college != null) {
                        binding.txtDate.text =
                            "from " + userData.educationDetails!!.college + SINGLE_SPACE + "  • " + userData.educationDetails!!.year
                        binding.txtDate.setColorize("from")
                    } else {
                        binding.txtDate.text = SINGLE_SPACE + "•" + userData.educationDetails!!.year
                    }
                } else {
                    binding.txtDate.visibility = GONE
                    if (userData.educationDetails!!.college != null) {
                        binding.txtDate.visibility = VISIBLE
                        binding.txtDate.text = "from " + userData.educationDetails!!.college
                        binding.txtDate.setColorize("from")
                    } else {
                        binding.txtDate.visibility = GONE
                    }
                }
            } else {
                educationDetailsFlag = false
                binding.labelEducation.visibility = GONE
            }
            if (educationDetailsFlag && occupationDetailsFlag) {
                binding.EducationOccupationLayoutDp.text =
                    getString(R.string.education_occupation_text)
                binding.seperatorEducationDetails.visibility = VISIBLE
            } else {
                if (educationDetailsFlag) {
                    binding.EducationOccupationLayoutDp.text = getString(R.string.education_text)
                }
                if (occupationDetailsFlag) {
                    binding.EducationOccupationLayoutDp.text = getString(R.string.occupation_text)
                }
                binding.seperatorEducationDetails.visibility = GONE
            }
        }

        if (userData.isSeniorStudent) {
            binding.txtLabelSeniorStudent.text = getString(R.string.label_senior_student, resp)
            binding.imgSeniorStudentBadge.visibility = View.VISIBLE
        } else {
            binding.txtLabelSeniorStudent.visibility = View.GONE
            binding.txtLabelBecomeSeniorStudent.visibility = View.GONE
            binding.imgSeniorStudentBadge.visibility = View.GONE
        }
        userData.points?.let {
            var incrementalPoints = 0
            val incrementalValue = it.div(50)
            CoroutineScope(Dispatchers.IO).launch {
                if (incrementalValue > 0) {
                    while (incrementalPoints <= it) {
                        AppObjectController.uiHandler.post {
                            binding.points.text =
                                DecimalFormat("#,##,##,###").format(incrementalPoints)
                        }
                        incrementalPoints = incrementalPoints.plus(incrementalValue)
                        delay(25)
                    }
                }
                AppObjectController.uiHandler.post {
                    binding.points.text = DecimalFormat("#,##,##,###").format(it)
                }
            }
        }

        userData.minutesSpoken?.let {
            var incrementalPoints = 0
            val incrementalValue = it.div(50)
            CoroutineScope(Dispatchers.IO).launch {
                if (incrementalValue > 0) {
                    while (incrementalPoints <= it) {
                        AppObjectController.uiHandler.post {
                            binding.minutes.text =
                                DecimalFormat("#,##,##,###").format(incrementalPoints)
                        }
                        incrementalPoints = incrementalPoints.plus(incrementalValue)
                        delay(25)
                    }
                }
                AppObjectController.uiHandler.post {
                    binding.minutes.text = DecimalFormat("#,##,##,###").format(it)
                }
            }
        }
        if (userData.profilePicturesCount != 0) {
            binding.previousProfilePicLayout.visibility = View.VISIBLE
            binding.labelPreviousDp.text =
                getString(R.string.previous_profile_text, userData.profilePicturesCount.toString())
        } else {
            binding.previousProfilePicLayout.visibility = View.GONE
        }

        // binding.points.text = DecimalFormat("#,##,##,###").format(userData.points)
        binding.streaksText.text = getString(R.string.user_streak_text, userData.streak)
        binding.streaksText.visibility = GONE
        binding.scrollView.fullScroll(ScrollView.FOCUS_UP)
    }

    private fun setSeniorStudentAwardView(view: View) {
        var v: View? = view.findViewById<ConstraintLayout>(R.id.award1)
        v?.visibility = View.VISIBLE
        var image: ImageView = view.findViewById(R.id.image_award1)
        var title: AppCompatTextView = view.findViewById(R.id.title_award1)
        var date: AppCompatTextView = view.findViewById(R.id.date_award1)
        var count: AppCompatTextView = view.findViewById(R.id.txt_count_award1)
        date.visibility = GONE
        title.visibility = GONE
        count.visibility = GONE
        image.setImageResource(R.drawable.senior_student_with_shadow)
    }

    fun TextView.setColorize(subStringToColorize: String) {
        val spannable: Spannable = SpannableString(text)
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#687c90")),
            0,
            subStringToColorize.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            subStringToColorize.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        setText(spannable, TextView.BufferType.SPANNABLE)
    }

    private fun checkIsAwardAchieved(awardCategory: List<AwardCategory>?): Boolean {
        if (mentorId == Mentor.getInstance().getId()) {
            return true
        }
        if (awardCategory.isNullOrEmpty()) {
            return false
        } else {
            awardCategory.forEach {
                it.awards?.forEach { award ->
                    if (award.is_achieved) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun openChooser() {
        UserPicChooserFragment.showDialog(
            supportFragmentManager,
            viewModel.getUserProfileUrl().isNullOrBlank(),
            isFromRegistration = false
        )

    }

    @SuppressLint("WrongViewCast")
    private fun getFppLayoutItem(fppDetails: FppDetails): View? {
        val layoutInflater =
            AppObjectController.joshApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view =
            layoutInflater.inflate(R.layout.fpp_item_list_for_profile, binding.rootView, false)
        val txtUserName = view.findViewById(R.id.tv_name) as AppCompatTextView
        val imageUserProfile = view.findViewById(R.id.profile_image) as CircleImageView
        val txtTotalSpokeTime = view.findViewById(R.id.tv_spoken_time) as AppCompatTextView
        txtUserName.text = fppDetails.fullName ?: ""
        txtTotalSpokeTime.text = fppDetails.text
        imageUserProfile.setUserImageOrInitials(
            fppDetails.photoUrl ?: "",
            fppDetails.fullName ?: ""
        )
        return view
    }

    @SuppressLint("WrongViewCast")
    private fun getAwardLayoutItem(awardCategory: AwardCategory): View? {
        val layoutInflater =
            AppObjectController.joshApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.award_view_holder, binding.rootView, false)
        val title = view.findViewById(R.id.title) as AppCompatTextView
        title.text = awardCategory.label
        var haveAchievedAwards = false
        var index = 0

        awardCategory.awards?.sortedBy { it.sortOrder }?.forEach {


            if (mentorId == Mentor.getInstance().getId()) {
                setAwardView(it, index, view!!)

                view.tag = it.id
                index = index.plus(1)
            } else if (it.is_achieved) {
                haveAchievedAwards = true
                setAwardView(it, index, view!!)
                index = index.plus(1)
            }
        }
        if (haveAchievedAwards.not() && (mentorId == Mentor.getInstance().getId()).not()) {
            return null
        }
        return view
    }

    private fun getMyGroupsLayoutItem(groupInfo: GroupInfo): View? {
        val layoutInflater =
            this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view =
            layoutInflater.inflate(R.layout.my_groups_row_item, binding.rootView, false)
        val txtGroupName = view.findViewById(R.id.tv_group_name) as AppCompatTextView
        val txtMinutesSpoken = view.findViewById(R.id.tv_minutes_spoken) as AppCompatTextView
        val imggGroupIcon = view.findViewById(R.id.group_icon) as CircleImageView

        txtGroupName.text = groupInfo.groupName
        txtMinutesSpoken.text = groupInfo.textToShow
        if (groupInfo.groupIcon == null) {
            imggGroupIcon.setImageResource(R.drawable.group_default_icon)

        } else {
            setImage(imggGroupIcon, groupInfo.groupIcon)
        }
        return view
    }

    @SuppressLint("WrongViewCast")
    private fun getEnrolledCourseLayoutItem(course: CourseEnrolled): View? {
        val layoutInflater =
            this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view =
            layoutInflater.inflate(
                R.layout.enrolled_courses_row_item,
                binding.parentContainer,
                false
            )
        val txtCourseName = view.findViewById(R.id.tv_course_name) as AppCompatTextView
        val txtStudentsEnrolled = view.findViewById(R.id.tv_students_enrolled) as AppCompatTextView
        val imgCourseIcon = view.findViewById(R.id.profile_image) as CircleImageView

        txtCourseName.text = course.courseName
        txtStudentsEnrolled.text =
            getString(R.string.total_students_enrolled, course.noOfStudents.toString())
        setImage(imgCourseIcon, course.courseImage)
        return view
    }

    fun setImage(imageView: ImageView, url: String?) {
        if (url.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.ic_josh_course)
            return
        }

        val multi = MultiTransformation(
            CropTransformation(
                Utils.dpToPx(48),
                Utils.dpToPx(48),
                CropTransformation.CropType.CENTER
            ),
            RoundedCornersTransformation(
                Utils.dpToPx(ROUND_CORNER),
                0,
                RoundedCornersTransformation.CornerType.ALL
            )
        )
        Glide.with(AppObjectController.joshApplication)
            .load(url)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(
                RequestOptions.bitmapTransform(multi).apply(
                    RequestOptions().placeholder(R.drawable.ic_josh_course)
                        .error(R.drawable.ic_josh_course)
                )

            )
            .into(imageView)
    }

    private fun setAwardView(award: Award, index: Int, view: View) {
        var v: View? = null
        when (index) {
            0 -> {
                v = view.findViewById<ConstraintLayout>(R.id.award1)
                v.visibility = VISIBLE

                setViewToLayout(
                    award,
                    view.findViewById(R.id.image_award1),
                    view.findViewById(R.id.title_award1),
                    view.findViewById(R.id.date_award1),
                    view.findViewById(R.id.txt_count_award1)
                )
            }
            1 -> {
                v = view.findViewById<ConstraintLayout>(R.id.award2)
                v.visibility = VISIBLE

                setViewToLayout(
                    award,
                    view.findViewById(R.id.image_award2),
                    view.findViewById(R.id.title_award2),
                    view.findViewById(R.id.date_award2),
                    view.findViewById(R.id.txt_count_award2)
                )
            }
            2 -> {
                v = view.findViewById<ConstraintLayout>(R.id.award3)
                v.visibility = VISIBLE
                setViewToLayout(
                    award,
                    view.findViewById(R.id.image_award3),
                    view.findViewById(R.id.title_award3),
                    view.findViewById(R.id.date_award3),
                    view.findViewById(R.id.txt_count_award3)
                )
            }
            else -> {
            }
        }
    }

    private fun setViewToLayout(
        award: Award,
        image: ImageView,
        title: AppCompatTextView,
        date: AppCompatTextView,
        count: AppCompatTextView
    ) {
        title.text = award.awardText
        if (award.dateText.isNullOrBlank()) {
            date.visibility = INVISIBLE
        } else {
            date.visibility = VISIBLE
            date.text = award.dateText
        }
        award.imageUrl?.let {
            image.setImage(it, this)
        }
        if (award.count > 1) {
            count.visibility = VISIBLE
            count.text = award.count.toString()
        } else {
            count.visibility = GONE
        }
    }

    private fun getProfileData(intervalType: String?, previousPage: String?) {
        viewModel.getProfileData(mentorId, intervalType, previousPage)
    }

    fun showAllAwards() {

        awardCategory?.let {
            SeeAllAwardActivity.startSeeAllAwardActivity(
                this,
                it,
                this.isSeniorStudent,
                this.mentorId
            )
        }
    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listen(AwardItemClickedEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        openAwardPopUp(it.award)
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(DeleteProfilePicEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        viewModel.userData.value?.photoUrl = it.url
                        if (it.url.isBlank()) {
                            viewModel.saveProfileInfo(null)
                        }
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
    }

    private fun openAwardPopUp(award: Award) {
//        showAward(listOf(award), true)
    }

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }

    private fun addUserImageInView(imagePath: String) {
        val imageUpdatedPath = AppDirectory.getImageSentFilePath()
        AppDirectory.copy(imagePath, imageUpdatedPath)
        viewModel.uploadMedia(imageUpdatedPath)
    }

    companion object {
        const val KEY_MENTOR_ID = "leaderboard_mentor_id"
        const val INTERVAL_TYPE = "interval_type"
        const val PREVIOUS_PAGE = "previous_page"

        fun startUserProfileActivity(
            activity: Activity,
            mentorId: String,
            flags: Array<Int> = arrayOf(),
            intervalType: String? = null,
            previousPage: String? = null,
            conversationId: String? = null,
            isFromConversationRoom: Boolean = false,

            ) {
            Intent(activity, UserProfileActivity::class.java).apply {
                putExtra(KEY_MENTOR_ID, mentorId)
                intervalType?.let {
                    putExtra(INTERVAL_TYPE, it)
                }
                putExtra(PREVIOUS_PAGE, previousPage)
                putExtra(CONVERSATION_ID, conversationId)
                flags.forEach { flag ->
                    this.addFlags(flag)
                }
            }.run {
                activity.startActivity(this)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val url = data?.data?.path ?: EMPTY
            if (url.isNotBlank()) {
                addUserImageInView(url)
            }
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Timber.e(ImagePicker.getError(data))
            // Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
        } else {
            Timber.e("Task Cancelled")
            // Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        if (!PrefManager.getBoolValue(HAS_SEEN_PROFILE_ANIMATION))
            showOverlayAnimation()

        liveData.observe(this) {
            when (it.what) {
                ON_BACK_PRESS -> {
                    popBackStack()
                }
                COURSE_LIST_DATA -> {
                    showCourseList(it.obj as EnrolledCoursesList)
                }
                MY_GROUP_LIST_DATA -> {
                    showGroupList(it.obj as GroupsList)
                }
            }
        }
    }

    private fun showCourseList(courseEnrolled: EnrolledCoursesList) {
        if (courseEnrolled.courses.isNullOrEmpty()) {
            binding.enrolledCoursesLayout.visibility = GONE
            binding.enrolledCoursesLl.visibility = GONE
        } else {
            binding.enrolledCoursesLayout.visibility = View.VISIBLE
            binding.enrolledCoursesLl.visibility = View.VISIBLE
            binding.labelEnrolledCourses.text = getString(R.string.courses_title)
            binding.enrolledCoursesLl.removeAllViews()
            var countCourses = 0
            courseEnrolled.courses.forEach { course ->
                if (countCourses < 3) {
                    val view = getEnrolledCourseLayoutItem(course)
                    if (view != null) {
                        binding.enrolledCoursesLl.addView(view)
                        countCourses++
                    }
                }
            }
        }
    }

    private fun showGroupList(myGroup: GroupsList) {
        if (myGroup.myGroupsList.isNullOrEmpty()) {
            binding.myGroupsLayout.visibility = GONE
            binding.myGroupsLl.visibility = GONE
        } else {
            binding.myGroupsLayout.visibility = VISIBLE
            binding.myGroupsLl.visibility = VISIBLE
            binding.labelMyGroups.text = getString(R.string.group_title)
            binding.myGroupsLl.removeAllViews()
            var countGroups = 0
            myGroup.myGroupsList.forEach {
                if (countGroups < 3) {
                    val view = getMyGroupsLayoutItem(it)
                    if (view != null) {
                        binding.myGroupsLl.addView(view)
                        countGroups++
                    }
                }
            }
        }
    }

    private fun showOverlayAnimation() {
        lifecycleScope.launch(Dispatchers.Main) {
            delay(1000)
            binding.contentOverlay.visibility = View.VISIBLE
            binding.arrowAnimation.visibility = View.VISIBLE
            binding.arrowAnimation.playAnimation()
            binding.toolbarOverlay.visibility = VISIBLE
            //animatePoints()
            binding.labelTapToDismiss.visibility = INVISIBLE
            binding.overlayProfileTooltip.visibility = VISIBLE
            PrefManager.put(HAS_SEEN_PROFILE_ANIMATION, true)
            isAnimationVisible = true
            binding.overlayProfileTooltip.startAnimation(
                AnimationUtils.loadAnimation(
                    this@UserProfileActivity,
                    R.anim.slide_in_right
                )
            )
            delay(6500)
            binding.contentOverlay.setOnClickListener {
                hideOverlayAnimation()
            }

            binding.toolbarOverlay.setOnClickListener {
                hideOverlayAnimation()
            }

            binding.overlayProfileTooltip.setOnClickListener {
                hideOverlayAnimation()
            }

            binding.labelTapToDismiss.visibility = View.VISIBLE
            binding.labelTapToDismiss.setOnClickListener {
                hideOverlayAnimation()
            }

            binding.labelTapToDismiss.startAnimation(
                AnimationUtils.loadAnimation(this@UserProfileActivity, R.anim.slide_up_dialog)
            )
        }
    }

    private fun hideOverlayAnimation() {
        binding.contentOverlay.setOnClickListener(null)
        binding.overlayProfileTooltip.setOnClickListener(null)
        binding.labelTapToDismiss.setOnClickListener(null)
        binding.toolbarOverlay.setOnClickListener(null)
        window.statusBarColor = ContextCompat.getColor(this, R.color.status_bar_color)
        //stopPointAnimation()
        binding.arrowAnimation.visibility = GONE
        binding.contentOverlay.visibility = GONE
        binding.toolbarOverlay.visibility = GONE
        binding.labelTapToDismiss.visibility = INVISIBLE
        binding.overlayProfileTooltip.visibility = GONE
        isAnimationVisible = false
    }

    fun showSeniorStudentScreen() {
        SeniorStudentActivity.startSeniorStudentActivity(this)
    }

    fun showFreeTrialPaymentScreen() {
        FreeTrialPaymentActivity.startFreeTrialPaymentActivity(
            this,
            AppObjectController.getFirebaseRemoteConfig().getString(
                FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
            ),
            viewModel.userData.value?.expiryDate?.time
        )
        // finish()
    }

    private fun popBackStack() {
        if (isAnimationVisible) {
            hideOverlayAnimation()
            return
        }
        if (supportFragmentManager.backStackEntryCount > 1) {
            try {
                supportFragmentManager.popBackStackImmediate()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        } else {
            startTime = System.currentTimeMillis().minus(startTime).div(1000)
            if (startTime > 0 && impressionId.isBlank().not()) {
                viewModel.engageUserProfileTime(impressionId, startTime)
            }
            onBackPressed()
        }
    }

    fun getUserProfileTooltip(): String {
        val courseId = PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID)
        return AppObjectController.getFirebaseRemoteConfig()
            .getString(TOOLTIP_USER_PROFILE_SCREEN + courseId)
    }
}
