package com.joshtalks.joshskills.ui.course_details.extra


import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.TextDrawable
import com.joshtalks.joshskills.core.getUserNameInShort
import com.joshtalks.joshskills.repository.server.course_detail.Review

class ReviewsAdapter(private val reviews: List<Review>) :
    RecyclerView.Adapter<ReviewsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        val view = inflater.inflate(R.layout.user_review_item_layout, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]
        holder.name.text = review.username
        holder.location.text = review.userLocaton
        holder.rating.text = review.rating
        holder.reviewTitle.text = review.title
        holder.reviewMessage.text = review.description
        if (review.dpUrl.isNullOrEmpty()) {
            holder.picture.setImageDrawable(
                placeHolder(
                    holder.itemView.context,
                    review.username
                )
            )
        } else {
            Glide.with(holder.itemView.context)
                .load(review.dpUrl)
                .override(Target.SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .apply(RequestOptions.circleCropTransform())
                .into(holder.picture)
        }
    }

    private fun placeHolder(context: Context, name: String): TextDrawable {
        val font = Typeface.createFromAsset(context.assets, "fonts/OpenSans-Bold.ttf")
        return TextDrawable.builder()
            .beginConfig()
            .textColor(Color.WHITE)
            .useFont(font)
            .fontSize(Utils.dpToPx(10))
            .toUpperCase()
            .endConfig()
            .buildRound(
                getUserNameInShort(name),
                ContextCompat.getColor(context, R.color.button_primary_color)
            )

    }

    override fun getItemCount(): Int {
        return reviews.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rating = itemView.findViewById(R.id.rating) as AppCompatTextView
        val reviewTitle =
            itemView.findViewById(R.id.user_review_title) as AppCompatTextView
        val reviewMessage =
            itemView.findViewById(R.id.user_review_message) as AppCompatTextView
        val name = itemView.findViewById(R.id.user_name) as AppCompatTextView
        val location: AppCompatTextView =
            itemView.findViewById(R.id.user_location) as AppCompatTextView
        val picture = itemView.findViewById(R.id.user_picture) as AppCompatImageView
    }

}