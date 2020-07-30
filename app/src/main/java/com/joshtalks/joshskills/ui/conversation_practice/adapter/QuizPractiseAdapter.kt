package com.joshtalks.joshskills.ui.conversation_practice.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.AudioPractiseSentItemBinding
import com.joshtalks.joshskills.repository.server.conversation_practice.QuizModel

class QuizPractiseAdapter(var items: MutableList<QuizModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AudioPractiseSentItemBinding.inflate(inflater, parent, false)
        return ViewHolderSent(binding)

    }


    override fun getItemId(position: Int): Long {
        return items[position].id.toLong()
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {


    }

    inner class ViewHolderSent(
        val binding: AudioPractiseSentItemBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(quizModel: QuizModel) {
            with(binding) {

            }
        }
    }

}
