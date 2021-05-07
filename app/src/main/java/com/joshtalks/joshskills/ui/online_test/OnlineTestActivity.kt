package com.joshtalks.joshskills.ui.online_test

import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.WebRtcMiddlewareActivity
import com.joshtalks.joshskills.databinding.ActivityOnlineTestActivityBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.points_history.viewmodel.PointsViewModel
import kotlinx.android.synthetic.main.base_toolbar.iv_back
import kotlinx.android.synthetic.main.base_toolbar.iv_help
import kotlinx.android.synthetic.main.base_toolbar.text_message_title

class OnlineTestActivity : WebRtcMiddlewareActivity() {
    private val viewModel: PointsViewModel by lazy {
        ViewModelProvider(this).get(PointsViewModel::class.java)
    }
    private lateinit var binding: ActivityOnlineTestActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_online_test_activity)
        binding.lifecycleOwner = this
        binding.handler = this
        initToolbar()
        startOnlineExamTest()
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
        text_message_title.text = "Online Test"
    }

    private fun startOnlineExamTest() {
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.parent_Container,
                OnlineTestFragment.getInstance(),
                OnlineTestFragment.TAG
            )
            .commitAllowingStateLoss()
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 1) {
            this.finish()
            return
        }
        super.onBackPressed()
    }

}
