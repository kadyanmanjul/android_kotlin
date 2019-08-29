package com.joshtalks.joshskills.ui.view_holders

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.CardLocationAutocompleteBinding
import com.joshtalks.joshskills.messaging.RxBus
import com.joshtalks.joshskills.repository.local.model.googlelocation.GoogleSearchLocationObj

class PlaceApiViewHolder(
    private val binding: CardLocationAutocompleteBinding
) : RecyclerView.ViewHolder(binding.root) {
    var googleSearchLocationObj = GoogleSearchLocationObj()

    fun bind(prediction: AutocompletePrediction) {
         googleSearchLocationObj = GoogleSearchLocationObj(
            prediction.getPrimaryText(null).toString(),
            prediction.getFullText(null).toString(),
            prediction.placeId
        )
        this.binding.tvName.text = googleSearchLocationObj.name
        this.binding.tvFormattedAddress.text = googleSearchLocationObj.formattedAddress


    }

    fun onClick() {
        RxBus.getDefault().send(googleSearchLocationObj)
    }


}