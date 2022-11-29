package com.greentoad.turtlebody.mediapicker.ui.component.media.audiovideo

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.greentoad.turtlebody.mediapicker.R
import com.greentoad.turtlebody.mediapicker.widget.ImageViewCheckable
import java.io.File
import java.util.*

/**
 * Created by WANGSUN on 26-Mar-19.
 */
class DefaultAdapter : RecyclerView.Adapter<DefaultAdapter.ImageVewHolder>() {
    private var mData: MutableList<DefaultModel> = arrayListOf()
    private var mOnMediaClickListener: OnMediaSelectClickListener? = null
    var mShowCheckBox: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageVewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.tb_media_picker_default, parent, false)
        return ImageVewHolder(view)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun onBindViewHolder(holder: ImageVewHolder, position: Int) {
        holder.bind(mData[position])
    }


    fun setListener(listener: OnMediaSelectClickListener) {
        mOnMediaClickListener = listener
    }

    fun setData(pData: MutableList<DefaultModel>) {
        mData = pData
        notifyDataSetChanged()
    }

    fun updateIsSelected(pData: DefaultModel) {
        val pos = mData.indexOf(pData)
        if (pos >= 0) {
            mData[pos] = pData
            notifyItemChanged(pos)
        }
    }

    inner class ImageVewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(pData: DefaultModel) {
            val checkBoxView = itemView.findViewById<ImageViewCheckable>(R.id.tb_media_picker_item_checkbox)
            Glide.with(itemView)
                .load(File(pData.thumbnailPath))
                .into(itemView.findViewById(R.id.tb_media_picker_item_image))

            checkBoxView.isChecked = pData.isSelected

            itemView.setOnClickListener {
                mOnMediaClickListener?.onSelectMedia(pData)
            }

            if (!mShowCheckBox) {
                checkBoxView.visibility = View.GONE
            } else {
                checkBoxView.visibility = View.VISIBLE
            }

            if (pData.fileType.toLowerCase(Locale.getDefault()) == "video/mp4") {
                itemView.findViewById<FrameLayout>(R.id.mediatype_fl).visibility = View.VISIBLE
            } else {
                itemView.findViewById<FrameLayout>(R.id.mediatype_fl).visibility = View.GONE

            }

        }
    }


    interface OnMediaSelectClickListener {
        fun onSelectMedia(pData: DefaultModel)
    }
}