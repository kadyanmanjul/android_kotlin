package com.joshtalks.joshskills.common.ui.voip.favorite

import android.app.Activity
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.base.BaseActivity
import com.joshtalks.joshskills.common.constants.*
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.voip.base.constants.*
import com.joshtalks.joshskills.common.databinding.FavoriteListActivityBinding
import com.joshtalks.joshskills.common.track.CONVERSATION_ID
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.joshtalks.joshskills.voip.constant.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val FAVOURITE_LIST = "FAVOURITE_LIST"

class FavoriteListActivity : BaseActivity() {

    private var conversationId1: String = EMPTY
    private var actionMode: ActionMode? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var navigator: Navigator

    private val binding by lazy<FavoriteListActivityBinding> {
        DataBindingUtil.setContentView(this, R.layout.favorite_list_activity)
    }

    private val viewModel: FavoriteCallerViewModel by lazy {
        ViewModelProvider(this)[FavoriteCallerViewModel::class.java]
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
                scope.launch {
                    viewModel.deleteFavoriteUserFromList()
                    withContext(Dispatchers.Main) {
                        mode?.finish()
                    }
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            scope.launch {
                viewModel.deleteRecords.clear()
                viewModel.adapter.clearSelections()
                actionMode = null
            }
        }
    }

    fun setIntentExtras() {
        conversationId1 = intent.getStringExtra(CONVERSATION_ID).toString()
        navigator = AppObjectController.navigator
//        navigator = intent.getSerializableExtra(NAVIGATOR) as Navigator
    }

    override fun initViewBinding() {
        binding.vm = viewModel
        binding.executePendingBindings()
    }

    override fun onCreated() {
        setIntentExtras()
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
                OPEN_RECENT_SCREEN -> openRecentScreen()
                ENABLE_ACTION_MODE -> enableMode()
//                TODO : To refactor to FPP
                START_FPP_CALL -> {
                    val callIntent = Intent(applicationContext, VoiceCallActivity::class.java)
                    callIntent.apply {
                        putExtra(STARTING_POINT, FROM_ACTIVITY)
                        putExtra(INTENT_DATA_CALL_CATEGORY, Category.FPP.ordinal)
                        putExtra(INTENT_DATA_FPP_MENTOR_ID, viewModel.selectedUser?.mentorId)
                        putExtra(INTENT_DATA_FPP_NAME, viewModel.selectedUser?.name)
                        putExtra(INTENT_DATA_FPP_IMAGE, viewModel.selectedUser?.image)
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
        navigator.with(this).navigate(object : RecentCallContract {
            override val navigator = this@FavoriteListActivity.navigator
        })
    }

    private fun popBackStack() {
        try {
            if (supportFragmentManager.backStackEntryCount>0) {
                supportFragmentManager.popBackStack()
            } else {
                onBackPressed()
            }
        }catch (ex:Exception){
            ex.printStackTrace()
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

    //TODO Make navigation to Open UserProfileActivity
    private fun openProfileScreen(mId: String,position:Int) {
        if (viewModel.deleteRecords.isEmpty()){
//            UserProfileActivity.startUserProfileActivity(
//                this,
//                mentorId = mId,
//                conversationId = conversationId1,
//                previousPage = FAVOURITE_LIST
//            )
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
}
