package com.joshtalks.joshskills.ui.voip.favorite

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.interfaces.RecyclerViewItemClickListener
import com.joshtalks.joshskills.databinding.FavoriteListActivityBinding
import com.joshtalks.joshskills.repository.local.entity.practise.FavoriteCaller
import com.joshtalks.joshskills.ui.voip.favorite.adapter.FavoriteAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect


class FavoriteListActivity : AppCompatActivity(), RecyclerViewItemClickListener {
    private lateinit var binding: FavoriteListActivityBinding
    private var actionMode: ActionMode? = null
    private val deleteRecords: MutableSet<FavoriteCaller> = mutableSetOf()

    private val viewModel: FavoriteCallerViewModel by lazy {
        ViewModelProvider(this).get(FavoriteCallerViewModel::class.java)
    }
    private val favoriteAdapter: FavoriteAdapter by lazy { FavoriteAdapter(this) }


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
    }

    override fun onItemClick(view: View?, position: Int) {
        if (deleteRecords.isEmpty()) {
            return
        }
        updateListRow(position)
    }

    override fun onItemLongClick(view: View?, position: Int) {
        updateListRow(position)
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
        favoriteAdapter.removeAndUpdated()
        viewModel.deleteUsersFromFavoriteList(deleteRecords.toMutableList())
    }

    companion object {
        fun openFavoriteCallerActivity(activity: Activity) {
            Intent(activity, FavoriteListActivity::class.java).also {
                activity.startActivity(it)
            }
        }
    }
}