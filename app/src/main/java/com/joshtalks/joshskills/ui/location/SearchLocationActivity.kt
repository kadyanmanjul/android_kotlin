package com.joshtalks.joshskills.ui.location

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.custom_ui.JoshTextWatcher
import com.joshtalks.joshskills.databinding.ActivitySearchLocationBinding
import com.joshtalks.joshskills.repository.local.model.googlelocation.GoogleSearchLocationObj
import com.joshtalks.joshskills.ui.location.adapter.SearchLocationAdapter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_search_location.*


const val KEY_PLACE = "place"

class SearchLocationActivity : BaseActivity(), OnFailureListener,
    OnSuccessListener<FindAutocompletePredictionsResponse> {

    private lateinit var layout: ActivitySearchLocationBinding

    private val listCards = ArrayList<AutocompletePrediction>()
    private val adapter = SearchLocationAdapter(listCards)
    private lateinit var placesClient: PlacesClient

    private val handler = Handler()

    private var compositeDisposable = CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)

        layout = DataBindingUtil.setContentView(this, R.layout.activity_search_location)
        layout.handler = this

        layout.rvList.layoutManager = LinearLayoutManager(this)
        layout.rvList.adapter = adapter

        Places.initialize(
            applicationContext,
            resources.getString(R.string.google_places_api_key)
        )
        placesClient = Places.createClient(this)

        layout.etSearch.addTextChangedListener(object : JoshTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                super.afterTextChanged(s)

                layout.pbLoading.visibility = VISIBLE
                performQuery(s.toString())
            }
        })

        layout.etSearch.requestFocus()


        /*compositeDisposable.add(
            RxBus.getDefault().toObservable()
            .subscribeOn(Schedulers.io()).subscribe({
                if (it is GoogleSearchLocationObj) {
                    onPlaceSelected(it)
                }
            }, {

                it.printStackTrace()
            })
        )*/

    }

    private fun performQuery(s: String) {

        handler.removeCallbacksAndMessages(null)

        handler.postDelayed({

            Log.d(TAG, "performing search $s")

            val request = FindAutocompletePredictionsRequest.builder()
                .setCountry("in")
                .setTypeFilter(TypeFilter.CITIES)
                .setSessionToken(AutocompleteSessionToken.newInstance())
                .setQuery(s)
                .build()

            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(this@SearchLocationActivity)
                .addOnFailureListener(this@SearchLocationActivity)

        }, 1000)

    }


    private fun onClearList() {

        adapter.notifyItemRangeRemoved(0, listCards.size)
        listCards.clear()

    }

    override fun onFailure(e: Exception) {
        onClearList()

        layout.pbLoading.visibility = View.INVISIBLE
        Log.e(TAG, "onFailure: $e")

    }


    override fun onSuccess(response: FindAutocompletePredictionsResponse) {
        onClearList()
        layout.pbLoading.visibility = View.INVISIBLE

        for (prediction in response.autocompletePredictions) {

            listCards.add(prediction)
        }

        Log.d(TAG, "onSuccess: ")
        adapter.notifyItemRangeInserted(0, listCards.size)

    }

    private fun onPlaceSelected(obj: GoogleSearchLocationObj) {

        if (progress_dialog.isVisible) progress_dialog.visibility=GONE

        progress_dialog.visibility= VISIBLE

        val placeFields = listOf(Place.Field.ID, Place.Field.LAT_LNG)

        val request = FetchPlaceRequest.newInstance(obj.placeId, placeFields)

        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            val intent = Intent()
            intent.putExtra(KEY_PLACE, place)
            setResult(RESULT_OK, intent)
            finish()

        }.addOnFailureListener { exception ->
            Toast.makeText(
                this,
                "Something went wrong!",
                Toast.LENGTH_SHORT
            ).show()
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}
