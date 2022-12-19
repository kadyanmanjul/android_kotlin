package com.joshtalks.joshskills.common.ui.senior_student

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.CoreJoshActivity
import com.joshtalks.joshskills.common.core.SettingsContract
import com.joshtalks.joshskills.common.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.common.core.analytics.AppAnalytics
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.common.databinding.ActivitySeniorStudentBinding
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.ui.senior_student.viewmodel.SeniorStudentViewModel

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
        val settingsBtnView =
            binding.toolbarContainer.findViewById<AppCompatImageView>(R.id.iv_setting)
        val toolbarTitleView =
            binding.toolbarContainer.findViewById<AppCompatTextView>(R.id.text_message_title)
        backBtnView.visibility = View.VISIBLE
        backBtnView.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
            onBackPressed()
        }
//        settingsBtnView.setOnClickListener {
//            openPopupMenu(it)
//        }
        helpBtnView.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.HELP).push()
            openHelpActivity()
        }
        toolbarTitleView.visibility = View.VISIBLE
        helpBtnView.visibility = View.VISIBLE
        settingsBtnView.visibility = View.GONE
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

                        // TODO: Use navigator -- Sahil
//                        com.joshtalks.joshskills.referral.ReferralActivity.startReferralActivity(this)
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
        //TODO: Replace AppObjectController with intent navigator -- Sukesh
        AppObjectController.navigator.with(this).navigate(
            object : SettingsContract {
                override val navigator = AppObjectController.navigator
            }
        )
    }

    companion object {
        fun startSeniorStudentActivity(activity: Activity) {
            Intent(activity, SeniorStudentActivity::class.java).let {
                activity.startActivity(it)
            }
        }
    }
}