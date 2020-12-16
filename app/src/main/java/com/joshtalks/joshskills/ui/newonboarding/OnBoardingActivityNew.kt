package com.joshtalks.joshskills.ui.newonboarding

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.repository.server.onboarding.ONBOARD_VERSIONS
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.ui.newonboarding.fragment.OnBoardIntroFragment
import com.joshtalks.joshskills.ui.newonboarding.fragment.SelectCourseFragment
import com.joshtalks.joshskills.ui.newonboarding.fragment.SelectCourseHeadingFragment
import com.joshtalks.joshskills.ui.newonboarding.fragment.SelectInterestFragment
import com.joshtalks.joshskills.ui.newonboarding.viewmodel.OnBoardViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OnBoardingActivityNew : CoreJoshActivity() {

    lateinit var viewModel: OnBoardViewModel
    lateinit var progressLayout: FrameLayout

    companion object {
        const val FLOW_FROM_INBOX = "FLOW_FROM_INBOX"
        const val HAVE_COURSES = "HAVE_COURSES"

        fun startOnBoardingActivity(
            context: Activity,
            requestCode: Int,
            flowFromInbox: Boolean = false,
            alreadyHaveCourses: Boolean = false
        ) {
            val intent = Intent(context, OnBoardingActivityNew::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            intent.putExtra(FLOW_FROM_INBOX, flowFromInbox)
            intent.putExtra(HAVE_COURSES, alreadyHaveCourses)
            context.startActivityForResult(intent, requestCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding_new)
        viewModel = ViewModelProvider(this).get(OnBoardViewModel::class.java)
        progressLayout= findViewById<FrameLayout>(R.id.progress_layout)

        val haveCourses = intent.getBooleanExtra(HAVE_COURSES, false)
        if (intent.hasExtra(FLOW_FROM_INBOX)) {
            if (intent.getBooleanExtra(FLOW_FROM_INBOX, false)) {
                openCoursesFragment(haveCourses)
            } else {
                openOnBoardingIntroFragment()
            }
        } else openOnBoardingIntroFragment()

        addObserver()

    }

    private fun addObserver() {
        viewModel.courseRegistrationStatus.observe(this, Observer {
            if (it == ApiCallStatus.SUCCESS) {
                progressLayout.visibility= View.GONE
                startActivity((this as BaseActivity).getInboxActivityIntent(true))
            } else if(it == ApiCallStatus.FAILED){
                progressLayout.visibility= View.GONE
            }else {
                progressLayout.visibility= View.VISIBLE
            }
        })
    }

    private fun openCoursesFragment(haveCourses: Boolean) {
        when (VersionResponse.getInstance().version!!.name) {
            ONBOARD_VERSIONS.ONBOARDING_V1, ONBOARD_VERSIONS.ONBOARDING_V7, ONBOARD_VERSIONS.ONBOARDING_V8 -> {
                this.finish()
            }
            ONBOARD_VERSIONS.ONBOARDING_V2 -> {
                replaceFragment(
                    R.id.onboarding_container,
                    SelectCourseFragment.newInstance(),
                    SelectCourseFragment.TAG
                )
            }
            ONBOARD_VERSIONS.ONBOARDING_V3 -> {
                if (haveCourses) {
                    replaceFragment(
                        R.id.onboarding_container,
                        SelectCourseFragment.newInstance(true),
                        SelectCourseFragment.TAG
                    )
                } else {
                    replaceFragment(
                        R.id.onboarding_container,
                        SelectInterestFragment.newInstance(),
                        SelectInterestFragment.TAG
                    )
                }
            }
            ONBOARD_VERSIONS.ONBOARDING_V4 -> {
                if (haveCourses) {
                    replaceFragment(
                        R.id.onboarding_container,
                        SelectCourseFragment.newInstance(),
                        SelectCourseFragment.TAG
                    )
                } else {
                    replaceFragment(
                        R.id.onboarding_container,
                        SelectInterestFragment.newInstance(),
                        SelectInterestFragment.TAG
                    )
                }
            }
            ONBOARD_VERSIONS.ONBOARDING_V5 -> {
                if (haveCourses) {
                    replaceFragment(
                        R.id.onboarding_container,
                        SelectCourseFragment.newInstance(true),
                        SelectCourseFragment.TAG
                    )
                } else {
                    replaceFragment(
                        R.id.onboarding_container,
                        SelectCourseHeadingFragment.newInstance(),
                        SelectCourseHeadingFragment.TAG
                    )
                }
            }
            ONBOARD_VERSIONS.ONBOARDING_V6 -> {
                if (haveCourses) {
                    replaceFragment(
                        R.id.onboarding_container,
                        SelectCourseFragment.newInstance(true),
                        SelectCourseFragment.TAG
                    )
                } else {
                    replaceFragment(
                        R.id.onboarding_container,
                        SelectCourseHeadingFragment.newInstance(),
                        SelectCourseHeadingFragment.TAG
                    )
                }
            }
            ONBOARD_VERSIONS.ONBOARDING_V9 -> {
                replaceFragment(
                    R.id.onboarding_container,
                    SelectInterestFragment.newInstance(true),
                    SelectInterestFragment.TAG
                )
            }
            else ->{
                this.finish()
            }
        }
    }

    protected fun openOnBoardingIntroFragment() {
        replaceFragment(
            R.id.onboarding_container,
            OnBoardIntroFragment.newInstance(),
            OnBoardIntroFragment.TAG
        )
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val count = supportFragmentManager.backStackEntryCount
        if (count == 0) {
            super.onBackPressed()
            //additional code
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == 1343 && data != null) {
            if (data.hasExtra("result")) {
                val courseIds = data.getIntegerArrayListExtra("result")
                if (courseIds.isNotEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        supportFragmentManager.popBackStack()
                        VersionResponse.getInstance().version?.run {
                            if(name!==ONBOARD_VERSIONS.ONBOARDING_V6){
                                progressLayout.visibility=View.VISIBLE
                            }
                        }
                        viewModel.enrollMentorAgainstTest(courseIds.toList(),false)
                    }
                }
            }
        }
    }
}
