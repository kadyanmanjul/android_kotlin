package com.joshtalks.joshskills.ui.group.bindingadapters

import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.widget.AppCompatEditText
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.flurry.sdk.it
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.ui.group.adapters.GroupAdapter
import com.joshtalks.joshskills.ui.group.adapters.GroupStateAdapter
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.views.GroupsAppBar
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.flow.MutableStateFlow

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

@BindingAdapter("groupHeader", "groupSubHeader")
fun GroupsAppBar.setGroupHeaders(header: String, subHeader: String) =
    this.setGroupSubTitle(subHeader, header)

@BindingAdapter("secondIcon")
fun GroupsAppBar.setSecondIcon(drawableRes: Int) {
    if (drawableRes != R.drawable.josh_skill_logo)
        this.secondIcon(drawableRes)
}

@BindingAdapter("groupImage")
fun GroupsAppBar.setGroupImage(imageUrl: String) = this.setImage(imageUrl)

@BindingAdapter("groupImage")
fun CircleImageView.setGroupImage(imageUrl: String) {
    if (imageUrl.isNotBlank())
        Glide.with(this)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(this)
    else
        Glide.with(this)
            .load(R.drawable.group_default_icon)
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