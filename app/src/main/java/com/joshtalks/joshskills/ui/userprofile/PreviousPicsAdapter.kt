package com.joshtalks.joshskills.ui.userprofile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.repository.server.Picture

class PreviousPicsAdapter(
    private val items: List<Picture> = emptyList(),
    private val onPreviousPicClickListener: OnPreviousPicClickListener
) : RecyclerView.Adapter<PreviousPicsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.previous_pic_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var imgPreviousPic: AppCompatImageView = view.findViewById(R.id.previous_p)
        var picture: Picture? = null

        fun bind(picture: Picture) {
            this.picture = picture
            imgPreviousPic.setImage(picture.photoUrl, view.context)
            view.setOnClickListener { onPreviousPicClickListener.onPreviousPicClick(picture) }
        }

    }

    interface OnPreviousPicClickListener {
        fun onPreviousPicClick(picture: Picture)
    }

}