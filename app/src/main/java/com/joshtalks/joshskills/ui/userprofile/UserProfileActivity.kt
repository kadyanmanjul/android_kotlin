package com.joshtalks.joshskills.ui.userprofile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.ActivityUserProfileBinding
import com.joshtalks.joshskills.repository.server.Award
import com.joshtalks.joshskills.repository.server.AwardCategory
import com.joshtalks.joshskills.repository.server.UserProfileResponse
import com.mindorks.placeholderview.PlaceHolderView
import com.mindorks.placeholderview.SmoothLinearLayoutManager

class UserProfileActivity : AppCompatActivity() {

    lateinit var binding: ActivityUserProfileBinding
    private var mentorId: String = EMPTY
    private var viewCount: Int = 0

    private val viewModel by lazy {
        ViewModelProvider(this).get(
            UserProfileViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mentorId = intent.getStringExtra(KEY_MENTOR_ID)
        if (mentorId.isNullOrEmpty()) {
            this.finish()
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_profile)
        binding.lifecycleOwner = this
        binding.handler = this
        addObserver()
        //initRecyclerView()
        getProfileData()
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
                binding.progressLayout.visibility = View.GONE
                initView(it)
            }
        })

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
        userData.awardCategory?.let {
            binding.awardsHeading.visibility = View.VISIBLE
            binding.moreInfo.visibility = View.VISIBLE
            it.forEach { awardCategory ->
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
                    certificate.imageUrl
                )
            )
        }
        binding.multiLineLl.addView(addLinerLayout(AwardCategory(null, null, null, listAwardss)))
    }

    @SuppressLint("WrongViewCast")
    private fun addLinerLayout(awardCategory: AwardCategory): View? {
        val layoutInflater =
            AppObjectController.joshApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.award_view_holder, binding.rootView, false)
        val title = view.findViewById(R.id.title) as AppCompatTextView
        val recyclerView = view.findViewById(R.id.award_rv) as PlaceHolderView
        var text = awardCategory.label
        if (awardCategory.label == null) {
            text = "Certificates"
        }
        title.text = text
        val linearLayoutManager = SmoothLinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            true
        )
        linearLayoutManager.isSmoothScrollbarEnabled = true
        recyclerView.builder.setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)

        awardCategory.awards?.forEach {
            recyclerView.addView(AwardItemViewHolder(it, this))
        }
        recyclerView.getLayoutManager()?.scrollToPosition(0);
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