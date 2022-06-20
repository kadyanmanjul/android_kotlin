package com.joshtalks.joshskills.ui.voip.favorite

import android.app.Activity
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.constants.FROM_ACTIVITY
import com.joshtalks.joshskills.base.constants.INTENT_DATA_CALL_CATEGORY
import com.joshtalks.joshskills.base.constants.INTENT_DATA_FPP_MENTOR_ID
import com.joshtalks.joshskills.base.constants.STARTING_POINT
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.IS_COURSE_BOUGHT
import com.joshtalks.joshskills.databinding.FavoriteListActivityBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.fpp.BaseFppActivity
import com.joshtalks.joshskills.ui.fpp.RecentCallActivity
import com.joshtalks.joshskills.ui.fpp.constants.*
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.joshtalks.joshskills.ui.voip.WebRtcActivity
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.joshtalks.joshskills.voip.constant.Category

class FavoriteListActivity : BaseFppActivity() {

    private var conversationId1: String = EMPTY

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
            viewModel.deleteRecords.clear()
            viewModel.adapter.clearSelections()
            actionMode = null
        }
    }

    override fun setIntentExtras() {
        conversationId1 = intent.getStringExtra(CONVERSATION_ID).toString()
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
                FAV_CLICK_ON_PROFILE ->{
                    if (it.obj != null)
                        openProfileScreen(it.obj.toString(), it.arg1)
                }
                OPEN_CALL_SCREEN ->{
                    if (it.obj != null) {
                        callScreenOpen(it.obj as Int)
                    }
                }
                OPEN_RECENT_SCREEN -> openRecentScreen()
                ENABLE_ACTION_MODE -> enableMode()
                1234->{
                    val callIntent = Intent(applicationContext, VoiceCallActivity::class.java)
                    callIntent.apply {
                        putExtra(STARTING_POINT, FROM_ACTIVITY)
                        putExtra(INTENT_DATA_CALL_CATEGORY, Category.FPP.ordinal)
                        putExtra(INTENT_DATA_FPP_MENTOR_ID, "c2555b43-3bb3-40d3-8621-b2abee4a67d9")
                    }
                startActivity(callIntent)
                }
                SET_TEXT_ON_ENABLE_ACTION_MODE -> {
                    if (it.obj != null) {
                        setTextOnActionMode(it.obj.toString())
                    }
                }
                FINISH_ACTION_MODE -> finishActionMode()
            }
        }
    }

    private fun openRecentScreen() {
        RecentCallActivity.openRecentCallActivity(this, conversationId1)
    }

    private fun popBackStack() {
        if (supportFragmentManager.backStackEntryCount>0) {
            supportFragmentManager.popBackStack()
        } else {
            onBackPressed()
        }
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

    private fun openProfileScreen(mId: String,position:Int) {
        if (viewModel.deleteRecords.isEmpty()){
            UserProfileActivity.startUserProfileActivity(
                this,
                mentorId = mId,
                conversationId = conversationId1,
                previousPage = FAVOURITE_LIST
            )
            return
        }
        viewModel.updateListRow(position)
    }

    companion object {
        fun openFavoriteCallerActivity(activity: Activity, conversationId: String) {
            Intent(activity, FavoriteListActivity::class.java).apply {
                putExtra(CONVERSATION_ID, conversationId)
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
