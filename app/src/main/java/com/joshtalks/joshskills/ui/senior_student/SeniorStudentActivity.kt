package com.joshtalks.joshskills.ui.senior_student

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivitySeniorStudentBinding
import com.joshtalks.joshskills.databinding.SeniorStudentRvItemBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.senior_student.viewmodel.SeniorStudentViewModel
import com.joshtalks.joshskills.ui.settings.SettingsActivity
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity

class SeniorStudentActivity : CoreJoshActivity() {
    lateinit var binding: ActivitySeniorStudentBinding
    private var popupMenu: PopupMenu? = null
    val vm by lazy {
        ViewModelProvider(this)[SeniorStudentViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_senior_student)
        binding.vm = vm
        showProgressBar()
        lifecycleScope.launchWhenCreated {
            vm.fetchSeniorStudentData()
            hideProgressBar()
        }
        initToolbar()
    }

    private fun initToolbar() {
        val backBtnView = binding.toolbarContainer.findViewById<AppCompatImageView>(R.id.iv_back)
        val helpBtnView = binding.toolbarContainer.findViewById<AppCompatImageView>(R.id.iv_help)
        val settingsBtnView = binding.toolbarContainer.findViewById<AppCompatImageView>(R.id.iv_setting)
        val toolbarTitleView = binding.toolbarContainer.findViewById<AppCompatTextView>(R.id.text_message_title)
        backBtnView.visibility = View.VISIBLE
        backBtnView.setOnClickListener {
            onBackPressed()
        }
        settingsBtnView.setOnClickListener {
            openPopupMenu(it)
        }
        toolbarTitleView.visibility = View.VISIBLE
        helpBtnView.visibility = View.GONE
        settingsBtnView.visibility = View.VISIBLE
        toolbarTitleView.text = "Become a Senior Student"
    }

    private fun openPopupMenu(view: View) {
        if (popupMenu == null) {
            popupMenu = PopupMenu(this, view, R.style.setting_menu_style)
            popupMenu?.inflate(R.menu.more_options_menu)
            popupMenu?.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_referral -> {
                        AppAnalytics
                            .create(AnalyticsEvent.REFER_BUTTON_CLICKED.NAME)
                            .addBasicParam()
                            .addUserDetails()
                            .addParam(
                                AnalyticsEvent.REFERRAL_CODE.NAME,
                                Mentor.getInstance().referralCode
                            )
                            .push()
                        ReferralActivity.startReferralActivity(this)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.menu_help -> {
                        openHelpActivity()
                    }
                    R.id.menu_settings ->
                        openSettingActivity()
                }
                return@setOnMenuItemClickListener false
            }
        }
        popupMenu?.show()
    }

    private fun openSettingActivity() {
        openSettingActivity.launch(SettingsActivity.getIntent(this))
    }

    companion object {
        fun startSeniorStudentActivity(activity: Activity) {
            Intent(activity, UserProfileActivity::class.java).let {
                activity.startActivity(it)
            }
        }
    }
}