package com.joshtalks.joshskills.ui.fpp.adapters

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.ui.fpp.model.PendingRequestDetail

object PendingRequestsBindingAdapter {
    @BindingAdapter(value = ["recentCallImage"], requireAll = false)
    @JvmStatic
    fun recentCallImage(imageView: ImageView, caller: PendingRequestDetail?) {
        caller?.let {
            imageView.setUserImageOrInitials(it.photoUrl, it.fullName ?: "", isRound = true)
        } ?: imageView.setImageResource(R.drawable.ic_call_placeholder)
    }
}