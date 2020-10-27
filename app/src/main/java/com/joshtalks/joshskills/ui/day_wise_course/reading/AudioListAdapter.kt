package com.joshtalks.joshskills.ui.day_wise_course.reading

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.PracticeAudioItemBinding
import com.joshtalks.joshskills.repository.local.entity.AudioType

class AudioListAdapter(val audioList: List<AudioType>) :
    RecyclerView.Adapter<AudioListAdapter.AudioViewHolder>() {

    inner class AudioViewHolder(binding: PracticeAudioItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(audioItem: AudioType) {

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val binding =
            PracticeAudioItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AudioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {

        holder.bind(audioList.get(position))
    }

    override fun getItemCount(): Int {
        return audioList.size
    }
}
