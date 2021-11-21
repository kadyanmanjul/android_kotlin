package com.joshtalks.joshskills.quizgame.ui.main.adapter

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
import com.joshtalks.joshskills.quizgame.ui.data.model.Favourite
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

class FavouriteAdapter(
    var context: Context, var arrayList: ArrayList<Favourite>?,
    private val openCourseListener: QuizBaseInterface
):
    RecyclerView.Adapter<FavouriteAdapter.FavViewHolder>(){

    fun addItems(newList: ArrayList<Favourite>?) {
        if (newList!!.isEmpty()) {
            return
        }
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(FavouriteDiffCallback(newList, arrayList))
        diffResult.dispatchUpdatesTo(this)
        arrayList?.clear()
        arrayList?.addAll(newList)
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FavViewHolder {
        val binding =
            CustomFavouriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavViewHolder, position: Int) {
        arrayList?.get(position)?.let { holder.bind(it, position) }
    }


    override fun getItemCount(): Int {
        return arrayList?.size!!
    }

    inner class FavViewHolder(val binding: CustomFavouriteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(favouriteDemoData: Favourite?,position: Int){
            //binding.status.text = favouriteDemoData.status
            imageUrl(binding.userImage, favouriteDemoData?.image)
            binding.userName.text=favouriteDemoData?.name
            binding.status.text = favouriteDemoData?.status

            when (favouriteDemoData?.status) {
                "active" -> {
                    binding.clickToken.setImageResource(R.drawable.plus)
                    binding.clickToken.setOnClickListener(View.OnClickListener {
//                        binding.clickToken.setImageResource(R.drawable.ic_grass_timer)
//                        binding.clickToken.isEnabled = false
                        openCourseListener.onClickForGetToken(arrayList?.get(position))
                    })
                }
                "inactive" -> {
                    binding.clickToken.visibility=View.INVISIBLE
                }
                else -> {
                    binding.clickToken.visibility=View.INVISIBLE
                }
            }
        }

        fun imageUrl(imageView: ImageView, url: String?) {
            val imageUrl=url?.replace("\n","")

            if (imageUrl.isNullOrEmpty()) {
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
                .load(imageUrl)
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
    fun setGrassImage(){
       // binding?.clickToken?.isEnabled = true
     //   binding?.clickToken?.setImageResource(R.drawable.plus)
    }
    interface QuizBaseInterface {
        fun onClickForGetToken(favourite: Favourite?)
    }

}