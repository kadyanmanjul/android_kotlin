package com.joshtalks.joshskills.ui.userprofile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.ScrollView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshcamerax.JoshCameraActivity
import com.joshtalks.joshcamerax.utils.Options
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.databinding.ActivityUserProfileBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.AwardItemClickedEventBus
import com.joshtalks.joshskills.repository.local.eventbus.DeleteProfilePicEventBus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.Award
import com.joshtalks.joshskills.repository.server.AwardCategory
import com.joshtalks.joshskills.repository.server.UserProfileResponse
import com.joshtalks.joshskills.ui.chat.IMAGE_SELECT_REQUEST_CODE
import com.joshtalks.joshskills.ui.extra.ImageShowFragment
import com.joshtalks.joshskills.ui.points_history.PointsInfoActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.text.DecimalFormat
import java.util.*
import kotlinx.android.synthetic.main.base_toolbar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserProfileActivity : BaseActivity() {

    lateinit var binding: ActivityUserProfileBinding
    private var mentorId: String = EMPTY
    private var impressionId: String = EMPTY
    private var intervalType: String? = EMPTY
    private var previousPage: String? = EMPTY
    private val compositeDisposable = CompositeDisposable()
    private var awardCategory: List<AwardCategory>? = emptyList()
    private var startTime = 0L

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
        addObserver()
        startTime = System.currentTimeMillis()
        initToolbar()
        getProfileData(intervalType, previousPage)
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        binding.pointLayout.setOnClickListener {
            openPointHistory(mentorId)
        }

        binding.userPic.setOnClickListener {
            if (mentorId == Mentor.getInstance().getId()) {
                if (viewModel.getUserProfileUrl().isNullOrBlank().not()) {
                    ImageShowFragment.newInstance(viewModel.getUserProfileUrl(), null, null)
                        .show(supportFragmentManager, "ImageShow")
                } else {
                    openChooser()
                }
            } else {
                if (viewModel.getUserProfileUrl().isNullOrBlank().not()) {
                    ImageShowFragment.newInstance(viewModel.getUserProfileUrl(), null, null)
                        .show(supportFragmentManager, "ImageShow")
                }
            }
        }
        binding.editPic.setOnClickListener {
            if (mentorId == Mentor.getInstance().getId()) {
                openChooser()
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
            visibility = View.VISIBLE
            setOnClickListener {
                openHelpActivity()
            }
        }
        text_message_title.text = getString(R.string.profile)
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
            binding.editPic.visibility = View.GONE
        }
    }

    private fun openPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view, R.style.setting_menu_style)
        popupMenu.inflate(R.menu.user_profile__menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_points_history -> {
                    openPointHistory(mentorId)
                }
                R.id.how_to_get_points -> {
                    startActivity(Intent(this, PointsInfoActivity::class.java))
                }
                R.id.change_dp -> {
                    openChooser()
                }
            }
            return@setOnMenuItemClickListener false
        }
        popupMenu.show()
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
        viewModel.userData.observe(this, {
            it?.let {
                impressionId = it.userProfileImpressionId ?: EMPTY
                hideProgressBar()
                initView(it)
            }
        })

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
                    binding.editPic.visibility = View.GONE
                }
            }
            binding.userPic.post {
                binding.userPic.setUserImageOrInitials(
                    url = it,
                    viewModel.userData.value?.name ?: "Name",
                    28
                )
            }
        }

        viewModel.apiCallStatus.observe(this)
        {
            if (it == ApiCallStatus.SUCCESS) {
                hideProgressBar()
            } else if (it == ApiCallStatus.FAILED) {
                hideProgressBar()
            } else if (it == ApiCallStatus.START) {
                showProgressBar()
            }
        }

    }

    private fun initView(userData: UserProfileResponse) {
        val resp = StringBuilder()
        userData.name?.split(" ")?.forEachIndexed { index, string ->
            if (index < 2) {
                resp.append(string.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault()))
                    .append(" ")
            }
        }
        binding.userName.text = resp
        binding.userAge.text = userData.age.toString()
        binding.joinedOn.text = userData.joinedOn

        binding.points.text = DecimalFormat("#,##,##,###").format(userData.points)
        binding.streaksText.text = getString(R.string.user_streak_text, userData.streak)

        if (userData.awardCategory.isNullOrEmpty()) {
            binding.awardsHeading.visibility = View.GONE
        } else {
            this.awardCategory = userData.awardCategory

            if (mentorId == Mentor.getInstance().getId()) {
                binding.moreInfo.visibility = View.VISIBLE
            }
            binding.awardsHeading.visibility = View.VISIBLE
            if (checkIsAwardAchieved(userData.awardCategory)) {
                userData.awardCategory?.forEach { awardCategory ->
                    val view = addLinerLayout(awardCategory)
                    if (view != null) {
                        binding.multiLineLl.addView(view)
                    }
                }
            } else {
                binding.noAwardIcon.visibility = View.VISIBLE
                binding.noAwardText.visibility = View.VISIBLE
            }
        }
        binding.scrollView.fullScroll(ScrollView.FOCUS_UP)
    }

    private fun checkIsAwardAchieved(awardCategory: List<AwardCategory>?): Boolean {
        if (mentorId == Mentor.getInstance().getId()) {
            return true
        }
        if (awardCategory.isNullOrEmpty()) {
            return false
        } else {
            awardCategory.forEach {
                it.awards?.forEach {
                    if (it.is_achieved) {
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
            viewModel.getUserProfileUrl().isNullOrBlank()
        )
    }

    private fun getPermissionAndImage() {
        PermissionUtils.storageReadAndWritePermission(this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            RxBus2.publish(DeleteProfilePicEventBus("No d"))
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(this@UserProfileActivity)
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
    }


    @SuppressLint("WrongViewCast")
    private fun addLinerLayout(awardCategory: AwardCategory): View? {
        val layoutInflater =
            AppObjectController.joshApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.award_view_holder, binding.rootView, false)
        val title = view.findViewById(R.id.title) as AppCompatTextView
        title.text = awardCategory.label
        var haveAchievedAwards = false
        var index = 0

        awardCategory.awards?.forEach {
            if (mentorId == Mentor.getInstance().getId()) {
                setAwardView(it, index, view!!)
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

    private fun setAwardView(award: Award, index: Int, view: View) {
        when (index) {
            0 -> {
                view.findViewById<ConstraintLayout>(R.id.award1).visibility = View.VISIBLE
                setViewToLayout(
                    award,
                    view.findViewById(R.id.image_award1),
                    view.findViewById(R.id.title_award1),
                    view.findViewById(R.id.date_award1)
                )
            }
            1 -> {
                view.findViewById<ConstraintLayout>(R.id.award2).visibility = View.VISIBLE
                setViewToLayout(
                    award,
                    view.findViewById(R.id.image_award2),
                    view.findViewById(R.id.title_award2),
                    view.findViewById(R.id.date_award2)
                )
            }
            2 -> {
                view.findViewById<ConstraintLayout>(R.id.award3).visibility = View.VISIBLE
                setViewToLayout(
                    award,
                    view.findViewById(R.id.image_award3),
                    view.findViewById(R.id.title_award3),
                    view.findViewById(R.id.date_award3)
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
        date: AppCompatTextView
    ) {
        title.text = award.awardText
        date.text = award.dateText
        award.imageUrl?.let {
            image.setImage(it, this)
        }
    }

    private fun getProfileData(intervalType: String?, previousPage: String?) {
        viewModel.getProfileData(mentorId, intervalType, previousPage)
    }

    fun showAllAwards() {
        awardCategory?.let {
            SeeAllAwardActivity.startSeeAllAwardActivity(this, it)
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
                .subscribe({
                    if (mentorId == Mentor.getInstance().getId())
                        openAwardPopUp(it.award)
                }, {
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(
            RxBus2.listen(DeleteProfilePicEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    viewModel.userData.value?.photoUrl = it.url
                    if (it.url.isBlank()) {
                        viewModel.completingProfile("")
                    } else {
                        openSomeActivityForResult()
                    }
                }, {
                    it.printStackTrace()
                })
        )
    }

    var activityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        CoroutineScope(Dispatchers.IO).launch {

            // There are no request code
            result.data.data.let { it ->
                val selectedImage: Uri = it
                val filePathColumn = arrayOf<String>(MediaStore.Images.Media.DATA)
                var bitmap: Bitmap? = null
                // Get the cursor
                val cursor = contentResolver.query(
                    selectedImage, filePathColumn, null, null, null
                )

                cursor?.let {
                    it.moveToFirst()
                    val columnIndex: Int = cursor.getColumnIndex(filePathColumn[0])
                    val imgDecodableString: String = cursor.getString(columnIndex)
                    cursor.close()
                    addUserImageInView(imgDecodableString)

                }
            }
        }
    }

    fun selectImageActivity(options: Options) {
        val cameraIntent = Intent(this, JoshCameraActivity::class.java).apply {
            putExtra("options", options)
        }
        startActivityForResult(cameraIntent, IMAGE_SELECT_REQUEST_CODE)
    }

    fun openSomeActivityForResult() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activityResultLauncher.launch(galleryIntent)
    }

    private fun openAwardPopUp(award: Award) {
        showAward(listOf(award), true)
    }

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }

    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount
        if (count == 0) {
            startTime = System.currentTimeMillis().minus(startTime).div(1000)
            if (startTime > 0 && impressionId.isBlank().not()) {
                viewModel.engageUserProfileTime(impressionId, startTime)
            }
            super.onBackPressed()
            //additional code
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
            previousPage: String? = null
        ) {
            Intent(activity, UserProfileActivity::class.java).apply {
                putExtra(KEY_MENTOR_ID, mentorId)
                intervalType?.let {
                    putExtra(INTERVAL_TYPE, it)
                }
                previousPage?.let {
                    putExtra(PREVIOUS_PAGE, it)
                }
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
        if (requestCode == IMAGE_SELECT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.getStringArrayListExtra(JoshCameraActivity.IMAGE_RESULTS)?.getOrNull(0)?.let {
                if (it.isNotBlank()) {
                    addUserImageInView(it)
                }
            }
        }
    }
}