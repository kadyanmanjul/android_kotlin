package com.joshtalks.joshskills.ui.course_details.viewholder

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
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.VERSION
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.repository.server.SearchLocality
import com.joshtalks.joshskills.repository.server.UpdateUserLocality
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.joshtalks.joshskills.repository.server.course_detail.LocationStats
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.patloew.rxlocation.RxLocation
import io.reactivex.CompletableObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.regex.Pattern

@Layout(R.layout.layout_location_stats_view_holder)
class LocationStatViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private var locationStats: LocationStats,
    val activity: Activity,
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

    private var compositeDisposable = CompositeDisposable()

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
            })
    }

    @SuppressLint("MissingPermission")
    private fun getLocationAndUpload() {
        val rxLocation = RxLocation(AppObjectController.joshApplication)
        val locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(10000)
        rxLocation.settings().checkAndHandleResolutionCompletable(locationRequest)
            .subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.computation())
            .subscribe(object : CompletableObserver {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onComplete() {
                    compositeDisposable.add(
                        rxLocation.location().updates(locationRequest)
                            .subscribeOn(Schedulers.computation())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ location ->
                                try {
                                    val request = UpdateUserLocality()
                                    request.locality =
                                        SearchLocality(location.latitude, location.longitude)
                                    getAddressAndSetView(location.latitude, location.longitude)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                hideProgressBar()
                                compositeDisposable.clear()
                            }, { ex ->
                                ex.printStackTrace()
                            })
                    )
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                    hideProgressBar()
                }
            })
    }

    private fun hideProgressBar() {
        AppObjectController.uiHandler.post {
            progressBar.visibility = View.GONE
        }
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
        if (city.isNullOrBlank().not() && city.equals(addresses[0].locality))
            return
        city = addresses[0].locality
        state = addresses[0].adminArea
        AppObjectController.uiHandler.post {
            stateCityName.text = city.plus(" , ").plus(state)
            showNextImageAndRandomData()
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
