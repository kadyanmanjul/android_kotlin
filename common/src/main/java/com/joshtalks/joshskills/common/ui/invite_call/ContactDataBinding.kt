package com.joshtalks.joshskills.common.ui.invite_call

import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.setUserInitial
import com.joshtalks.joshskills.common.repository.local.entity.PhonebookContact

@BindingAdapter("contactAdapter", "contactClickListener", "scrollToTop")
fun setContactAdapter(
    recyclerView: RecyclerView,
    adapter: ContactsAdapter,
    callback: ContactsAdapter.OnContactClickListener,
    scrollToTop: Boolean
) {
    recyclerView.adapter = adapter
    recyclerView.setHasFixedSize(false)
    adapter.setContactClickListener(callback)
    if (scrollToTop)
        recyclerView.smoothScrollToPosition(0)
}

@BindingAdapter("contactCallback", "contactItem")
fun setOnContactItemClick(
    button: MaterialButton,
    callback: ContactsAdapter.OnContactClickListener,
    contact: PhonebookContact
) {
    button.setOnClickListener { callback.onContactClick(contact) }
}

@BindingAdapter("setUsername")
fun setUsernameInitials(view: AppCompatImageView, username: String) {
    view.setUserInitial(username, background = R.color.text_default)
}