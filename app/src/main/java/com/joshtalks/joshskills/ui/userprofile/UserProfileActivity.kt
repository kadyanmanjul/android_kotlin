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
import android.widget.ScrollView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.IS_PROFILE_FEATURE_ACTIVE
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.databinding.ActivityUserProfileBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.AwardItemClickedEventBus
import com.joshtalks.joshskills.repository.local.eventbus.DeleteProfilePicEventBus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.Award
import com.joshtalks.joshskills.repository.server.AwardCategory
import com.joshtalks.joshskills.repository.server.UserProfileResponse
import com.joshtalks.joshskills.repository.server.chat_message.TImageMessage
import com.joshtalks.joshskills.ui.extra.ImageShowFragment
import com.mindorks.placeholderview.PlaceHolderView
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.base_toolbar.iv_back
import kotlinx.android.synthetic.main.base_toolbar.iv_help
import kotlinx.android.synthetic.main.base_toolbar.iv_setting
import kotlinx.android.synthetic.main.base_toolbar.text_message_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserProfileActivity : BaseActivity() {

    lateinit var binding: ActivityUserProfileBinding
    private var mentorId: String = EMPTY
    private var viewCount: Int = 0
    private val compositeDisposable = CompositeDisposable()

    private val viewModel by lazy {
        ViewModelProvider(this).get(
            UserProfileViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mentorId = intent.getStringExtra(KEY_MENTOR_ID)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_profile)
        binding.lifecycleOwner = this
        binding.handler = this
        addObserver()
        //initRecyclerView()
        initToolbar()
        getProfileData()
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        binding.userPic.setOnClickListener {
            if (mentorId.equals(Mentor.getInstance().getId())) {
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
            if (mentorId.equals(Mentor.getInstance().getId())) {
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
        if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE) && mentorId.equals(
                Mentor.getInstance().getId()
            )
        ) {
            with(iv_setting) {
                visibility = View.VISIBLE
                setOnClickListener {
                    openPopupMenu(it)
                }
            }
        }
        binding.pointLayout.setOnClickListener {
                openPointHistory(mentorId)
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
        viewModel.userData.observe(this, Observer {
            it?.let {
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
        binding.userName.text = userData.name
        binding.userAge.text = userData.age.toString()
        binding.joinedOn.text = userData.joinedOn
        binding.points.text = userData.points.toString()
        binding.streaks.text = userData.streak.toString().plus(" Days")

        if (userData.awardCategory.isNullOrEmpty()) {
            binding.awardsHeading.visibility = View.GONE
        } else {
            binding.awardsHeading.visibility = View.VISIBLE
            binding.moreInfo.visibility = View.VISIBLE
            userData.awardCategory?.forEach { awardCategory ->
                val view = addLinerLayout(awardCategory)
                if (view != null) {
                    binding.multiLineLl.addView(view)
                } else {

                }
            }
        }
        binding.scrollView.fullScroll(ScrollView.FOCUS_UP)
    }

    private fun openChooser() {
        UserPicChooserFragment.showDialog(supportFragmentManager)
    }


    @SuppressLint("WrongViewCast")
    private fun addLinerLayout(awardCategory: AwardCategory): View? {
        val layoutInflater =
            AppObjectController.joshApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.award_view_holder, binding.rootView, false)
        val title = view.findViewById(R.id.title) as AppCompatTextView
        val recyclerView = view.findViewById(R.id.award_rv) as PlaceHolderView
        title.text = awardCategory.label
        val linearLayoutManager = SmoothLinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        linearLayoutManager.isSmoothScrollbarEnabled = true
        recyclerView.builder.setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)

        var haveAchievedAwards = false

        awardCategory.awards?.forEach {
            if (mentorId.equals(Mentor.getInstance().getId())) {
                recyclerView.addView(AwardItemViewHolder(it, this))
            } else if (it.is_achieved) {
                haveAchievedAwards = true
                recyclerView.addView(AwardItemViewHolder(it, this))
            }
        }
        if (haveAchievedAwards.not() && mentorId.equals(Mentor.getInstance().getId()).not()) {
            return null
        }
        recyclerView.requestFocus(0)
        if (view != null) {
            viewCount = viewCount.plus(1)
        }
        if (viewCount > 3) {
            view.visibility = View.GONE
        }
        return view
    }

    private fun getProfileData() {
        viewModel.getProfileData(mentorId)
    }

    fun showAllAwards() {
        binding.moreInfo.visibility = View.GONE
        //viewModel.getProfileData(mentorId)
        for (i in 0 until binding.multiLineLl.childCount) {
            val view: View = binding.multiLineLl.getChildAt(i)
            view.visibility = View.VISIBLE
        }
        binding.multiLineLl
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
                    if (mentorId.equals(Mentor.getInstance().getId()))
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
            result.getData()?.data?.let {
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

    fun openSomeActivityForResult() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activityResultLauncher.launch(galleryIntent)
    }

    private fun openAwardPopUp(award: Award) {
        showAward(listOf(award),true)
    }

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }

    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount
        if (count == 0) {
            super.onBackPressed()
            //additional code
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    private fun addUserImageInView(imagePath: String) {
        val imageUpdatedPath = AppDirectory.getImageSentFilePath()
        AppDirectory.copy(imagePath, imageUpdatedPath)
        val tImageMessage = TImageMessage(imageUpdatedPath, imageUpdatedPath)
        viewModel.uploadMedia(imageUpdatedPath)
    }

    companion object {
        const val KEY_MENTOR_ID = "leaderboard-mentor-id"

        fun startUserProfileActivity(
            activity: Activity,
            mentorId: String,
            flags: Array<Int> = arrayOf(),
        ) {
            Intent(activity, UserProfileActivity::class.java).apply {
                putExtra(KEY_MENTOR_ID, mentorId)
                flags.forEach { flag ->
                    this.addFlags(flag)
                }
            }.run {
                activity.startActivity(this)
            }
        }
    }
}