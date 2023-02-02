package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.ListConverters
import com.joshtalks.joshskills.repository.local.SliderImageConverter
import com.joshtalks.joshskills.repository.local.TestimonialosVideoConverter
import java.util.*

@Entity(tableName = "buy_course_feature")
data class BuyCourseFeatureModelNew(
    @PrimaryKey
    @ColumnInfo(name = "id")
    var id: Int = 0,

    @TypeConverters(
        ListConverters::class
    )
    @ColumnInfo(name = "features")
    @SerializedName("features") var features: List<String> = arrayListOf(),

    @TypeConverters(
        ListConverters::class
    )
    @ColumnInfo(name = "information")
    @SerializedName("information") var information: List<String> = arrayListOf(),

    @ColumnInfo(name = "rating")
    @SerializedName("rating") var rating: Float,

    @ColumnInfo(name = "ratings_count")
    @SerializedName("ratings_count") var ratingCount: Int,

    @ColumnInfo(name = "expire_time")
    @SerializedName("expire_time") var expiryTime: Date,

    @ColumnInfo(name = "call_us_text")
    @SerializedName("call_us_text") var callUsText: String,

    @ColumnInfo(name = "course_name")
    @SerializedName("course_name") var courseName: String,

    @ColumnInfo(name = "image_name")
    @SerializedName("image_name") var otherCourseImage: String,

    @ColumnInfo(name = "bp_text")
    @SerializedName("bp_text") val priceEnglishText: String,

    @ColumnInfo(name = "is_call_us_active")
    @SerializedName("is_call_us_active") var isCallUsActive: Int,

    @ColumnInfo(name = "payment_button_text")
    @SerializedName("payment_button_text") var paymentButtonText: Int = 0,

    @TypeConverters(SliderImageConverter::class)
    @ColumnInfo(name = "slider_image")
    @SerializedName("images") var images: List<SliderImage> = arrayListOf(),

    @TypeConverters(TestimonialosVideoConverter::class)
    @ColumnInfo(name = "testimonials_video")
    @SerializedName("videos") var videos: List<TestimonialVideo> = arrayListOf(),

    @TypeConverters(
        ListConverters::class
    )
    @ColumnInfo(name = "live_messages")
    @SerializedName("live_messages") var liveMessages: List<String> = arrayListOf(),

)

data class SliderImage(
    @SerializedName("url")
    val imageUrl: String
)

data class TestimonialVideo(

    @SerializedName("id")
    var id: Int = 0,

    @SerializedName("url")
    var video_url: String? = "",

    @SerializedName("thumbnail_link")
    val thumbnailUrl: String?,
)