package com.joshtalks.joshskills.ui.view_holders

import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.joshtalks.joshskills.databinding.CardLocationAutocompleteBinding
import com.joshtalks.joshskills.messaging.RxBus
import com.joshtalks.joshskills.messaging.RxBus2
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
        this.binding.root.setOnClickListener {
            RxBus.getDefault().send(googleSearchLocationObj)
        }
    }


}