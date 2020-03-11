package com.joshtalks.joshskills.ui.referral

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.FragmentPrmotationDialogBinding
import com.joshtalks.joshskills.ui.payment.COURSE_ID
import com.joshtalks.joshskills.ui.payment.PaymentActivity
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

private const val ARG_COURSE_ID = "course_id"
private const val ARG_PLACEHOLDER_IMAGE = "placeholder_url"

class PromotionDialogFragment : DialogFragment() {
    private var courseId: String? = null
    private var placeHolderImageUrl: String? = null
    private lateinit var fragmentPrmotationDialogBinding: FragmentPrmotationDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            courseId = it.getString(ARG_COURSE_ID)
            placeHolderImageUrl = it.getString(ARG_PLACEHOLDER_IMAGE)
        }
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)

    }


    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = AppObjectController.screenWidth * .9
            val height = AppObjectController.screenHeight * .8
            dialog.window?.setLayout(width.toInt(), height.toInt())
            dialog.setCanceledOnTouchOutside(false)
            dialog.setCancelable(false)

        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentPrmotationDialogBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_prmotation_dialog, container, false)
        fragmentPrmotationDialogBinding.lifecycleOwner = this
        fragmentPrmotationDialogBinding.handler = this
        return fragmentPrmotationDialogBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        placeHolderImageUrl?.run {
            setImageView(this)
        }
    }

    private fun setImageView(url: String) {

        val width = AppObjectController.screenWidth * .8
        val height = AppObjectController.screenHeight * .7

        val multi = MultiTransformation(
            RoundedCornersTransformation(
                Utils.dpToPx(ROUND_CORNER),
                8,
                RoundedCornersTransformation.CornerType.ALL
            )
        )
        Glide.with(AppObjectController.joshApplication)
            .load(url)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .override(width.toInt(), height.toInt())
            .apply(RequestOptions.bitmapTransform(multi))
            .into(fragmentPrmotationDialogBinding.ivPromotion)
    }

    fun cancelPromotion() {
        dismissAllowingStateLoss()
    }

    fun openPromotion() {
        requireActivity().startActivity(Intent(requireActivity(), PaymentActivity::class.java).apply {
            putExtra(COURSE_ID, courseId)
        })
        dismissAllowingStateLoss()
    }


    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PromotionDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_COURSE_ID, param1)
                    putString(ARG_PLACEHOLDER_IMAGE, param2)
                }
            }
    }
}
