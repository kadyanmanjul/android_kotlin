package com.joshtalks.joshskills.ui.userprofile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ScrollView
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
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.IS_LEADERBOARD_ACTIVE
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.ActivityUserProfileBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.AwardItemClickedEventBus
import com.joshtalks.joshskills.repository.server.Award
import com.joshtalks.joshskills.repository.server.AwardCategory
import com.joshtalks.joshskills.repository.server.UserProfileResponse
import com.mindorks.placeholderview.PlaceHolderView
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.base_toolbar.iv_back
import kotlinx.android.synthetic.main.base_toolbar.iv_help
import kotlinx.android.synthetic.main.base_toolbar.iv_setting
import kotlinx.android.synthetic.main.base_toolbar.text_message_title

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
        if (PrefManager.getBoolValue(IS_LEADERBOARD_ACTIVE)) {
            with(iv_setting) {
                visibility = View.VISIBLE
                setOnClickListener {
                    openPopupMenu(it)
                }
            }
        }
    }

    private fun openPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view, R.style.setting_menu_style)
        popupMenu.inflate(R.menu.user_profile__menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_points_history -> {
                    openPointHistory()
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

    }

    private fun initView(userData: UserProfileResponse) {
        binding.userName.text = userData.name
        binding.userAge.text = userData.age.toString()
        binding.joinedOn.text = userData.joinedOn
        userData.photoUrl?.let { photoUrl ->
            binding.userPic.setImage(photoUrl, this)
        }
        binding.points.text = userData.points.toString()
        binding.streaks.text = userData.streak.toString().plus(" Days")


        /*userData.sortedMap?.forEach { (_, listAward) ->
            binding.awardRv.addView(AwardViewHolder(listAward, this))
        }
        val listAwardss: ArrayList<Award> = ArrayList()
        userData.certificates?.map { certificate ->
            listAwardss.add(
                Award(
                    certificate.certificateText,
                    null,
                    certificate.dateText,
                    certificate.imageUrl
                )
            )
        }
        binding.awardRv.addView(AwardViewHolder(listAwardss, this))*/

        if (userData.awardCategory.isNullOrEmpty()) {
            binding.awardsHeading.visibility = View.GONE
        } else {
            binding.awardsHeading.visibility = View.VISIBLE
            binding.moreInfo.visibility = View.VISIBLE
            userData.awardCategory.forEach { awardCategory ->
                binding.multiLineLl.addView(addLinerLayout(awardCategory))
            }
        }
        val listAwardss: ArrayList<Award> = ArrayList()
        userData.certificates?.map { certificate ->
            listAwardss.add(
                Award(
                    certificate.certificateText,
                    certificate.sortOrder,
                    certificate.dateText,
                    certificate.imageUrl,
                    certificate.certificateDescription
                )
            )
        }
        binding.multiLineLl.addView(addLinerLayout(AwardCategory(null, null, null, listAwardss)))
        binding.scrollView.fullScroll(ScrollView.FOCUS_UP)
    }

    @SuppressLint("WrongViewCast")
    private fun addLinerLayout(awardCategory: AwardCategory): View? {
        val layoutInflater =
            AppObjectController.joshApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.award_view_holder, binding.rootView, false)
        val title = view.findViewById(R.id.title) as AppCompatTextView
        val recyclerView = view.findViewById(R.id.award_rv) as PlaceHolderView
        var text = awardCategory.label
        if (awardCategory.label == null && awardCategory.awards.isNullOrEmpty().not()) {
            text = "Certificates"
        }
        title.text = text
        val linearLayoutManager = SmoothLinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        linearLayoutManager.isSmoothScrollbarEnabled = true
        recyclerView.builder.setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)

        awardCategory.awards?.forEach {
            recyclerView.addView(AwardItemViewHolder(it, this))
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
                    if (AppObjectController.getFirebaseRemoteConfig()
                            .getBoolean(FirebaseRemoteConfigKey.SHOW_AWARDS_FULL_SCREEN)
                    ) {
                        openAwardPopUp(it.award)
                    }
                }, {
                    it.printStackTrace()
                })
        )
    }

    private fun openAwardPopUp(award: Award) {
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.parent_Container,
                ShowAwardFragment.newInstance(award),
                "Show Award Fragment"
            )
            .addToBackStack(null)
            .commit()
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