package com.joshtalks.joshskills.ui.tooltip

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.skydoves.balloon.OnBalloonClickListener
import com.joshtalks.skydoves.balloon.OnBalloonDismissListener
import de.hdodenhof.circleimageview.CircleImageView

object TooltipUtility {


    fun showFirstTimeUserTooltip(
        inboxEntity: InboxEntity?,
        context: Context,
        lifecycleOwner: LifecycleOwner,
        callback: () -> Unit
    ) {
        if (inboxEntity == null) {
            return
        }
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.fragment_first_time_user_dialog)
        dialog.findViewById<CircleImageView>(R.id.profile_image)
            .setImageResource(R.drawable.ic_josh_course)

        dialog.findViewById<AppCompatTextView>(R.id.tv_name).text = inboxEntity.course_name
        dialog.findViewById<AppCompatTextView>(R.id.tv_last_message).text =
            "Apni pehli class dekhne k liye click kare "

        Glide.with(context)
            .load(inboxEntity.course_icon)
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .into(dialog.findViewById<CircleImageView>(R.id.profile_image))


        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.8f)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnDismissListener {
            dialog.dismiss()
        }
        dialog.show()
        val window: Window = dialog.window!!
        window.setLayout(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        val root = dialog.findViewById<RelativeLayout>(R.id.root_view)
        val profileBalloon by lazy {
            BalloonFactory.getFirstTimeUserBalloon(
                context,
                lifecycleOwner,
                object : OnBalloonClickListener {
                    override fun onBalloonClick(view: View) {
                        dialog.dismiss()
                    }
                }, object : OnBalloonDismissListener {
                    override fun onBalloonDismiss() {
                        dialog.dismiss()
                    }

                })
        }
        profileBalloon.showAlignBottomException(root)
        callback()
    }


}