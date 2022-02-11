package com.joshtalks.joshskills.ui.voip.favorite

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.WebRtcMiddlewareActivity
import com.joshtalks.joshskills.core.interfaces.OnClickUserProfile
import com.joshtalks.joshskills.core.interfaces.RecyclerViewItemClickListener
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FavoriteListActivityBinding
import com.joshtalks.joshskills.repository.local.entity.practise.FavoriteCaller
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.fpp.RecentCallActivity
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.joshtalks.joshskills.ui.voip.WebRtcActivity
import com.joshtalks.joshskills.ui.voip.favorite.adapter.FavoriteAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

class FavoriteListActivity : WebRtcMiddlewareActivity(), RecyclerViewItemClickListener,OnClickUserProfile {
    private lateinit var binding: FavoriteListActivityBinding
    private var actionMode: ActionMode? = null
    private val deleteRecords: MutableSet<FavoriteCaller> = mutableSetOf()

    private val viewModel: FavoriteCallerViewModel by lazy {
        ViewModelProvider(this).get(FavoriteCallerViewModel::class.java)
    }
    private val favoriteAdapter: FavoriteAdapter by lazy { FavoriteAdapter(this,this) }

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
                deleteFavoriteUserFromList()
                mode?.finish()
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            deleteRecords.clear()
            favoriteAdapter.clearSelections()
            actionMode = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.favorite_list_activity)
        binding.handler = this
        setSupportActionBar(binding.toolbar)
        initView()
        addObservable()
        viewModel.getFavorites()
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
    }

    private fun initView() {

        binding.favoriteListRv.apply {
            itemAnimator = null
            layoutManager = LinearLayoutManager(applicationContext).apply {
                isSmoothScrollbarEnabled = true
            }
            adapter = favoriteAdapter
        }
        binding.favoriteListRv.addOnItemTouchListener(
            RecyclerTouchListener(
                applicationContext,
                binding.favoriteListRv,
                this
            )
        )
    }

    private fun addObservable() {
        lifecycleScope.launchWhenStarted {
            viewModel.favoriteCallerList.collect {
                if (it.isEmpty()) {
                    return@collect
                }
                delay(350)
                favoriteAdapter.addItems(it)
                binding.progressBar.visibility = View.GONE
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.apiCallStatus.collect {
                binding.progressBar.visibility = View.GONE
                if (favoriteAdapter.itemCount == 0) {
                    showToast("You can add partners to this list by doing calls and pressing yes")
                }
            }
        }
    }

    override fun onItemClick(view: View?, position: Int) {

    }

    private fun openProfileScreen(fc: FavoriteCaller) {
        UserProfileActivity.startUserProfileActivity(
            this,
            mentorId = fc.mentorId,
            conversationId = intent.getStringExtra(CONVERSATION_ID),
        )
    }

    override fun onItemLongClick(view: View?, position: Int) {
       // updateListRow(position)
    }

    private fun updateListRow(position: Int) {
        enableActionMode(position)
    }

    private fun enableActionMode(position: Int) {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback)
        }
        toggleSelection(position)
    }

    private fun toggleSelection(position: Int) {
        val item = favoriteAdapter.getItemAtPosition(position)
        if (deleteRecords.contains(item)) {
            item.selected = false
            deleteRecords.remove(item)
        } else {
            item.selected = true
            deleteRecords.add(item)
        }
        favoriteAdapter.updateItem(item, position)
        if (deleteRecords.isEmpty()) {
            actionMode?.finish()
            actionMode = null
        } else {
            actionMode?.title = deleteRecords.size.toString()
            actionMode?.invalidate()
        }
    }

    private fun deleteFavoriteUserFromList() {
        showToast(getDeleteMessage())
        favoriteAdapter.removeAndUpdated()
        viewModel.deleteUsersFromFavoriteList(deleteRecords.toMutableList())
    }

    private fun getDeleteMessage(): String {
        if (deleteRecords.size > 1) {
            return "${deleteRecords.size} practice partners removed"
        }
        return "${deleteRecords.size} practice partner removed"
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

    override fun clickOnProfile(position: Int) {
        if (deleteRecords.isEmpty()) {
            openProfileScreen(favoriteAdapter.getItemAtPosition(position))
            return
        }
        updateListRow(position)
    }

    override fun clickOnPhoneCall(position: Int) {
       val intent =  WebRtcActivity.getFavMissedCallbackIntent(favoriteAdapter.getItemAtPosition(position).id, this).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun clickLongPressDelete(position: Int) {
        updateListRow(position)
    }
}
