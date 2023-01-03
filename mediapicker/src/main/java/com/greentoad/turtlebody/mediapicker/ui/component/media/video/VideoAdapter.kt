package com.greentoad.turtlebody.mediapicker.ui.component.media.video

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.greentoad.turtlebody.mediapicker.R
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.greentoad.turtlebody.mediapicker.widget.ImageViewCheckable
import java.io.File

/**
 * Created by WANGSUN on 26-Mar-19.
 */
class VideoAdapter : RecyclerView.Adapter<VideoAdapter.VideoVewHolder>() {
    private var mData: MutableList<VideoModel> = arrayListOf()
    private var mOnVideoClickListener: OnVideoClickListener? = null
    var mShowCheckBox: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoVewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.tb_media_picker_item_video, parent, false)
        return VideoVewHolder(view)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun onBindViewHolder(holder: VideoVewHolder, position: Int) {
        holder.bind(mData[position])
    }


    fun setListener(listener: OnVideoClickListener) {
        mOnVideoClickListener = listener
    }

    fun setData(pData: MutableList<VideoModel>) {
        mData = pData
        notifyDataSetChanged()
    }

    fun updateIsSelected(pData: VideoModel) {
        val pos = mData.indexOf(pData)
        if (pos >= 0) {
            mData[pos] = pData
            notifyItemChanged(pos)
        }
    }

    inner class VideoVewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(pData: VideoModel) {
            val videoCheckbox = itemView.findViewById<ImageViewCheckable>(R.id.tb_media_picker_item_video_checkbox)

            Glide.with(itemView)
                .load(File(pData.thumbnailPath))
                .into(itemView.findViewById(R.id.tb_media_picker_item_video_image))

            videoCheckbox.isChecked = pData.isSelected

            itemView.setOnClickListener {
                mOnVideoClickListener?.onVideoCheck(pData)
            }

            itemView.findViewById<TextView>(R.id.tb_media_picker_item_video_duration).text =
                UtilTime.timeFormatted(pData.duration.toLong())

            if (!mShowCheckBox) {
                videoCheckbox.visibility = View.GONE
            } else {
                videoCheckbox.visibility = View.VISIBLE
            }
        }
    }

    interface OnVideoClickListener {
        fun onVideoCheck(pData: VideoModel)
    }
}