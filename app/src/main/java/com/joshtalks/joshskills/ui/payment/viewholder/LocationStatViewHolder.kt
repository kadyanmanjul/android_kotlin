package com.joshtalks.joshskills.ui.payment.viewholder

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Geocoder
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.google.android.gms.location.LocationRequest
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.repository.server.SearchLocality
import com.joshtalks.joshskills.repository.server.UpdateUserLocality
import com.joshtalks.joshskills.repository.server.course_detail.LocationStats
import com.joshtalks.joshskills.ui.view_holders.CourseDetailsBaseCell
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.patloew.rxlocation.RxLocation
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.regex.Pattern

@Layout(R.layout.layout_location_stats_view_holder)
class LocationStatViewHolder(
    override val sequenceNumber: Int,
    private var locationStats: LocationStats,
    val activity: Activity,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(sequenceNumber) {

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

    private var compositeDisposable = CompositeDisposable()

    private var index = 0
    val p: Pattern = Pattern.compile("\\d+")
    var city: String? = null
    var state: String? = null
    var randomStudents = 0


    @Resolve
    fun onResolved() {
        val matcher = p.matcher(locationStats.studentText)

        studentsNearby.text = locationStats.studentText
        if (randomStudents > 0) studentsNearby.text =
            randomStudents.toString().plus(" students from")
        if (city.isNullOrBlank())
            stateCityName.text = locationStats.locationText
        else stateCityName.text = city.plus(" , ").plus(state)
        setDefaultImageView(imageView, locationStats.imageUrls.get(index))

        checkLocation.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            onClick()
        }
    }

    fun onClick() {
        if (locationPermissionGranted().not())
            getlocationPermissionAndLoction()
        else getLocationAndUpload()
    }

    private fun locationPermissionGranted() = (PermissionUtils.isLocationPermissionEnabled(context))

    private fun getlocationPermissionAndLoction() {
        PermissionUtils.locationPermission(activity,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            getLocationAndUpload()
                            return

                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
    }

    @SuppressLint("MissingPermission")
    private fun getLocationAndUpload() {
        val rxLocation = RxLocation(AppObjectController.joshApplication)
        val locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(10000)
        compositeDisposable.add(
            rxLocation.location().updates(locationRequest)
                .subscribeOn(Schedulers.computation())
                .subscribe({ location ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val request = UpdateUserLocality()
                            request.locality = SearchLocality(location.latitude, location.longitude)
                            getAddressAndSetView(location.latitude, location.longitude)
                            progressBar.visibility = View.GONE
                        } catch (e: Exception) {
                            progressBar.visibility = View.GONE
                            e.printStackTrace()
                        }
                        compositeDisposable.clear()
                    }
                }, { ex ->
                    ex.printStackTrace()
                })
        )

    }

    private fun getAddressAndSetView(latitude: Double, longitude: Double) {
        if (Geocoder.isPresent().not())
            return
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(
            latitude,
            longitude,
            1
        )
        if (city.isNullOrBlank().not() && city.equals(addresses.get(0).locality))
            return
        city = addresses.get(0).locality
        state = addresses.get(0).adminArea
        CoroutineScope(Dispatchers.Main).launch {
            stateCityName.text = city.plus(" , ").plus(state)
            showNextImageAndRandomData()
        }
    }

    private fun showNextImageAndRandomData() {
        index++
        if (index >= locationStats.imageUrls.size)
            index = 0
        randomStudents = rand(
            locationStats.totalEnrolled.times(0.25).toInt(),
            locationStats.totalEnrolled.times(0.75).toInt()
        )
        studentsNearby.text = randomStudents.toString().plus(" students from")
        setDefaultImageView(imageView, locationStats.imageUrls.get(index))
    }

    fun rand(start: Int, end: Int): Int {
        require(start <= end) { "Illegal Argument" }
        return (start..end).random()
    }
}