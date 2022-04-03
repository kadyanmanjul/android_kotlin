package com.joshtalks.joshskills.ui.voip.favorite

import android.app.Activity
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.IS_COURSE_BOUGHT
import com.joshtalks.joshskills.databinding.FavoriteListActivityBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.fpp.BaseFppActivity
import com.joshtalks.joshskills.ui.fpp.RecentCallActivity
import com.joshtalks.joshskills.ui.fpp.constants.FAV_CLICK_ON_PROFILE
import com.joshtalks.joshskills.ui.fpp.constants.FAV_LIST_SCREEN_BACK_PRESSED
import com.joshtalks.joshskills.ui.fpp.constants.OPEN_CALL_SCREEN
import com.joshtalks.joshskills.ui.fpp.constants.OPEN_RECENT_SCREEN
import com.joshtalks.joshskills.ui.fpp.constants.FINISH_ACTION_MODE
import com.joshtalks.joshskills.ui.fpp.constants.SET_TEXT_ON_ENABLE_ACTION_MODE
import com.joshtalks.joshskills.ui.fpp.constants.ENABLE_ACTION_MODE
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.joshtalks.joshskills.ui.voip.WebRtcActivity

class FavoriteListActivity : BaseFppActivity() {

    private var conversationId1: String = EMPTY
    private var isCourseBought:Boolean = false

    private var actionMode: ActionMode? = null

    private val binding by lazy<FavoriteListActivityBinding> {
        DataBindingUtil.setContentView(this, R.layout.favorite_list_activity)
    }

    private val viewModel: FavoriteCallerViewModel by lazy {
        ViewModelProvider(this).get(FavoriteCallerViewModel::class.java)
    }

    private var actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_favorite_mode, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
            if (item.itemId == R.id.action_delete) {
                viewModel.deleteFavoriteUserFromList()
                mode?.finish()
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
        }
    }

    override fun setIntentExtras() {
        conversationId1 = intent.getStringExtra(CONVERSATION_ID).toString()
        isCourseBought = intent.getBooleanExtra(IS_COURSE_BOUGHT,false)
    }

    override fun initViewBinding() {
        binding.vm = viewModel
        binding.executePendingBindings()
    }

    override fun onCreated() {
        viewModel.getFavorites()
    }

    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                FAV_LIST_SCREEN_BACK_PRESSED -> popBackStack()
                FAV_CLICK_ON_PROFILE -> openProfileScreen(it.obj.toString())
                OPEN_CALL_SCREEN -> callScreenOpen(it.obj as Int)
                OPEN_RECENT_SCREEN -> openRecentScreen()
                ENABLE_ACTION_MODE -> enableMode()
                SET_TEXT_ON_ENABLE_ACTION_MODE -> setTextOnActionMode(it.obj.toString())
                FINISH_ACTION_MODE -> finishActionMode()
            }
        }
    }

    private fun openRecentScreen() {
        RecentCallActivity.openRecentCallActivity(
            this,
            conversationId1,
            isCourseBought
        )
    }

    private fun popBackStack() {
        onBackPressed()
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.getFavorites()
    }

    private fun enableMode() {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback)
        }
    }

    private fun finishActionMode(){
        actionMode?.finish()
    }

    private fun setTextOnActionMode(size: String) {
        actionMode?.title = size
        actionMode?.invalidate()
    }

    private fun openProfileScreen(mId: String) {
        UserProfileActivity.startUserProfileActivity(
            this,
            mentorId = mId,
            conversationId = conversationId1
        )
        return
    }

    companion object {
        fun openFavoriteCallerActivity(activity: Activity, conversationId: String,isCourseBought:Boolean) {
            Intent(activity, FavoriteListActivity::class.java).apply {
                putExtra(CONVERSATION_ID, conversationId)
                putExtra(IS_COURSE_BOUGHT, isCourseBought)
            }.also {
                activity.startActivity(it)
            }
        }
    }

    private fun callScreenOpen(uid: Int) {
        val intent =
            WebRtcActivity.getFavMissedCallbackIntent(uid, this).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        startActivity(intent)
    }
}
