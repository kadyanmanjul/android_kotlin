package com.joshtalks.joshskills.ui.group.utils

import android.text.Editable
import android.text.TextWatcher
import android.util.Log

import androidx.appcompat.widget.AppCompatEditText
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.ui.group.adapters.GroupAdapter
import com.joshtalks.joshskills.ui.group.adapters.GroupChatAdapter
import com.joshtalks.joshskills.ui.group.adapters.GroupMemberAdapter
import com.joshtalks.joshskills.ui.group.adapters.GroupStateAdapter
import com.joshtalks.joshskills.ui.group.model.DefaultImage
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.views.GroupsAppBar

import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.flow.MutableStateFlow

private const val TAG = "GroupBindingAdapter"
@BindingAdapter("onBackPressed")
fun GroupsAppBar.onBackPress(function: () -> Unit) = this.onBackPressed(function)

@BindingAdapter("onFirstIconPressed")
fun GroupsAppBar.onFirstIconPress(function: () -> Unit) = this.onFirstIconPressed(function)

@BindingAdapter("onSecondIconPressed")
fun GroupsAppBar.onSecondIconPress(function: () -> Unit) = this.onSecondIconPressed(function)

@BindingAdapter("onToolbarPressed")
fun GroupsAppBar.onToolbarPressed(function: () -> Unit) = this.onToolbarPressed(function)

@BindingAdapter("firstIcon")
fun GroupsAppBar.setFirstIcon(drawableRes: Int) {
    if (drawableRes != R.drawable.josh_skill_logo)
        this.firstIcon(drawableRes)
}

@BindingAdapter("groupHeader", "groupSubHeader", "subHeaderTimer", requireAll = false)
fun GroupsAppBar.setGroupHeaders(header: String, subHeader: String, boolean: Boolean = false) =
    this.setGroupSubTitle(subHeader, header, boolean)

@BindingAdapter("secondIcon")
fun GroupsAppBar.setSecondIcon(drawableRes: Int) {
    if (drawableRes != R.drawable.josh_skill_logo)
        this.secondIcon(drawableRes)
}

@BindingAdapter("groupImage")
fun GroupsAppBar.setGroupImage(imageUrl: String) = this.setImage(imageUrl)

@BindingAdapter("groupImage", "defaultImage")
fun CircleImageView.setGroupImage(imageUrl: String, defaultImage: DefaultImage) {
    if (imageUrl.isNotBlank())
        Glide.with(this)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(this)
    else
        Glide.with(this)
            .load(defaultImage.drwRes)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(this)
}

@BindingAdapter("groupAdapter", "stateAdapter", "onGroupItemClick")
fun setGroupAdapter(
    view: RecyclerView,
    adapter: GroupAdapter,
    stateAdapter: GroupStateAdapter,
    function: (GroupItemData) -> Unit
) {
    view.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
    view.setHasFixedSize(false)
    view.adapter = adapter.withLoadStateHeaderAndFooter(
        header = stateAdapter,
        footer = stateAdapter
    )
    adapter.setListener(function)
}

@BindingAdapter("groupMemberAdapter")
fun setGroupMemberAdapter(
    view: RecyclerView,
    adapter: GroupMemberAdapter,
) {
    view.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
    view.setHasFixedSize(false)
    view.adapter = adapter
}

@BindingAdapter("groupChatAdapter")
fun setGroupChatAdapter(
    view: RecyclerView,
    adapter: GroupChatAdapter,
) {
    view.layoutManager = LinearLayoutManager(
        view.context,
        RecyclerView.VERTICAL,
        false)
        .apply {
        isSmoothScrollbarEnabled = true
    }
    view.setHasFixedSize(false)
    view.adapter = adapter
}

@BindingAdapter("onSearch")
fun setOnSearch(view: AppCompatEditText, query: MutableStateFlow<String>) {
    view.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            query.value = s.toString()
        }

    })
}