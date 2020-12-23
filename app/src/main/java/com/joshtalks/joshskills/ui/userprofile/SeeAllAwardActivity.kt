package com.joshtalks.joshskills.ui.userprofile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.databinding.FragmentSeeAllAwardBinding
import com.joshtalks.joshskills.repository.server.AwardCategory
import com.mindorks.placeholderview.PlaceHolderView
import kotlinx.android.synthetic.main.base_toolbar.iv_back
import kotlinx.android.synthetic.main.base_toolbar.iv_help
import kotlinx.android.synthetic.main.base_toolbar.text_message_title

class SeeAllAwardActivity : BaseActivity() {
    private lateinit var binding: FragmentSeeAllAwardBinding
    private lateinit var awardCategory: List<AwardCategory>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        awardCategory = intent.getParcelableArrayListExtra(AWARD_CATEGORY)
        binding = DataBindingUtil.setContentView(this, R.layout.fragment_see_all_award)
        binding.lifecycleOwner = this
        binding.fragment = this
        initRecyclerView()
        initToolbar()
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

        awardCategory.forEach { awardCategory ->
            val view = addLinerLayout(awardCategory)
            if (view != null) {
                binding.multiLineLl.addView(view)
            } else {

            }
        }
    }

    @SuppressLint("WrongViewCast")
    private fun addLinerLayout(awardCategory: AwardCategory): View? {
        val layoutInflater =
            AppObjectController.joshApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.award_view_holder, binding.rootView, false)
        val title = view.findViewById(R.id.title) as AppCompatTextView
        (view.findViewById(R.id.view) as View).visibility=View.GONE
        val recyclerView = view.findViewById(R.id.rv) as PlaceHolderView
        recyclerView.visibility=View.VISIBLE
        title.text = awardCategory.label
        val linearLayoutManager = GridLayoutManager(
            this, 3
        )
        linearLayoutManager.isSmoothScrollbarEnabled = true
        recyclerView.builder.setLayoutManager(linearLayoutManager)
        awardCategory.awards?.forEach {
            recyclerView.addView(AwardItemViewHolder(it, this))
        }
        return view
    }


    fun dismiss() {
        finish()
    }

    companion object {
        const val AWARD_CATEGORY = "award_category"

        fun startSeeAllAwardActivity(
            activity: Activity,
            awardCategory: List<AwardCategory>,
            flags: Array<Int> = arrayOf(),
        ) {
            Intent(activity, SeeAllAwardActivity::class.java).apply {
                putParcelableArrayListExtra(AWARD_CATEGORY, ArrayList(awardCategory))
                flags.forEach { flag ->
                    this.addFlags(flag)
                }
            }.run {
                activity.startActivity(this)
            }
        }
    }
}