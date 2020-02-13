package com.joshtalks.joshskills.ui.payment

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.google.gson.JsonSyntaxException
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.TextDrawable
import com.joshtalks.joshskills.databinding.FragmentCouponCodeSubmitBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.referral.DRAWABLE_RIGHT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.textColor
import retrofit2.HttpException


class CouponCodeSubmitFragment : DialogFragment() {
    private var listener: OnCouponCodeSubmitListener? = null
    private lateinit var binding: FragmentCouponCodeSubmitBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
    }


    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = AppObjectController.screenWidth * .9
            val height = AppObjectController.screenHeight * .35
            dialog.window?.setLayout(width.toInt(), height.toInt())
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_coupon_code_submit,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.etCode.requestFocus()

        val clipBoardManager = context?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val copiedString = clipBoardManager.primaryClip?.getItemAt(0)?.text?.toString()
        Log.i("copytext", copiedString ?: "nope")

        binding.etCode.setOnTouchListener { _, event ->
            try {
                if (event.action == MotionEvent.ACTION_UP) {
                    if (event.rawX >= (binding.etCode.right - binding.etCode.compoundDrawables[DRAWABLE_RIGHT].bounds.width())) {
                        validatingCouponCode(binding.etCode.text.toString().trim())
                        return@setOnTouchListener true
                    }
                }
            } catch (ex: Exception) {
            }
            return@setOnTouchListener false
        }



        binding.etCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.trim().isNullOrEmpty()) {
                    binding.etCode.setCompoundDrawables(null, null, null, null)
                } else {
                    addDrawableInEditText()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })

        binding.etCode.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
            source.toString().filterNot { it.isWhitespace() }
        })

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnCouponCodeSubmitListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }


    interface OnCouponCodeSubmitListener {
        fun getCouponCode(code: String?)
    }

    fun completePayment() {
        listener?.getCouponCode(binding.etCode.text?.toString())
        dismissAllowingStateLoss()
    }

    companion object {
        @JvmStatic
        fun newInstance() = CouponCodeSubmitFragment()
    }

    private fun addDrawableInEditText() {
        val font = Typeface.createFromAsset(context!!.assets, "fonts/OpenSans-SemiBold.ttf")
        val drawable: TextDrawable = TextDrawable.builder()
            .beginConfig()
            .textColor(ContextCompat.getColor(activity!!, R.color.button_primary_color))
            .useFont(font)
            .fontSize(Utils.dpToPx(12))
            .toUpperCase()
            .endConfig()
            .buildRect("Apply      ", Color.TRANSPARENT)
        binding.etCode.setCompoundDrawables(null, null, drawable, null)

    }


    private fun validatingCouponCode(couponCode: String) {
        if (couponCode.isEmpty()) {
            return
        }

        if (Mentor.getInstance().hasId() && couponCode.equals(
                Mentor.getInstance().referralCode,
                ignoreCase = true
            )
        ) {
            inValidCode()
            return
        }


        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reqObj = mapOf("code" to couponCode)
                val response =
                    AppObjectController.signUpNetworkService.validateOrGetAndReferralOrCouponAsync(
                        reqObj
                    ).await()
                if (response.isNullOrEmpty()) {
                    inValidCode()
                } else {
                    validCode()
                }
            } catch (ex: JsonSyntaxException) {
                inValidCode()
            } catch (ex: HttpException) {
                ex.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            hideProgressBar()

        }
    }

    private fun hideProgressBar() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    private fun validCode() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.couponValidTv.text = "Coupon Successfully Applied"
            binding.couponValidTv.textColor =
                ContextCompat.getColor(context!!, R.color.color_success)
            binding.couponValidTv.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_check_circle,
                0,
                0,
                0
            )

            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    private fun inValidCode() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.couponValidTv.text = "Coupon Code Invalid"
            binding.couponValidTv.textColor =
                ContextCompat.getColor(context!!, R.color.error_color)
            binding.couponValidTv.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_error_outline,
                0,
                0,
                0
            )

            binding.progressBar.visibility = View.INVISIBLE
        }
    }
}
