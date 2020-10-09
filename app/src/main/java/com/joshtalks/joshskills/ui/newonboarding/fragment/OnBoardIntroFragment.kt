package com.joshtalks.joshskills.ui.newonboarding.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bekawestberg.loopinglayout.library.LoopingLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.FragmentOnBoardIntroBinding
import com.joshtalks.joshskills.repository.server.onboarding.ONBOARD_VERSIONS
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.ui.newonboarding.adapter.OnBoardingIntroTextAdapter
import com.joshtalks.joshskills.ui.newonboarding.viewholder.CarouselImageViewHolder
import com.joshtalks.joshskills.ui.newonboarding.viewmodel.OnBoardViewModel
import com.joshtalks.joshskills.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.tyagiabhinav.dialogflowchatlibrary.Chatbot
import com.tyagiabhinav.dialogflowchatlibrary.ChatbotActivity
import com.tyagiabhinav.dialogflowchatlibrary.ChatbotSettings
import com.tyagiabhinav.dialogflowchatlibrary.DialogflowCredentials
import java.util.UUID

class OnBoardIntroFragment : Fragment() {
    var handler: Handler? = null
    lateinit var viewModel: OnBoardViewModel
    var width: Int = 0
    private var delay = 100L
    lateinit var binding: FragmentOnBoardIntroBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_on_board_intro, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(OnBoardViewModel::class.java)
        initView()
        VersionResponse.getInstance().image?.run {
            val mLayoutManager =
                LoopingLayoutManager(requireContext(), LoopingLayoutManager.HORIZONTAL, false)
            binding.recyclerView.builder
                .setHasFixedSize(true)
                .setLayoutManager(mLayoutManager)
            binding.recyclerView.addView(CarouselImageViewHolder(this))
            startImageScrolling()
            binding.recyclerView.setOnTouchListener { v, event ->
                if (event.action == ACTION_DOWN) {
                    handler?.removeCallbacksAndMessages(null)
                } else if (event.action == ACTION_UP) {
                    startImageScrolling()
                }
                return@setOnTouchListener false
            }

        }
    }

    private fun initView() {
        if (VersionResponse.getInstance().hasVersion()) {
            // Set buttonText
            if (VersionResponse.getInstance().version!!.name == ONBOARD_VERSIONS.ONBOARDING_V2)
                binding.startBtn.text = getString(R.string.start_your_7_day_trial)
            else
                binding.startBtn.text = getString(R.string.get_started)

            //Set Cover Image
            //Set up text viewpager
            VersionResponse.getInstance().content?.let {
                binding.viewPagerText.adapter = OnBoardingIntroTextAdapter(
                    requireActivity().supportFragmentManager,
                    it
                )
                binding.wormDotsIndicator.setViewPager(binding.viewPagerText)
            }
        }

        binding.startBtn.setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.NEW_ONBOARDING_GET_STARTED.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam("version", VersionResponse.getInstance().version?.name.toString())
                .push()
            viewModel.logGetStartedEvent()
            moveToNextScreen()
        }

        if ((requireActivity() as BaseActivity).isGuestUser()) {
            binding.alreadySubscribed.setOnClickListener {
                AppAnalytics.create(AnalyticsEvent.NEW_ONBOARDING_ALREADY_USER.NAME)
                    .addBasicParam()
                    .addUserDetails()
                    .addParam("version", VersionResponse.getInstance().version?.name.toString())
                    .push()

                val intent = Intent(requireActivity(), SignUpActivity::class.java).apply {
                    putExtra(FLOW_FROM, "NewOnBoardFlow journey")
                }
                startActivity(intent)
                requireActivity().finish()
            }
        } else binding.alreadySubscribed.visibility = View.GONE
    }

    private fun moveToNextScreen() {
        if (VersionResponse.getInstance().hasVersion()) {
            when (VersionResponse.getInstance().version!!.name) {
                ONBOARD_VERSIONS.ONBOARDING_V1, ONBOARD_VERSIONS.ONBOARDING_V7, ONBOARD_VERSIONS.ONBOARDING_V8 -> {
                    return
                }
                ONBOARD_VERSIONS.ONBOARDING_V2 -> {
                    (requireActivity() as BaseActivity).replaceFragment(
                        R.id.onboarding_container,
                        SelectCourseFragment.newInstance(),
                        SelectCourseFragment.TAG
                    )
                }
                ONBOARD_VERSIONS.ONBOARDING_V3, ONBOARD_VERSIONS.ONBOARDING_V4 -> {
                    (requireActivity() as BaseActivity).replaceFragment(
                        R.id.onboarding_container,
                        SelectInterestFragment.newInstance(),
                        SelectInterestFragment.TAG
                    )
                }
                ONBOARD_VERSIONS.ONBOARDING_V5 -> {
                    (requireActivity() as BaseActivity).replaceFragment(
                        R.id.onboarding_container,
                        SelectCourseHeadingFragment.newInstance(),
                        SelectCourseHeadingFragment.TAG
                    )
                }
                ONBOARD_VERSIONS.ONBOARDING_V6 -> {
                    openChatbot()
                }
            }
        }
    }

    private fun startImageScrolling() {
        handler = Handler()
        handler?.postDelayed(object : Runnable {
            override fun run() {
                binding.recyclerView.smoothScrollBy(20, 0)
                handler?.postDelayed(this, delay)
            }
        }, delay)
    }

    companion object {
        const val TAG = "OnBoardingIntroFragment"

        @JvmStatic
        fun newInstance() =
            OnBoardIntroFragment()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler?.removeCallbacksAndMessages(null)
    }

    fun openChatbot() {
        // provide your Dialogflow's Google Credential JSON saved under RAW folder in resources
        DialogflowCredentials.getInstance()
            .setInputStream(resources.openRawResource(R.raw.test_agent_credentials))

        ChatbotSettings.getInstance().chatbot = Chatbot.ChatbotBuilder()
            .setDoAutoWelcome(true)
            //  .setChatBotAvatar(getDrawable(R.drawable.avatarBot)) // provide avatar for your bot if default is not required
            //  .setChatUserAvatar(getDrawable(R.drawable.avatarUser)) // provide avatar for your the user if default is not required
            //  .setShowMic(true) // False by Default, True if you want to use Voice input from the user to chat
            .build()
        val intent = Intent(requireActivity(), ChatbotActivity::class.java)
        val bundle = Bundle()

        // provide a UUID for your session with the Dialogflow agent
        bundle.putString(ChatbotActivity.SESSION_ID, UUID.randomUUID().toString())
        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        intent.putExtras(bundle)
        startActivityForResult(intent, 1342, bundle)
    }

}
