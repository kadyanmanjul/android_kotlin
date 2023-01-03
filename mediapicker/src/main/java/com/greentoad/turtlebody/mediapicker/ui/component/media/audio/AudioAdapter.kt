package com.greentoad.turtlebody.mediapicker.ui.component.media.audio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.greentoad.turtlebody.mediapicker.R

class AudioAdapter : RecyclerView.Adapter<AudioAdapter.AudioVewHolder>() {
    private var mData: MutableList<AudioModel> = arrayListOf()
    private var mOnAudioClickListener: OnAudioClickListener? = null
    var mShowCheckBox: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioVewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.tb_media_picker_item_audio, parent, false)
        return AudioVewHolder(view)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun onBindViewHolder(holder: AudioVewHolder, position: Int) {
        holder.bind(mData[position])
    }


    fun setListener(listener : OnAudioClickListener){
        mOnAudioClickListener = listener
    }

    fun setData(pData: MutableList<AudioModel>){
        mData = pData
        notifyDataSetChanged()
    }

    fun updateIsSelected(pData: AudioModel){
        val pos = mData.indexOf(pData)
        if(pos>=0){
            mData[pos] = pData
            notifyItemChanged(pos)
        }
    }

    inner class AudioVewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        fun bind(pData: AudioModel){
            val audioCheckBox = itemView.findViewById<CheckBox>(R.id.item_audio_checkbox)
            Glide.with(itemView)
                .load(getDrawableForMime(pData.mimeType, pData.filePath))
                .into(itemView.findViewById(R.id.item_audio_mimetype_icon))

            audioCheckBox.isChecked = pData.isSelected
            val size = (pData.size/1000).toString()

            itemView.findViewById<TextView>(R.id.item_audio_name).text = pData.name
            itemView.findViewById<TextView>(R.id.item_audio_size).text = "$size KB"

            itemView.setOnClickListener {
                mOnAudioClickListener?.onAudioCheck(pData)
            }

            audioCheckBox.setOnClickListener {
                mOnAudioClickListener?.onAudioCheck(pData)
            }

            if(!mShowCheckBox){
                audioCheckBox.visibility = View.GONE
            }
            else{
                audioCheckBox.visibility = View.VISIBLE
            }
        }

        private fun getDrawableForMime(mimeType: String?, filePath: String): Int {
            //info { "mimeType: "+ mimeType }
            var extType = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            //info { "extType: "+ extType }

            if(extType==null){
                val i = filePath.lastIndexOf('.')
                if (i > 0 && i< filePath.length-1) {
                    extType = filePath.substring(i + 1)
                }
            }
            return when(extType){
                "mp3"-> R.drawable.tb_media_picker_ic_audio_mp3
                "m4a"-> R.drawable.tb_media_picker_ic_audio_m4a
                "mp4"-> R.drawable.tb_media_picker_ic_audio_m4a
                "aac" -> R.drawable.tb_media_picker_ic_audio_aac
                "wav" -> R.drawable.tb_media_picker_ic_audio_wav
                else -> R.drawable.tb_media_picker_ic_audio_aud
            }
        }
    }

    interface OnAudioClickListener {
        fun onAudioCheck(pData: AudioModel)
    }
}