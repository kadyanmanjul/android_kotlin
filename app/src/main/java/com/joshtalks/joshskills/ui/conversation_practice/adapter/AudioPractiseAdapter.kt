package com.joshtalks.joshskills.ui.conversation_practice.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.AudioPractiseReceivedItemBinding
import com.joshtalks.joshskills.databinding.AudioPractiseSentItemBinding

import com.joshtalks.joshskills.repository.server.conversation_practice.ListenModel

class AudioPractiseAdapter(private var items: List<ListenModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            val binding = AudioPractiseReceivedItemBinding.inflate(inflater, parent, false)
            ViewHolderReceived(binding, viewType)
        } else {
            val binding = AudioPractiseSentItemBinding.inflate(inflater, parent, false)
            ViewHolderSent(binding, viewType)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolderSent) {
            (holder).also {
                it.bind(items[position])
            }
        } else if (holder is ViewHolderReceived) {
            (holder).also {
                it.bind(items[position])
            }
        }

    }

    inner class ViewHolderSent(
        val binding: AudioPractiseSentItemBinding,
        private val viewType: Int
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(listenModel: ListenModel) {
            with(binding) {

            }
        }
    }

    inner class ViewHolderReceived(
        val binding: AudioPractiseReceivedItemBinding,
        private val viewType: Int
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(listenModel: ListenModel) {
            with(binding) {

            }
        }
    }

}
