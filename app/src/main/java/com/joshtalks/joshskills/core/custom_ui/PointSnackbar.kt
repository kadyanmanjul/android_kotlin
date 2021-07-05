package com.joshtalks.joshskills.core.custom_ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.findSuitableParent
import timber.log.Timber

class PointSnackbar(
    parent: ViewGroup,
    content: PointsSnackbarView
) : BaseTransientBottomBar<PointSnackbar>(parent, content, content) {

    init {
        getView().setBackgroundColor(
            ContextCompat.getColor(
                view.context,
                android.R.color.transparent
            )
        )
        getView().setPadding(0, 0, 0, 0)
    }

    companion object {

        fun make(view: View, duration: Int, action_lable: String?): PointSnackbar? {

            // First we find a suitable parent for our custom view
            val parent = view.findSuitableParent() ?: throw IllegalArgumentException(
                "No suitable parent found from the given view. Please provide a valid view."
            )

            // We inflate our custom view
            try {
                val customView = LayoutInflater.from(view.context).inflate(
                    R.layout.point_snackbar_view,
                    parent,
                    false
                ) as PointsSnackbarView
                // We create and return our Snackbar
                action_lable?.let {
                    customView.tvMsg.text = action_lable
                }

                return PointSnackbar(
                    parent,
                    customView
                ).setDuration(duration)
                    .setAnimationMode(ANIMATION_MODE_SLIDE)

            } catch (e: Exception) {
                Timber.e(e)
            }

            return null
        }
    }

}
