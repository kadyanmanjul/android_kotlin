package com.joshtalks.joshskills.ui.activity_feed

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.format.DateFormat
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedResponseFirebase
import de.hdodenhof.circleimageview.CircleImageView
import java.util.*

class ActivityFeedListAdapter(
    private val items: ArrayList<ActivityFeedResponseFirebase>
) : RecyclerView.Adapter<ActivityFeedListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_feed_row_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.bind(items[position])

    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var userIcon: CircleImageView = view.findViewById(R.id.user_icon)
        var feedText: TextView = view.findViewById(R.id.feed_text)
        var feedTime: AppCompatTextView = view.findViewById(R.id.feed_time)
        var activityFeedResponse: ActivityFeedResponseFirebase? = null

        fun bind(activityFeedResponse: ActivityFeedResponseFirebase) {
            this.activityFeedResponse = activityFeedResponse
            feedText.text = activityFeedResponse.name + " " + activityFeedResponse.text
            activityFeedResponse.name?.let { feedText.colorize(it) }
            val calendar: Calendar = Calendar.getInstance()
            activityFeedResponse.date?.let {
                calendar.setTimeInMillis(it.seconds)
            }
            val date: String = DateFormat.format("hh:mm", calendar).toString()
            feedTime.text = date
//                toString()?.substring(startIndex=11, endIndex=16)
            if (activityFeedResponse.photoUrl == null) {
                userIcon.setImageResource(R.drawable.ic_call_placeholder)
            } else {
                userIcon.setImage(activityFeedResponse.photoUrl!!, view.context)

            }
        }

    }
    fun TextView.colorize(subStringToColorize: String) {
        val spannable: Spannable = SpannableString(text)
        spannable.setSpan(
                ForegroundColorSpan(Color.parseColor(getColorHexCode())),
                0,
                subStringToColorize.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                subStringToColorize.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        setText(spannable, TextView.BufferType.SPANNABLE)
    }

    fun getColorHexCode(): String {
        val colorArray = arrayOf(
            "#f83a7e", "#2213fa", "#d5857a",
            "#706d45", "#63805a", "#b812bc",
            "#ee431b", "#f56fbe", "#721fde",
            "#953f30", "#ed9207", "#8d8eb4",
            "#78bcb2", "#3c6c9b", "#6ce172",
            "#4dc7b6", "#fe5b00", "#846fd2",
            "#755812", "#3b9c42", "#c2d542",
            "#a22b2f", "#cc794a", "#c20748",
            "#7a4ff8", "#163d52"
        )
        return colorArray[itemCount % colorArray.size]
    }

}