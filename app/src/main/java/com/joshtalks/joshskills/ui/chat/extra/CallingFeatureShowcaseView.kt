package com.joshtalks.joshskills.ui.chat.extra

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.skydoves.balloon.ArrowOrientation
import com.joshtalks.skydoves.balloon.Balloon
import com.joshtalks.skydoves.balloon.BalloonAnimation
import com.joshtalks.skydoves.balloon.OnBalloonDismissListener
import com.joshtalks.skydoves.balloon.TextForm
import io.github.inflationx.calligraphy3.TypefaceUtils
import kotlinx.android.synthetic.main.calling_feature_showcas_view.iv_call
import kotlinx.android.synthetic.main.calling_feature_showcas_view.root_view
import kotlinx.android.synthetic.main.calling_feature_showcas_view.toolbar

class CallingFeatureShowcaseView : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), R.style.full_dialog) {
            override fun onBackPressed() {
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.run {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            window?.setLayout(width, height)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.calling_feature_showcas_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.inflateMenu(R.menu.conversation_menu)
        val typefaceSpan = TypefaceUtils.load(requireContext().assets, "fonts/OpenSans-Regular.ttf")
        val textForm: TextForm = TextForm.Builder(requireContext())
            .setText(getString(R.string.english_practise_hint))
            .setTextColorResource(R.color.black)
            .setTextSize(14f)
            .setTextTypeface(typefaceSpan)
            .build()
        val ballon = Balloon.Builder(requireContext())
            .setTextForm(textForm)
            .setArrowSize(10)
            .setArrowVisible(true)
            .setWidthRatio(0.85f)
            .setSpace(8)
            .setArrowPosition(0.84f)
            .setArrowOrientation(ArrowOrientation.TOP)
            .setArrowVisible(true)
            .setHeight(Utils.dpToPx(28))
            .setDismissWhenTouchOutside(true)
            .setCornerRadius(4f)
            .setBackgroundColorResource(R.color.yellow)
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(this)
            .setDismissWhenClicked(true)
            .setOnBalloonDismissListener {
            }
            .setOnBalloonDismissListener(object : OnBalloonDismissListener {
                override fun onBalloonDismiss() {
                    dismissAllowingStateLoss()
                }
            })
            .build()
        ballon.showAlignBottom(iv_call)
        root_view.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }


    companion object {
        @JvmStatic
        fun newInstance() = CallingFeatureShowcaseView()
    }

}