package com.joshtalks.joshskills.ui.payment.viewholder

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.core.showToast
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
    lateinit var stateName: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.state_country)
    lateinit var nearbyEnrolledStudents: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.check_location_btn)
    lateinit var checkLocation: MaterialTextView

    @com.mindorks.placeholderview.annotations.View(R.id.progress_bar)
    lateinit var progressBar: FrameLayout

    private var compositeDisposable = CompositeDisposable()

    private var lastLocation: Location? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var index = 0
    private var totalStudent = 3000
    private var shownStudent = 0
    val p: Pattern = Pattern.compile("\\d+")


    @Resolve
    fun onResolved() {
        //setDefaultImageView(imageView,locationStats.)
        val matcher = p.matcher(locationStats.studentText)
        if (matcher.find()) {
            shownStudent = matcher.group().toInt()
        }
        stateName.text = locationStats.locationText
        nearbyEnrolledStudents.text = locationStats.locationText
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        setDefaultImageView(imageView, locationStats.imageUrls[index])
        checkLocation.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            onClick()
        }
    }

    fun onClick() {
        showNextImageAndRandomData()
        showToast("Check Location")
        if (locationPermissionGranted().not())
            getlocationPermissionAndLoction()
        else getLocation()
    }

    private fun showNextImageAndRandomData() {
        index++
        if (index >= locationStats.imageUrls.size)
            index = 0
        shownStudent = rand(totalStudent.div(25), totalStudent.div(75))
        stateName.text = shownStudent.toString().plus(" students from")
        setDefaultImageView(imageView, locationStats.imageUrls.get(index))
    }

    fun rand(start: Int, end: Int): Int {
        require(start <= end) { "Illegal Argument" }
        return (start..end).random()
    }

    private fun locationPermissionGranted() = (PermissionUtils.isLocationPermissionEnabled(context))

    private fun getlocationPermissionAndLoction() {
        PermissionUtils.locationPermission(activity,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            getLocation()
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
    private fun getLocation() {
        fusedLocationClient.lastLocation?.addOnSuccessListener(
            activity,
            OnSuccessListener { location ->
                if (location == null) {
                    progressBar.visibility = View.GONE
                    return@OnSuccessListener
                }

                lastLocation = location
                showToast("location : ${location}")
                getLocationAndUpload()

                // Determine whether a Geocoder is available.
                if (!Geocoder.isPresent()) {
                    progressBar.visibility = View.GONE
                    return@OnSuccessListener
                }

                // If the user pressed the fetch address button before we had the location,
                // this will be set to true indicating that we should kick off the intent
                // service after fetching the location.
                //if (addressRequested) startIntentService()
            })?.addOnFailureListener(activity) {
            showToast(activity.getString(R.string.generic_message_for_error))
        }
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
                            request.locality =
                                SearchLocality(location.latitude, location.longitude)
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
}