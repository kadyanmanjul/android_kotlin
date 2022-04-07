package com.joshtalks.joshskills.ui.userprofile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.databinding.FragmentSeeAllAwardBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.AwardItemClickedEventBus
import com.joshtalks.joshskills.ui.userprofile.models.Award
import com.joshtalks.joshskills.ui.userprofile.models.AwardCategory
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.userprofile.viewmodel.UserProfileViewModel
import com.mindorks.placeholderview.PlaceHolderView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.base_toolbar.iv_back
import kotlinx.android.synthetic.main.base_toolbar.iv_help
import kotlinx.android.synthetic.main.base_toolbar.text_message_title

class SeeAllAwardActivity : BaseActivity() {
    private lateinit var binding: FragmentSeeAllAwardBinding
    private lateinit var awardCategory: List<AwardCategory>
    private var isSeniorStudent: Boolean = false
    private val compositeDisposable = CompositeDisposable()
    private var mentorId: String? = null
    private var startTime = 0L
    private var impressionId: String? = null
    private val viewModel by lazy {
        ViewModelProvider(this).get(
            UserProfileViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        awardCategory =
            intent.getParcelableArrayListExtra<AwardCategory>(AWARD_CATEGORY) as List<AwardCategory>
        isSeniorStudent = intent.extras!!.getBoolean(IS_SENIOR)
        binding = DataBindingUtil.setContentView(this, R.layout.fragment_see_all_award)
        binding.lifecycleOwner = this
        binding.fragment = this
        mentorId = intent.getStringExtra(MENTOR_ID)
        startTime = System.currentTimeMillis()
        addObservers()
        mentorId?.let { viewModel.userProfileSectionImpression(it, "AWARD") }
        initRecyclerView()
        initToolbar()
    }

    private fun addObservers() {
        viewModel.sectionImpressionResponse.observe(this) {
            impressionId = it.sectionImpressionId
        }

    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
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
        text_message_title.text = getString(R.string.awards)
    }

    private fun initRecyclerView() {
//        if (isSeniorStudent) {
//            val layoutInflater =
//                AppObjectController.joshApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//            val view = layoutInflater.inflate(R.layout.award_view_holder, binding.rootView, false)
//            val title = view.findViewById(R.id.title) as AppCompatTextView
//            title.text = "Senior Student"
//            setSeniorStudentAwardView(view!!)
//            if (view != null) {
//                binding.multiLineLl.addView(view)
//            }
//        }

        awardCategory.forEach{
            it.awards?.forEach {
                val view = addLinerLayout(it)
                if (view != null) {
                    binding.multiLineLl.addView(view)
                } else {

                }
            }
        }

//        awardCategory.first().awards?.forEach {
//            val view = addLinerLayout(it)
//            if (view != null) {
//                binding.multiLineLl.addView(view)
//            } else {
//
//            }
//        }
    }

    private fun setSeniorStudentAwardView(view: View) {
        var v: View? = view.findViewById<ConstraintLayout>(R.id.award1)
        v?.visibility = View.VISIBLE
        var image: ImageView = view.findViewById(R.id.image_award1)
        var title: AppCompatTextView = view.findViewById(R.id.title_award1)
        var date: AppCompatTextView = view.findViewById(R.id.date_award1)
        var count: AppCompatTextView = view.findViewById(R.id.txt_count_award1)
        date.visibility = View.GONE
        title.visibility = View.GONE
        count.visibility = View.GONE
        image.setImageResource(R.drawable.senior_student_with_shadow)
    }

    override fun onBackPressed() {
        startTime = System.currentTimeMillis().minus(startTime).div(1000)
        impressionId?.let {
            if (startTime > 0 && impressionId!!.isBlank().not()) {
                viewModel.engageUserProfileSectionTime(impressionId!!, startTime.toString())
            }
        }

        super.onBackPressed()

    }

    @SuppressLint("WrongViewCast")
    private fun addLinerLayout(award: Award): View? {
        val layoutInflater =
            AppObjectController.joshApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.award_view_holder, binding.rootView, false)
        val title = view.findViewById(R.id.title) as AppCompatTextView
//        val viewDivider = view.findViewById(R.id.view) as View
//        viewDivider.visibility = View.VISIBLE
        val recyclerView = view.findViewById(R.id.rv) as PlaceHolderView
        recyclerView.visibility = View.VISIBLE
//        title.text = awardCategory.label
        val linearLayoutManager = GridLayoutManager(
            this, 3
        )
        linearLayoutManager.isSmoothScrollbarEnabled = true
        recyclerView.builder.setLayoutManager(linearLayoutManager)
        var localAward: Award = award
        title.setText(award.awardText)
        award.dateList?.forEach {

            recyclerView.addView(AwardItemViewHolder(localAward, it, this))
        }
        recyclerView.isNestedScrollingEnabled = true

        return view
    }


    fun dismiss() {
        finish()
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
                    openAwardPopUp(it.award)
                }, {
                    it.printStackTrace()
                })
        )
    }

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }

    private fun openAwardPopUp(award: Award) {
//        showAward(listOf(award), true)
    }

    companion object {
        const val AWARD_CATEGORY = "award_category"
        const val IS_SENIOR = "IsSenior"
        const val MENTOR_ID = "MentorId"

        fun startSeeAllAwardActivity(
            activity: Activity,
            awardCategory: List<AwardCategory>,
            isSeniorStudent: Boolean,
            mentorId: String? = null,
            conversationId: String? = null,
            flags: Array<Int> = arrayOf(),
        ) {
            Intent(activity, SeeAllAwardActivity::class.java).apply {
                putExtra(CONVERSATION_ID, conversationId)
                putParcelableArrayListExtra(AWARD_CATEGORY, ArrayList(awardCategory))
                putExtra(IS_SENIOR, isSeniorStudent)
                putExtra(MENTOR_ID, mentorId)
                flags.forEach { flag ->
                    this.addFlags(flag)
                }
            }.run {
                activity.startActivity(this)
            }
        }
    }
}