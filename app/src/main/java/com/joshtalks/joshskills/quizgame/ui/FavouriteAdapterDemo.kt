package com.joshtalks.joshskills.quizgame.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.CustomFavouriteBinding
import com.joshtalks.joshskills.databinding.FavoriteItemLayoutBinding
import com.joshtalks.joshskills.databinding.InboxItemLayoutBinding
import com.joshtalks.joshskills.quizgame.ui.data.FavouriteDemoData
import com.joshtalks.joshskills.quizgame.ui.main.adapter.FavouriteDiffCallback
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

class FavouriteAdapterDemo(var context: Context, var arrayList: ArrayList<FavouriteDemoData>):RecyclerView.Adapter<FavouriteAdapterDemo.FavViewHolder>(){

//    fun addItems(newList: List<FavouriteDemoData>) {
//        if (newList.isEmpty()) {
//            return
//        }
//        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(FavouriteDiffCallback(newList, arrayList))
//        diffResult.dispatchUpdatesTo(this)
//        arrayList.clear()
//        arrayList.addAll(newList)
//    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int
    ): FavViewHolder {
        val binding =
            CustomFavouriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavViewHolder, position: Int) {
        holder.bind(arrayList[position], position)
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    class FavViewHolder(val binding: CustomFavouriteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(favouriteDemoData: FavouriteDemoData,position: Int){
            binding.status.text = favouriteDemoData.status
            imageUrl(binding.userImage, favouriteDemoData.image)
            binding.userName.text=favouriteDemoData.name
        }
        fun imageUrl(imageView: ImageView, url: String?) {
            if (url.isNullOrEmpty()) {
                imageView.setImageResource(R.drawable.ic_josh_course)
                return
            }

            val multi = MultiTransformation(
                CropTransformation(
                    Utils.dpToPx(48),
                    Utils.dpToPx(48),
                    CropTransformation.CropType.CENTER
                ),
                RoundedCornersTransformation(
                    Utils.dpToPx(ROUND_CORNER),
                    0,
                    RoundedCornersTransformation.CornerType.ALL
                )
            )
            Glide.with(AppObjectController.joshApplication)
                .load(url)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .apply(
                    RequestOptions.bitmapTransform(multi).apply(
                        RequestOptions().placeholder(R.drawable.ic_josh_course)
                            .error(R.drawable.ic_josh_course)
                    )

                )
                .into(imageView)
        }
    }


}