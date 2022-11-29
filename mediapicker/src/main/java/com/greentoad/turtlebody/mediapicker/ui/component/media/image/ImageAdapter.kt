package com.greentoad.turtlebody.mediapicker.ui.component.media.image

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.greentoad.turtlebody.mediapicker.R
import com.greentoad.turtlebody.mediapicker.widget.ImageViewCheckable
import java.io.File

/**
 * Created by WANGSUN on 26-Mar-19.
 */
class ImageAdapter: RecyclerView.Adapter<ImageAdapter.ImageVewHolder>() {
    private var mData: MutableList<ImageModel> = arrayListOf()
    private var mOnImageClickListener: OnImageClickListener? = null
    var mShowCheckBox: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageVewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.tb_media_picker_item_image, parent, false)
        return ImageVewHolder(view)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun onBindViewHolder(holder: ImageVewHolder, position: Int) {
        holder.bind(mData[position])
    }


    fun setListener(listener : OnImageClickListener){
        mOnImageClickListener = listener
    }

    fun setData(pData: MutableList<ImageModel>){
        mData = pData
        notifyDataSetChanged()
    }

    fun updateIsSelected(pData: ImageModel){
        val pos = mData.indexOf(pData)
        if(pos>=0){
            mData[pos] = pData
            notifyItemChanged(pos)
        }
    }

    inner class ImageVewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        fun bind(pData: ImageModel){
            val checkable = itemView.findViewById<ImageViewCheckable>(R.id.item_image_checkbox)
            Glide.with(itemView)
                    .load(File(pData.thumbnailPath))
                    .into(itemView.findViewById(R.id.item_image_cover_image))

            checkable.isChecked = pData.isSelected

            itemView.setOnClickListener {
                mOnImageClickListener?.onImageCheck(pData)
            }

            if(!mShowCheckBox){
                checkable.visibility = View.GONE
            }
            else{
                checkable.visibility = View.VISIBLE
            }
        }
    }


    interface OnImageClickListener {
        fun onImageCheck(pData: ImageModel)
    }
}