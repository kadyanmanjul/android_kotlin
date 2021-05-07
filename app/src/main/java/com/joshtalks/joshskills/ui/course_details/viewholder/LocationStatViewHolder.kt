package com.joshtalks.joshskills.ui.course_details.viewholder

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.EmptyEventBus
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.joshtalks.joshskills.repository.server.course_detail.LocationStats
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.patloew.colocation.CoGeocoder
import java.util.*
import java.util.regex.Pattern
import kotlinx.coroutines.*

@Layout(R.layout.layout_location_stats_view_holder)
open class LocationStatViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private var locationStats: LocationStats,
    val activity: FragmentActivity,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(type, sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.background_image_view)
    lateinit var imageView: ImageView

    @com.mindorks.placeholderview.annotations.View(R.id.students_enrolled_nearby)
    lateinit var studentsNearby: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.state_country)
    lateinit var stateCityName: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.check_location_btn)
    lateinit var checkLocation: MaterialTextView

    @com.mindorks.placeholderview.annotations.View(R.id.progress_bar)
    lateinit var progressBar: FrameLayout
    var location: Location? = null
    private var index = 0
    val p: Pattern = Pattern.compile("\\d+")
    var city: String? = null
    var state: String? = null
    var randomStudents = 0

    @Resolve
    fun onResolved() {
        studentsNearby.text = locationStats.studentText
        if (randomStudents > 0) {
            studentsNearby.text = randomStudents.toString().plus(" students from")
        }
        if (city.isNullOrBlank()) {
            stateCityName.text = locationStats.locationText
        } else {
            stateCityName.text = city.plus(" , ").plus(state)
        }
        locationStats.imageUrls[index].run {
            if (this.isNotEmpty()) {
                setDefaultImageView(imageView, this)
            }
        }
        checkLocation.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            onClick()
        }
        location?.let {
            getAddressAndSetView(it.latitude, it.longitude)
        }
    }

    fun onClick() {
        if (locationPermissionGranted().not())
            getLocationPermissionAndLocation()
        else getLocationAndUpload()
        logAnalyticsEvent()
    }

    fun logAnalyticsEvent() {
        AppAnalytics.create(AnalyticsEvent.CHECK_LOCATION_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(VERSION, PrefManager.getStringValue(VERSION)).push()
    }

    private fun locationPermissionGranted() = (PermissionUtils.isLocationPermissionEnabled(context))

    private fun getLocationPermissionAndLocation() {
        PermissionUtils.locationPermission(
            activity,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            getLocationAndUpload()
                            return
                        } else {
                            progressBar.visibility = View.GONE
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    protected fun getLocationAndUpload() {
        RxBus2.publish(EmptyEventBus())
    }

    private fun getAddressAndSetView(latitude: Double, longitude: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val coGeocoder = CoGeocoder.from(context)
                coGeocoder.getAddressFromLocation(latitude, longitude)?.let {
                    if (city.isNullOrBlank().not() && city.equals(it.locality))
                        return@launch
                    AppObjectController.uiHandler.post {
                        stateCityName.text = it.locality.plus(" , ").plus(it.subAdminArea)
                        showNextImageAndRandomData()
                    }
                }
            } catch (ex: Throwable) {
            }
        }
    }

    private fun showNextImageAndRandomData() {
        index++
        if (index >= locationStats.imageUrls.size)
            index = 0
        randomStudents = randomNumberGenerator(
            locationStats.totalEnrolled.times(0.70).toInt(),
            locationStats.totalEnrolled.times(0.90).toInt()
        )
        studentsNearby.text = randomStudents.toString().plus(" students from")
        setDefaultImageView(imageView, locationStats.imageUrls.get(index))
        checkLocation.visibility = View.GONE
    }

    private fun randomNumberGenerator(start: Int, end: Int): Int {
        require(start <= end) {
            return 0
        }
        return (start..end).random()
    }
}
