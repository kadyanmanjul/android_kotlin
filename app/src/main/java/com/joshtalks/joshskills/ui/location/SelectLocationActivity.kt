package com.joshtalks.joshskills.ui.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.model.Place
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.databinding.ActivitySelectLocationBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.googlelocation.Locality
import kotlinx.android.synthetic.main.layout_select_location_request_permission.*
import pub.devrel.easypermissions.EasyPermissions

import com.google.android.gms.location.LocationCallback;
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.repository.server.ProfileResponse
import com.joshtalks.joshskills.repository.server.SearchLocality
import com.joshtalks.joshskills.repository.server.UpdateUserLocality
import kotlinx.android.synthetic.main.layout_select_location_confirmation.*
import kotlinx.coroutines.*


const val RC_PLAY_SERVICES_RESOLUTION_REQUEST = 20
const val RC_SEARCH_LOCATION = 23
const val RC_LOCATION_PERMISSION = 12
const val RC_REQUEST_CHECK_SETTINGS = 21


class SelectLocationActivity : BaseActivity(), EasyPermissions.PermissionCallbacks {


    private lateinit var layout: ActivitySelectLocationBinding

    private var googleApiClient: GoogleApiClient? = null

    private var locality: Locality = Locality()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest

    private lateinit var mGeoLocationCallback: GeoLocationCallback


    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        layout = DataBindingUtil.setContentView(
            this,
            com.joshtalks.joshskills.R.layout.activity_select_location
        )
        layout.handler = this
        layout.tvFirstName.text = Mentor.getInstance().getUser()?.firstName ?: ""
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()
        createLocationCallback()
    }

    override fun onStart() {
        super.onStart()

    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(5000)
            .setFastestInterval(5000)
    }

    fun onEnterManuallyClicked(v: View) {
        val intent = Intent(this, SearchLocationActivity::class.java)
        startActivityForResult(intent, RC_SEARCH_LOCATION)
    }

    fun onAllowPermissionClicked(v: View) {

        val perms = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (EasyPermissions.hasPermissions(this, *perms)) {
            onPermissionsGranted(RC_LOCATION_PERMISSION, perms.toMutableList())
        } else {
            EasyPermissions.requestPermissions(
                this,
                getString(com.joshtalks.joshskills.R.string.location_rationale),
                RC_LOCATION_PERMISSION, *perms
            )
        }

    }


    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        enableGPSSensor()

    }

    private fun createLocationCallback() {
        mGeoLocationCallback = GeoLocationCallback(object : LocationUpdatedListener {
            override fun onLocationReceived(lat: Double, lng: Double) {
                onReceivedUserLocation(lat, lng)
            }

        })
    }


    private fun getLastKnownLocation() {
        fusedLocationProviderClient.lastLocation
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful && task.result != null) {
                    onReceivedUserLocation(task.result!!.latitude, task.result!!.longitude)
                } else {
                    updateLocation()
                }
            }
    }


    private fun updateLocation() {
        layout.vfFlipper.displayedChild = STATE_FETCHING_LOCATION
        fusedLocationProviderClient.requestLocationUpdates(
            mLocationRequest,
            mGeoLocationCallback, null
        )
    }


    private fun enableGPSSensor() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
        builder.setAlwaysShow(true)
        val mLocationSettingsRequest = builder.build()

        val mSettingsClient = LocationServices.getSettingsClient(this)

        mSettingsClient
            .checkLocationSettings(mLocationSettingsRequest)
            .addOnSuccessListener { getLastKnownLocation() }
            .addOnFailureListener { e ->
                when ((e as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val rae = e as ResolvableApiException
                        rae.startResolutionForResult(
                            this@SelectLocationActivity,
                            RC_REQUEST_CHECK_SETTINGS
                        )
                    } catch (sie: IntentSender.SendIntentException) {
                        onFailedToFetchLocation()
                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        onFailedToFetchLocation()
                    }
                }
            }
            .addOnCanceledListener {
                onFailedToFetchLocation()
            }

    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {

        tvEnterManually.visibility = View.VISIBLE
        Toast.makeText(this, "You've denied permission! Please enter Manually.", Toast.LENGTH_SHORT)
            .show()
    }

    private fun onFailedToFetchLocation() {

        Toast.makeText(baseContext, "Couldn't Locate you!", Toast.LENGTH_SHORT).show()
        layout.vfFlipper.displayedChild = STATE_REQUEST_PERMISSION
        tvEnterManually.visibility = View.VISIBLE
        terminateGPS()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);

    }

    private fun terminateGPS() {
        fusedLocationProviderClient.removeLocationUpdates(mGeoLocationCallback)
        if (googleApiClient != null)
            googleApiClient!!.disconnect()

    }

    private fun onReceivedUserLocation(latitude: Double, longitude: Double) {
        terminateGPS()
        CoroutineScope(Dispatchers.IO).launch {
            val map = mapOf("latitude" to latitude.toString(), "longitude" to longitude.toString())
            try {
                locality =
                    AppObjectController.signUpNetworkService.confirmUserLocationAsync(map).await()


                withContext(Dispatchers.Main) {
                    layout.vfFlipper.displayedChild = STATE_CONFIRMATION
                    tvLocation.text = locality.name + ", " + locality.state.name

                }
            } catch (e: Exception) {
                e.printStackTrace()
                // onFailedToFetchLocation()
            }


        }

    }

    fun onCancelFetchingLocationClicked(v: View) {
        onFailedToFetchLocation()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        when (requestCode) {

            RC_LOCATION_PERMISSION, RC_REQUEST_CHECK_SETTINGS -> {
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

                if (!isGpsEnabled)
                    onFailedToFetchLocation()
                else
                    getLastKnownLocation()

            }

            RC_SEARCH_LOCATION -> {

                if (resultCode == RESULT_OK) {
                    val place = data!!.getParcelableExtra<Place>(KEY_PLACE)
                    locality = Locality()
                    locality.latitude = place.latLng?.latitude ?: 0.toDouble()
                    locality.longitude = place.latLng?.longitude ?: 0.toDouble()
                    setLocation()
                }

            }
        }

    }

    override fun onBackPressed() {

        if (layout.vfFlipper.displayedChild > 0) {
            layout.vfFlipper.displayedChild = 0
            tvEnterManually.visibility = View.VISIBLE
            terminateGPS()
        } else {
            super.onBackPressed()
        }

    }

    fun onConfirmLocalityClicked(v: View) {
        setLocation()
    }

    private fun setLocation() {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = UpdateUserLocality()
                request.locality = SearchLocality(locality.latitude, locality.longitude)

                val response: ProfileResponse =
                    AppObjectController.signUpNetworkService.updateUserAddressAsync(
                        Mentor.getInstance().getId(),
                        request
                    ).await()
                Mentor.getInstance().setLocality(response.locality).update()
                startActivity(getIntentForState())

            } catch (e: Exception) {
                e.printStackTrace()
                // onFailedToFetchLocation()
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()

        terminateGPS()
    }

    companion object {

        private val STATE_REQUEST_PERMISSION = 0
        private val STATE_FETCHING_LOCATION = 1
        private val STATE_CONFIRMATION = 2
    }

    inner class GeoLocationCallback(private val listener: LocationUpdatedListener) :
        LocationCallback() {

        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            var location: Location? = null

            for (locationResultLocation in locationResult!!.locations) {
                location = locationResultLocation
            }

            if (location == null)
                location = locationResult.lastLocation


            location?.latitude?.let { listener.onLocationReceived(it, location.longitude) }
        }
    }

    interface LocationUpdatedListener {
        abstract fun onLocationReceived(lat: Double, lng: Double)
    }

}
