package com.joshtalks.joshskills.premium.ui.fpp.adapters

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.setUserImageOrInitials
import com.joshtalks.joshskills.premium.ui.fpp.model.PendingRequestDetail
import com.joshtalks.joshskills.premium.ui.fpp.model.RecentCall

object PendingRequestsBindingAdapter {
    @BindingAdapter(value = ["recentCallImage"], requireAll = false)
    @JvmStatic
    fun recentCallImage(imageView: ImageView, caller: PendingRequestDetail?) {
        caller?.let {
            imageView.setUserImageOrInitials(it.photoUrl, it.fullName?:"", isRound = true)
        } ?: imageView.setImageResource(R.drawable.ic_call_placeholder)
    }
}