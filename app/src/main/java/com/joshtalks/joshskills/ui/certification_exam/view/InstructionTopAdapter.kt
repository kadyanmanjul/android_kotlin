package com.joshtalks.joshskills.ui.certification_exam.view

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.core.setVectorImage
import com.joshtalks.joshskills.databinding.InstructionTopViewHolderBinding

class InstructionTopAdapter(
    val value: List<Int>,
    var label: List<String>,
    val image: List<String>,
    val bgColor: List<String>,
    val textColor: List<String>
    ): RecyclerView.Adapter<InstructionTopAdapter.ViewHolder>() {

        inner class ViewHolder(private val binding:InstructionTopViewHolderBinding): RecyclerView.ViewHolder(binding.root){
            fun setData(value: Int,
                        label: String,
                        image: String,
                        bgColor: String,
                        textColor: String){
                with(binding){
                    rootView.setCardBackgroundColor(Color.parseColor(bgColor))
                    tvLabel.setTextColor(Color.parseColor(textColor))
                    tvValue.text = value.toString()
                    tvLabel.text = label
                    imageView.setVectorImage("file:///android_asset/$image.svg")
                }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = InstructionTopViewHolderBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setData(value[position],label[position],image[position],bgColor[position],textColor[position])
    }

    override fun getItemCount() : Int = bgColor.size
}