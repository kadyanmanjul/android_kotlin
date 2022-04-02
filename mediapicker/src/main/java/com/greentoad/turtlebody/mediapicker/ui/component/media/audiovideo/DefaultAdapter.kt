package com.greentoad.turtlebody.mediapicker.ui.component.media.audiovideo

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.greentoad.turtlebody.mediapicker.R
import kotlinx.android.synthetic.main.tb_media_picker_default.view.*
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

            Glide.with(itemView)
                .load(File(pData.thumbnailPath))
                .into(itemView.tb_media_picker_item_image)

            itemView.tb_media_picker_item_checkbox.isChecked = pData.isSelected

            itemView.setOnClickListener {
                mOnMediaClickListener?.onSelectMedia(pData)
            }

            if (!mShowCheckBox) {
                itemView.tb_media_picker_item_checkbox.visibility = View.GONE
            } else {
                itemView.tb_media_picker_item_checkbox.visibility = View.VISIBLE
            }

            if (pData.fileType.toLowerCase(Locale.getDefault()) == "video/mp4") {
                itemView.mediatype_fl.visibility = View.VISIBLE
            } else {
                itemView.mediatype_fl.visibility = View.GONE

            }

        }
    }


    interface OnMediaSelectClickListener {
        fun onSelectMedia(pData: DefaultModel)
    }
}