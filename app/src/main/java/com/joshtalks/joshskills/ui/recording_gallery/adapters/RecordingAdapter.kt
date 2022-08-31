package com.joshtalks.joshskills.ui.recording_gallery.adapters

import android.util.Log
import android.view.LayoutInflater
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


class RecordingAdapter : ListAdapter<RecordingModel, RecordingAdapter.ViewHolder>(DiffCallback) {
    private var itemClickFunction: ((recording : RecordingModel) -> Unit)? = null
    private var currentDate : String?=null

     class ViewHolder(val binding: ItemRecordingGalleryBinding?, val bindingBreak: ItemGalleryBreakBinding?, val bindingBlank: ItemGalleryBlankBinding?=null) : RecyclerView.ViewHolder(
         ((binding?.root ?:bindingBreak?.root)?:bindingBlank?.root)!!) {

         fun bind(item : RecordingModel,shouldSetText : Boolean = false) {
             binding?.duration?.text = timeConversion(item.duration?.toLong())
            binding?.ssImg?.let {
                Glide.with(AppObjectController.joshApplication.applicationContext)
                    .load(item.videoUrl)
                    .into(it)
            }
             Log.d("naman", "bind: ${shouldSetText}")

             if (shouldSetText && bindingBreak?.breakText?.text.isNullOrBlank()) {
                 Log.d("naman", "bind: ${bindingBreak} ${item.timestamp?.toDateString()}")
                 bindingBreak?.breakText?.text = item.videoUrl.toString()
             }
         }

         fun timeConversion(millie: Long?): String? {
             return if (millie != null) {
                 val seconds = millie / 1000
                 val sec = seconds % 60
                 val min = seconds / 60 % 60
                 val hrs = seconds / (60 * 60) % 24
                 if (hrs > 0) {
                     String.format("%02d:%02d:%02d", hrs, min, sec)
                 } else {
                     String.format("%02d:%02d", min, sec)
                 }
             } else {
                 null
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
        Log.d("naman", "bind: ${getItem(position).videoUrl} ")

        if(getItem(position).imgUrl.equals("break")){
            Log.d("naman", "bind: ${holder.bindingBreak?.breakText?.text} ")
            holder.bind(getItem(position),true)
            holder.setIsRecyclable(false)
//            currentDate = getItem(position).timestamp?.toDateString()
        }
        else{
            holder.bind(getItem(position))
        }

        holder.itemView.setOnClickListener {
            itemClickFunction?.invoke(getItem(position))
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

    fun setItemClickFunction(function: (recording:RecordingModel) -> Unit) {
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