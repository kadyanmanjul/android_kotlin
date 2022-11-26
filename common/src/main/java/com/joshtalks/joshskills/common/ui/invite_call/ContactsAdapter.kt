package com.joshtalks.joshskills.common.ui.invite_call

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.common.databinding.LiContactBinding
import com.joshtalks.joshskills.common.repository.local.entity.PhonebookContact

class ContactsAdapter :
    ListAdapter<PhonebookContact, ContactsAdapter.ContactViewHolder>(ContactDiffCallback) {

    private lateinit var listener: OnContactClickListener

    fun setContactClickListener(listener: OnContactClickListener) {
        this.listener = listener
    }

    inner class ContactViewHolder(
        private val binding: LiContactBinding,
        private val callback: OnContactClickListener
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: PhonebookContact) {
            binding.contact = contact
            binding.executePendingBindings()
        }
    }

    interface OnContactClickListener {
        fun onContactClick(contact: PhonebookContact)
    }

    private object ContactDiffCallback : DiffUtil.ItemCallback<PhonebookContact>() {
        override fun areItemsTheSame(
            oldItem: PhonebookContact,
            newItem: PhonebookContact
        ): Boolean {
            return oldItem.phoneNumber == newItem.phoneNumber
        }

        override fun areContentsTheSame(
            oldItem: PhonebookContact,
            newItem: PhonebookContact
        ): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        return ContactViewHolder(
            (LiContactBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )),
            callback = listener
        )
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
