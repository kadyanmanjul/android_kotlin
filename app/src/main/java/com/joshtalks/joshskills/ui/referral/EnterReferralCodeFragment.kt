package com.joshtalks.joshskills.ui.referral

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.crashlytics.android.Crashlytics
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.FragmentEnterReferralCodeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*


class EnterReferralCodeFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentEnterReferralCodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BaseBottomSheetDialog)
        changeDialogConfiguration()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = false
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_enter_referral_code,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.fragment = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
    }

    private fun setListeners() {
        binding.tvReferralCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    if(it.length>0){binding.next.visibility = View.VISIBLE

                    }else binding.next.visibility = View.INVISIBLE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })
    }

    private fun changeDialogConfiguration() {
        val params: WindowManager.LayoutParams? = dialog?.window?.attributes
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        params?.height = WindowManager.LayoutParams.MATCH_PARENT
        params?.gravity = Gravity.CENTER
        dialog?.window?.attributes = params
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    fun validateAndMoveToNextFragment() {
        CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
            hideKeyboard(requireActivity(), binding.tvReferralCode)
            binding.progressBar.visibility=View.VISIBLE
            try {
                val data = HashMap<String, String>()
                data["instance_id"] = PrefManager.getStringValue(INSTANCE_ID)
                data["coupon"] = binding.tvReferralCode.text.toString()

                val res =
                    AppObjectController.signUpNetworkService.validateAndGetReferralDetails(data)
                if (res.referralStatus) {
                    binding.wrongCode.visibility=View.GONE
                    PrefManager.put(REFERRED_REFERRAL_CODE, data["coupon"].toString())
                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .replace(
                            R.id.parent_Container,
                            ValidReferralCodeFragment.newInstance(res),
                            "Congratulation"
                        )
                        .commit()
                }
                else binding.wrongCode.visibility=View.VISIBLE
            } catch (ex: Exception) {
                when (ex) {
                    is HttpException -> {
                        showToast(getString(R.string.generic_message_for_error))
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        showToast(getString(R.string.internet_not_available_msz))
                    }
                    else -> {
                        Crashlytics.logException(ex)
                    }
                }
            }
            binding.progressBar.visibility=View.GONE
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = EnterReferralCodeFragment()
    }
}
