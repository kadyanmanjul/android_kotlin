package com.joshtalks.joshskills.ui.userprofile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.databinding.DataBindingUtil
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
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.databinding.ActivityUserProfileBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.AwardItemClickedEventBus
import com.joshtalks.joshskills.repository.local.eventbus.DeleteProfilePicEventBus
import com.joshtalks.joshskills.repository.local.eventbus.SaveProfileClickedEvent
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.*
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_SEEN_PROFILE_ANIMATION
import com.joshtalks.joshskills.ui.payment.FreeTrialPaymentActivity
import com.joshtalks.joshskills.ui.points_history.PointsInfoActivity
import com.joshtalks.joshskills.ui.senior_student.SeniorStudentActivity
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import de.hdodenhof.circleimageview.CircleImageView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.text.DecimalFormat
import java.util.*
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.base_toolbar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

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
    private var viewerReferral: Int = 0
    private var helpCountControl: Boolean = false

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    private val viewModel by lazy {
        ViewModelProvider(this).get(
            UserProfileViewModel::class.java
        )
    }

    lateinit var ans: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_profile)
        binding.lifecycleOwner = this
        binding.handler = this
        mentorId = intent.getStringExtra(KEY_MENTOR_ID) ?: EMPTY
        intervalType = intent.getStringExtra(INTERVAL_TYPE)
        previousPage = intent.getStringExtra(PREVIOUS_PAGE)
        addObserver()
        startTime = System.currentTimeMillis()
        initToolbar()
        initABTest(mentorId, intervalType, previousPage)
        setOnClickListeners()
    }

    private fun initABTest(mentorId: String, intervalType: String?, previousPage: String?) {
        viewModel.getHelpCountCampaignData(CampaignKeys.PEOPLE_HELP_COUNT.name, mentorId, intervalType, previousPage)
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
                    ProfileImageShowFragment.newInstance(viewModel.getUserProfileUrl(), null, null,mentorId,false)
                        .show(supportFragmentManager, "ImageShow")
                } else {
                    openChooser()

                }
            } else {
                if (viewModel.getUserProfileUrl().isNullOrBlank().not()) {
                    ProfileImageShowFragment.newInstance(viewModel.getUserProfileUrl(), null, null,mentorId,false)
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
        binding.previousProfilePicLayout.setOnClickListener {
            openPreviousProfilePicsScreen()
        }
        binding.labelViewMoreGroups.setOnClickListener{
            openMyGroupsScreen()
        }
        binding.myGroupsLayout.setOnClickListener{
            openMyGroupsScreen()
        }
        binding.txtUserHometown.setOnClickListener {
            if (mentorId == Mentor.getInstance().getId()) {
                binding.txtUserHometown.isClickable = true
                EditProfileFragment.newInstance().show(supportFragmentManager, "EditProfile")
            }

        }
        binding.userAge.setOnClickListener {
            if (mentorId == Mentor.getInstance().getId()) {
                binding.userAge.isClickable = true
                EditProfileFragment.newInstance().show(supportFragmentManager, "EditProfile")
            }
        }

    }

    private fun initToolbar() {
        with(iv_back) {
            visibility = View.VISIBLE
            setOnClickListener {
                onBackPressed()
            }
        }
        with(iv_help) {
            if (mentorId == Mentor.getInstance().getId()) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                setOnClickListener {
                    openHelpActivity()
                }
            }
        }
        with(iv_edit) {
            if (mentorId == Mentor.getInstance().getId()) {
                visibility = View.VISIBLE
                setOnClickListener {
                    openEditProfileScreen()
                }
            } else {
                visibility = View.GONE
            }
        }
        if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE) && mentorId == Mentor.getInstance()
                .getId()
        ) {
            with(iv_setting) {
                visibility = View.VISIBLE
                setOnClickListener {
                    openPopupMenu(it)
                }
            }
        }
        if (mentorId == Mentor.getInstance().getId()) {
            binding.editPic.visibility = View.VISIBLE
        } else {
            binding.editPic.visibility = View.GONE
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

    fun openEditProfileScreen() {
        EditProfileFragment.newInstance().show(supportFragmentManager, "EditProfile")
    }

    fun openPreviousProfilePicsScreen() {
        PreviousProfilePicsFragment.newInstance(mentorId)
            .show(supportFragmentManager, "PreviousProfilePics")
    }
    fun openMyGroupsScreen() {
        MyGroupsFragment.newInstance()
            .show(supportFragmentManager, "MyGroups")
    }

    /* private fun initRecyclerView() {

         val linearLayoutManager = com.mindorks.placeholderview.SmoothLinearLayoutManager(this)
         linearLayoutManager.isSmoothScrollbarEnabled = true
         binding.awardRv.builder.setHasFixedSize(true)
             .setLayoutManager(linearLayoutManager)
         binding.awardRv.addItemDecoration(
             com.joshtalks.joshskills.util.DividerItemDecoration(
                 this,
                 R.drawable.list_divider
             )
         )
     }*/

    private fun addObserver() {
        viewModel.userData.observe(
            this,
            {
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
                    hideProgressBar()
                    initView(it)
                }
                viewerReferral = it.referralOfViewer

            }
        )

        viewModel.apiCallStatusLiveData.observe(this) {
            if (it == ApiCallStatus.SUCCESS) {
                hideProgressBar()
            } else if (it == ApiCallStatus.FAILED) {
                hideProgressBar()
                this.finish()
            } else if (it == ApiCallStatus.START) {
                showProgressBar()
            }
        }

        viewModel.userProfileUrl.observe(this) {
            if (mentorId.equals(Mentor.getInstance().getId())) {
                if (it.isNullOrBlank()) {
                    binding.editPic.visibility = View.VISIBLE
                    binding.editPic.text = "Add"
                } else {
                    binding.editPic.visibility = View.VISIBLE
                    binding.editPic.text = "Edit"
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

        viewModel.apiCallStatus.observe(this) {
            if (it == ApiCallStatus.SUCCESS) {
                getProfileData(intervalType, previousPage)
                hideProgressBar()
            } else if (it == ApiCallStatus.FAILED) {
                hideProgressBar()
            } else if (it == ApiCallStatus.START) {
                showProgressBar()
            }
        }

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(SaveProfileClickedEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    getProfileData(intervalType, previousPage)
                })

        viewModel.helpCountAbTestliveData.observe(this){helpCountAbTestliveData->
            helpCountAbTestliveData?.let { map->
                helpCountControl = (map.variantKey == VariantKeys.PHC_IS_ENABLED.name) && map.variableMap?.isEnabled == true
            }
        }

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
        text_message_title.text = resp
        binding.userName.text = resp
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
            binding.onlineStatusIv.visibility = View.VISIBLE
        }

        if (userData.previousProfilePictures != null) {
            binding.previousProfilePicLayout.visibility = View.VISIBLE
            binding.labelPreviousDp.setText("Previous Profile Photos (${userData.previousProfilePictures.profilePictures.size})")
        } else {
            binding.previousProfilePicLayout.visibility = View.GONE
            binding.labelPreviousDp.text = userData.previousProfilePictures?.label
        }

        if (userData.isSeniorStudent) {
            binding.txtLabelSeniorStudent.text = getString(R.string.label_senior_student, resp)
            // binding.txtLabelSeniorStudent.visibility = View.VISIBLE
            // binding.txtLabelBecomeSeniorStudent.visibility = View.VISIBLE
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
        // binding.points.text = DecimalFormat("#,##,##,###").format(userData.points)
        binding.streaksText.text = getString(R.string.user_streak_text, userData.streak)
        binding.streaksText.visibility = View.GONE
        if (userData.isSeniorStudent) {
            this.isSeniorStudent = true

            binding.awardsLayout.visibility = View.VISIBLE
            binding.multiLineLl.visibility = View.VISIBLE
            binding.multiLineLl.removeAllViews()
            val layoutInflater =
                AppObjectController.joshApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = layoutInflater.inflate(R.layout.award_view_holder, binding.rootView, false)
            val title = view.findViewById(R.id.title) as AppCompatTextView
            title.text = "Senior Student"
            setSeniorStudentAwardView(view!!)
            if (view != null) {
                binding.multiLineLl.addView(view)
            }
        }

        if (userData.awardCategory.isNullOrEmpty()) {
            binding.labelViewMoreAwards.visibility = View.GONE
            binding.awardsLayout.isClickable=false
            if(!userData.isSeniorStudent) {
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
            this.awardCategory = userData.awardCategory
            binding.awardsLayout.visibility = View.VISIBLE
            binding.multiLineLl.visibility = View.VISIBLE
            if (checkIsAwardAchieved(userData.awardCategory)) {
                binding.labelViewMoreAwards.visibility = View.VISIBLE
                userData.awardCategory?.sortedBy { it.sortOrder }?.forEach { awardCategory ->
                    val view = getAwardLayoutItem(awardCategory)
                    if (view != null) {
                        binding.multiLineLl.addView(view)
                    }
                }
            } else {
                if(!userData.isSeniorStudent) {
                    binding.noAwardText.visibility = View.VISIBLE
                    binding.labelViewMoreAwards.visibility = View.GONE
                    binding.awardsLayout.isClickable=false
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
        if (userData.myGroupsList.isNullOrEmpty()) {
            binding.myGroupsLayout.visibility = View.GONE
            binding.myGroupsLl.visibility = View.GONE
        } else {
            binding.myGroupsLayout.visibility = View.VISIBLE
            binding.myGroupsLl.visibility = View.VISIBLE
            binding.labelMyGroups.text= "My Groups (${userData.myGroupsList.size})"
            binding.myGroupsLl.removeAllViews()
            var countGroups = 0
            userData.myGroupsList.forEach {
                if (countGroups < 3) {
                    val view = getMyGroupsLayoutItem(it)
                    if (view != null) {
                        binding.myGroupsLl.addView(view)
                        countGroups++
                    }
                }
            }
        }
        if (userData.enrolledCoursesList == null) {
            binding.enrolledCoursesLayout.visibility = View.GONE
            binding.enrolledCoursesLl.visibility = View.GONE
        } else {
            binding.enrolledCoursesLayout.visibility = View.VISIBLE
            binding.enrolledCoursesLl.visibility = View.VISIBLE
            binding.labelEnrolledCourses.text = userData.enrolledCoursesList.label
            binding.enrolledCoursesLl.removeAllViews()
            var countCourses = 0
            userData.enrolledCoursesList.courses.forEach { course ->
                if (countCourses < 3) {
                    val view = getEnrolledCourseLayoutItem(course)
                    if (view != null) {
                        binding.enrolledCoursesLl.addView(view)
                        countCourses++
                    }
                }
            }
        }

        if(helpCountControl) {
            if (mentorId == Mentor.getInstance().getId()) {
                binding.referralInfoText.visibility = VISIBLE
                if (userData.numberOfReferral != 0) {
                    val text = SpannableStringBuilder()
                        .append("You have helped ")
                        .bold { append(userData.numberOfReferral.toString() + " people") }
                        .append(" start learning English")
                    binding.referralInfoText.text = text
                } else {
                    binding.referralInfoText.text = getString(R.string.help_text_me)
                }
            } else {
                if (userData.numberOfReferral != 0) {
                    binding.referralInfoText.visibility = VISIBLE
                    val text = SpannableStringBuilder()
                        .append(resp.trim().split(" ")[0] + " has helped ")
                        .bold { append(userData.numberOfReferral.toString() + " people") }
                        .append(" start learning English")
                    binding.referralInfoText.text = text
                } else {
                    binding.referralInfoText.visibility = GONE
                }
            }
        }

        binding.scrollView.fullScroll(ScrollView.FOCUS_UP)
    }

    private fun setSeniorStudentAwardView(view: View) {
        var v: View? = view.findViewById<ConstraintLayout>(R.id.award1)
        v?.visibility = View.VISIBLE
        var image: ImageView = view.findViewById(R.id.image_award1)
        var title: AppCompatTextView = view.findViewById(R.id.title_award1)
        var date: AppCompatTextView = view.findViewById(R.id.date_award1)
        var count: AppCompatTextView = view.findViewById(R.id.txt_count_award1)
        date.visibility =  GONE
        title.visibility =  GONE
        count.visibility =  GONE
        image.setImageResource(R.drawable.senior_student_with_shadow)
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
    @SuppressLint("WrongViewCast")
    private fun getMyGroupsLayoutItem(groupInfo: GroupInfo): View? {
        val layoutInflater =
            AppObjectController.joshApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view =
            layoutInflater.inflate(R.layout.my_groups_row_item, binding.rootView, false)
        val txtGroupName = view.findViewById(R.id.tv_group_name) as AppCompatTextView
        val txtMinutesSpoken= view.findViewById(R.id.tv_minutes_spoken) as AppCompatTextView
        val imggGroupIcon = view.findViewById(R.id.group_icon) as CircleImageView

        txtGroupName.text = groupInfo.groupName
        txtMinutesSpoken.text =groupInfo.textToShow
        if(groupInfo.groupIcon==null){
            imggGroupIcon.setImageResource(R.drawable.group_default_icon)

        }else{
            setImage(imggGroupIcon, groupInfo.groupIcon)
        }
        return view
    }

    @SuppressLint("WrongViewCast")
    private fun getEnrolledCourseLayoutItem(course: CourseEnrolled): View? {
        val layoutInflater =
            AppObjectController.joshApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view =
            layoutInflater.inflate(R.layout.enrolled_courses_row_item, binding.rootView, false)
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
                v.visibility = View.VISIBLE

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
                v.visibility = View.VISIBLE

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
                v.visibility = View.VISIBLE
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
//        v?.setOnClickListener {
//            RxBus2.publish(
//                AwardItemClickedEventBus(award)
//            )
//        }
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
            date.visibility = View.INVISIBLE
        } else {
            date.visibility = View.VISIBLE
            date.text = award.dateText
        }
        award.imageUrl?.let {
            image.setImage(it, this)
        }
        if (award.count > 1) {
            count.visibility = View.VISIBLE
            count.text = award.count.toString()
        } else {
            count.visibility = View.GONE
        }
    }

    private fun getProfileData(intervalType: String?, previousPage: String?) {
        viewModel.getProfileData(mentorId, intervalType, previousPage)
    }

    fun showAllAwards() {
        awardCategory?.let {
            SeeAllAwardActivity.startSeeAllAwardActivity(this, it, this.isSeniorStudent)
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
                            viewModel.saveProfileInfo("")
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

    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount
        if (isAnimationVisible) {
            hideOverlayAnimation()
            return
        }
        if (count == 0) {
            startTime = System.currentTimeMillis().minus(startTime).div(1000)
            if (startTime > 0 && impressionId.isBlank().not()) {
                viewModel.engageUserProfileTime(impressionId, startTime)
            }
            super.onBackPressed()
            // additional code
        } else {
            supportFragmentManager.popBackStack()
        }
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
            isFromConversationRoom: Boolean=false,

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
        /*super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_SELECT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.getStringArrayListExtra(JoshCameraActivity.IMAGE_RESULTS)?.getOrNull(0)?.let {
                if (it.isNotBlank()) {
                    addUserImageInView(it)
                }
            }
        }*/
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

    /*private fun animatePoints() {
        isPointAnimatorCancel = false
        pointAnimator.start()
    }*/

    override fun onStart() {
        super.onStart()
        if (!PrefManager.getBoolValue(HAS_SEEN_PROFILE_ANIMATION))
            showOverlayAnimation()
    }

    /*private fun stopPointAnimation() {
        isPointAnimatorCancel = true
        pointAnimator.cancel()
        binding.points.scaleX = 1f
        binding.points.scaleY = 1f
    }*/

    private fun showOverlayAnimation() {
        lifecycleScope.launch(Dispatchers.Main) {
            delay(1000)
            binding.contentOverlay.visibility = View.VISIBLE
            binding.arrowAnimation.visibility = View.VISIBLE
            /*binding.arrowAnimation.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    animatePoints()
                    Log.d(TAG, "onAnimationStart: ")
                }

                override fun onAnimationEnd(animation: Animator?) {
                    Log.d(TAG, "onAnimationEnd: ")
                }

                override fun onAnimationCancel(animation: Animator?) {
                    Log.d(TAG, "onAnimationCancel: ")
                }

                override fun onAnimationRepeat(animation: Animator?) {
                    Log.d(TAG, "onAnimationRepeat: ")
                }
            })*/
            binding.arrowAnimation.playAnimation()
            binding.toolbarOverlay.visibility = View.VISIBLE
            //animatePoints()
            binding.labelTapToDismiss.visibility = View.INVISIBLE
            binding.overlayProfileTooltip.visibility = View.VISIBLE
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

            /*binding.scrollView.setOnClickListener {
                hideOverlayAnimation()
            }
*/
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
        binding.arrowAnimation.visibility = View.GONE
        binding.contentOverlay.visibility = View.GONE
        binding.toolbarOverlay.visibility = View.GONE
        binding.labelTapToDismiss.visibility = View.INVISIBLE
        binding.overlayProfileTooltip.visibility = View.GONE
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

    fun getUserProfileTooltip() :String {
        val courseId = PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID)
        return AppObjectController.getFirebaseRemoteConfig()
            .getString(TOOLTIP_USER_PROFILE_SCREEN + courseId)
    }

    fun openShareScreen(){
        ShareFromProfile.startShareFromProfile(
            this,
            viewerReferral
        )
    }
}
