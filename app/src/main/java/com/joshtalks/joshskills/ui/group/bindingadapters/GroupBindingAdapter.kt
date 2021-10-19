package com.joshtalks.joshskills.ui.group.bindingadapters

import android.util.Log
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.ui.group.GroupAdapter
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.views.GroupsAppBar

@BindingAdapter("onBackPressed")
fun GroupsAppBar.onBackPress(function : () -> Unit) = this.onBackPressed(function)

@BindingAdapter("onFirstIconPressed")
fun GroupsAppBar.onFirstIconPress(function : () -> Unit) = this.onFirstIconPressed(function)

@BindingAdapter("onSecondIconPressed")
fun GroupsAppBar.onSecondIconPress(function : () -> Unit) = this.onSecondIconPressed(function)

@BindingAdapter("groupAdapter", "onGroupItemClick")
fun setGroupAdapter(view : RecyclerView, adapter : GroupAdapter, function : (GroupItemData) -> Unit) {
    view.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
    view.setHasFixedSize(false)
    view.adapter = adapter
    adapter.setListener(function)
}
