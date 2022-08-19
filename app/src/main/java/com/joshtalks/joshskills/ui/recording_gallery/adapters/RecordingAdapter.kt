package com.joshtalks.joshskills.ui.recording_gallery.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.ItemGalleryBlankBinding
import com.joshtalks.joshskills.databinding.ItemGalleryBreakBinding
import com.joshtalks.joshskills.databinding.ItemRecordingGalleryBinding
import com.joshtalks.joshskills.ui.recording_gallery.RecordingModel
import com.joshtalks.joshskills.ui.recording_gallery.toDateString
import java.io.File


class RecordingAdapter : ListAdapter<RecordingModel, RecordingAdapter.ViewHolder>(DiffCallback) {
    private var itemClickFunction: (() -> Unit)? = null
    private var currentDate : String?=null

     class ViewHolder(val binding: ItemRecordingGalleryBinding?,val bindingBreak: ItemGalleryBreakBinding?,val bindingBlank: ItemGalleryBlankBinding?=null) : RecyclerView.ViewHolder(
         ((binding?.root ?:bindingBreak?.root)?:bindingBlank?.root)!!) {

         fun bind(item : RecordingModel,shouldSetText : Boolean = false) {
             binding?.duration?.text = item.duration.toString()
//            binding?.ssImg?.let {
//                Glide.with(AppObjectController.joshApplication.applicationContext)
//                    .load(Uri.fromFile(item.imgUrl?.let { File(it) }))
//                    .into(it)
//            }
             Log.d("naman", "bind: ${shouldSetText}")

             if (shouldSetText && bindingBreak?.breakText?.text.isNullOrBlank()) {
                 Log.d("naman", "bind: ${bindingBreak}")

                 bindingBreak?.breakText?.text = item.timestamp?.toDateString()
             }
         }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        Log.d("naman", "onCreateViewHolder: $currentList")
        when (viewType) {
            0 -> {
                Log.d("naman", "onCreateViewHolder: 0")
                val binding = ItemGalleryBlankBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(null,null,binding)

            }
            1 -> {
                Log.d("naman", "onCreateViewHolder: 1")

                val binding = ItemGalleryBreakBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(bindingBreak = binding, binding = null)
            }
            else -> {
                Log.d("naman", "onCreateViewHolder: 2")

                val binding = ItemRecordingGalleryBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(bindingBreak = null, binding = binding)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        if(currentDate!=getItem(position).timestamp?.toDateString() &&getItem(position).imgUrl.equals("break") ){
            holder.bind(getItem(position),true)
            currentDate = getItem(position).timestamp?.toDateString()
        }else{
            holder.bind(getItem(position))

        }

        holder.itemView.setOnClickListener {
            itemClickFunction?.invoke()
        }
    }

    override fun getItemViewType(position: Int): Int {
        Log.d("naman", "getItemViewType: ${getItem(position).imgUrl} ")
        return when (getItem(position).imgUrl) {
            "blank" -> {
                0
            }
            "break" -> {
                1
            }
            else -> {
                2
            }
        }
    }

    fun setItemClickFunction(function: () -> Unit) {
        itemClickFunction = function
    }
}

object DiffCallback : DiffUtil.ItemCallback<RecordingModel>() {
    override fun areItemsTheSame(oldItem: RecordingModel, newItem: RecordingModel): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: RecordingModel, newItem: RecordingModel): Boolean {
        return oldItem.imgUrl == newItem.imgUrl
    }
}