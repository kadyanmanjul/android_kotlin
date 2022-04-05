package com.joshtalks.badebhaiya.core

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationRequest
import com.joshtalks.badebhaiya.R
import com.patloew.colocation.CoLocation
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.*

const val HELP_ACTIVITY_REQUEST_CODE = 9010
const val COURSE_EXPLORER_NEW = 2008
const val REQUEST_SHOW_SETTINGS = 123

abstract class BaseActivity :
    AppCompatActivity(),
    LifecycleObserver {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(this)
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.status_bar_color)
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        AppObjectController.screenHeight = displayMetrics.heightPixels
        AppObjectController.screenWidth = displayMetrics.widthPixels
        lifecycleScope.launch(Dispatchers.IO) {
            InstallReferralUtil.installReferrer(applicationContext)
        }
    }

    fun showProgressBar() {
        lifecycleScope.launch(Dispatchers.Main) {
            FullScreenProgressDialog.showProgressBar(this@BaseActivity)
        }
    }

    fun hideProgressBar() {
        lifecycleScope.launch(Dispatchers.Main) {
            FullScreenProgressDialog.hideProgressBar(this@BaseActivity)
        }
    }

    private var locationUpdatesJob: Job? = null
    private val coLocation: CoLocation by lazy {
        CoLocation.from(applicationContext)
    }
    private val locationRequest: LocationRequest by lazy {
        LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(5000)
    }

    @SuppressLint("MissingPermission")
    protected fun fetchUserLocation() {
        lifecycleScope.launch(Dispatchers.IO) {
            when (val settingsResult = coLocation.checkLocationSettings(locationRequest)) {
                CoLocation.SettingsResult.Satisfied -> {
                    val location = coLocation.getLastLocation()
                    if (null == location) {
                        startLocationUpdates()
                    }
                    else {
                        onUpdateLocation(location)
                    }
                }
                is CoLocation.SettingsResult.Resolvable -> {
                    settingsResult.resolve(this@BaseActivity, REQUEST_SHOW_SETTINGS)
                }
                else -> { /* Ignore for now, we can't resolve this anyway */
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        try {
            locationUpdatesJob?.cancel()
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
        locationUpdatesJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                coLocation.getLocationUpdates(locationRequest).collectLatest {
                    onUpdateLocation(it)
                    locationUpdatesJob?.cancel()
                }
            }
            catch (e: CancellationException) {
                e.printStackTrace()
            }
        }
    }

    protected suspend fun uploadUserLocation(location: Location) {
        try {
            /*val request = UpdateUserLocality()
            request.locality =
                    SearchLocality(location.latitude, location.longitude)
            AppAnalytics.setLocation(
                    location.latitude,
                    location.longitude
            )
            val response: ProfileResponse =
                    AppObjectController.signUpNetworkService.updateUserAddressAsync(
                            Mentor.getInstance().getId(), request
                    )
            Mentor.getInstance().setLocality(response.locality).update()*/
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    open fun onUpdateLocation(location: Location) {}
    open fun onDenyLocation() {}
}
