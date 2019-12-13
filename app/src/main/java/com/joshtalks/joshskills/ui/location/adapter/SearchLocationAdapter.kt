package com.joshtalks.joshskills.ui.location.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.joshtalks.joshskills.databinding.CardLocationAutocompleteBinding
import com.joshtalks.joshskills.ui.view_holders.PlaceApiViewHolder
import java.util.*


class SearchLocationAdapter(
    private val locationList: ArrayList<AutocompletePrediction>
) : RecyclerView.Adapter<PlaceApiViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceApiViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemBinding = CardLocationAutocompleteBinding.inflate(layoutInflater, parent, false)
        return PlaceApiViewHolder(itemBinding)
    }

    override fun getItemCount(): Int {
        return locationList.size
    }

    override fun onBindViewHolder(holder: PlaceApiViewHolder, position: Int) {
        holder.bind(locationList[position])
    }
}