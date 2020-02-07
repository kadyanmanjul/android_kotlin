package com.joshtalks.joshskills.ui.referral


import android.content.Intent
import android.graphics.Bitmap
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
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.FragmentPrmotationDialogBinding
import com.joshtalks.joshskills.ui.payment.COURSE_ID
import com.joshtalks.joshskills.ui.payment.PaymentActivity
import com.joshtalks.joshskills.ui.view_holders.IMAGE_SIZE
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class PromotionDialogFragment : DialogFragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var fragmentPrmotationDialogBinding: FragmentPrmotationDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
    }


    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = AppObjectController.screenWidth * .9
            val height = AppObjectController.screenHeight * .7
            dialog.window?.setLayout(width.toInt(), height.toInt())
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
        setImageView("http://cdn.pixabay.com/photo/2015/04/19/08/32/rose-729509_960_720.jpg")
    }

    private fun setImageView(url: String) {

        val multi = MultiTransformation<Bitmap>(
            CropTransformation(
                Utils.dpToPx(IMAGE_SIZE),
                Utils.dpToPx(IMAGE_SIZE),
                CropTransformation.CropType.CENTER
            ),
            RoundedCornersTransformation(
                Utils.dpToPx(ROUND_CORNER),
                0,
                RoundedCornersTransformation.CornerType.ALL
            )
        )
        Glide.with(AppObjectController.joshApplication)
            .load(url)
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(RequestOptions.bitmapTransform(multi))
            .into(fragmentPrmotationDialogBinding.ivPromotion)
    }

    fun cancelPromotion() {
        dismissAllowingStateLoss()
    }

    fun openPromotion() {
        activity!!.startActivity(Intent(activity!!, PaymentActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra(COURSE_ID, "1")
        })
        dismissAllowingStateLoss()
    }


    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PromotionDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
