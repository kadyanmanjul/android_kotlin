package com.joshtalks.joshskills.ui.payment

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.ImprovedBulletSpan
import com.joshtalks.joshskills.databinding.FragmentHintTooltipBinding
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.support.v4.dip

class HintTooltipDialog : DialogFragment() {

    private lateinit var binding: FragmentHintTooltipBinding
    private var compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
    }


    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = AppObjectController.screenWidth * .9
            dialog.window?.setLayout(width.toInt(), FrameLayout.LayoutParams.WRAP_CONTENT)
            /* dialog.window?.setLayout(
                 FrameLayout.LayoutParams.MATCH_PARENT,
                 FrameLayout.LayoutParams.MATCH_PARENT
             )
             */
            dialog.setCanceledOnTouchOutside(true)
            dialog.setCancelable(true)
            dialog.window?.setDimAmount(0.9F)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_hint_tooltip,
            container,
            false
        )
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnClickListener {
            dismissAllowingStateLoss()
        }

        val spannableString = SpannableString(getString(R.string.tip_message))

        spannableString.setSpan(
            ImprovedBulletSpan(bulletRadius = dip(3), gapWidth = dip(8)),
            0,
            spannableString.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )

        binding.tvTip.text = spannableString


        compositeDisposable.add(AppObjectController.appDatabase
            .courseDao()
            .isUserOldThen7Days()
            .concatMap {
                val (_, dayRemain) = com.joshtalks.joshskills.core.Utils.isUser7DaysOld(it.courseCreatedDate)
                return@concatMap Maybe.just(dayRemain)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { value ->
                    binding.tvTipValid.text = "VALID FOR $value DAYS"
                },
                { error ->
                    error.printStackTrace()
                }
            ))

    }


    companion object {
        @JvmStatic
        fun newInstance() = HintTooltipDialog()


    }

}