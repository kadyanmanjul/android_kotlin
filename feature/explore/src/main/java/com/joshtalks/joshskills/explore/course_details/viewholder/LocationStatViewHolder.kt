package com.joshtalks.joshskills.explore.course_details.viewholder

import android.annotation.SuppressLint
import android.app.Activity
import android.location.Location
import android.view.View
import com.google.gson.JsonObject
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.PermissionUtils
import com.joshtalks.joshskills.common.core.PrefManager
import com.joshtalks.joshskills.common.core.VERSION
import com.joshtalks.joshskills.common.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.common.core.analytics.AppAnalytics
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.EmptyEventBus
import com.joshtalks.joshskills.explore.course_details.models.LocationStats
import com.joshtalks.joshskills.explore.databinding.LayoutLocationStatsViewHolderBinding
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.patloew.colocation.CoGeocoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

open class LocationStatViewHolder(
    val item: LayoutLocationStatsViewHolderBinding,
    val activity: Activity
) : DetailsBaseViewHolder(item) {

    var location: Location? = null
    private var index = 0
    val p: Pattern = Pattern.compile("\\d+")
    var city: String? = null
    var state: String? = null
    var randomStudents = 0

    override fun bindData(sequence: Int, cardData: JsonObject) {
        val data = AppObjectController.gsonMapperForLocal.fromJson(
            cardData.toString(),
            LocationStats::class.java
        )
        item.studentsEnrolledNearby.text = data.studentText
        if (randomStudents > 0) {
            item.studentsEnrolledNearby.text = randomStudents.toString().plus(" students from")
        }
        if (city.isNullOrBlank()) {
            item.stateCountry.text = data.locationText
        } else {
            item.stateCountry.text = city.plus(" , ").plus(state)
        }
        data.imageUrls[index].run {
            if (this.isNotEmpty()) {
                setDefaultImageView(item.backgroundImageView, this)
            }
        }
        item.checkLocationBtn.setOnClickListener {
            item.progressBar.visibility = View.VISIBLE
            onClick()
        }
        location?.let {
            getAddressAndSetView(it.latitude, it.longitude, data)
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

    private fun locationPermissionGranted() = (PermissionUtils.isLocationPermissionEnabled(getAppContext()))

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
                            item.progressBar.visibility = View.GONE
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

    private fun getAddressAndSetView(latitude: Double, longitude: Double, data: LocationStats) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val coGeocoder = CoGeocoder.from(getAppContext())
                coGeocoder.getAddressFromLocation(latitude, longitude)?.let {
                    if (city.isNullOrBlank().not() && city.equals(it.locality))
                        return@launch
                    AppObjectController.uiHandler.post {
                        item.stateCountry.text = it.locality.plus(" , ").plus(it.subAdminArea)
                        showNextImageAndRandomData(data)
                    }
                }
            } catch (ex: Throwable) {
            }
        }
    }

    private fun showNextImageAndRandomData(data: LocationStats) {
        index++
        if (index >= data.imageUrls.size)
            index = 0
        randomStudents = randomNumberGenerator(
            data.totalEnrolled.times(0.70).toInt(),
            data.totalEnrolled.times(0.90).toInt()
        )
        item.studentsEnrolledNearby.text = randomStudents.toString().plus(" students from")
        setDefaultImageView(item.backgroundImageView, data.imageUrls.get(index))
        item.checkLocationBtn.visibility = View.GONE
    }

    private fun randomNumberGenerator(start: Int, end: Int): Int {
        require(start <= end) {
            return 0
        }
        return (start..end).random()
    }
}
