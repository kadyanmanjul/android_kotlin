package com.joshtalks.joshskills.ui.referral

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.crashlytics.android.Crashlytics
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.FragmentEnterReferralCodeBinding
import com.joshtalks.joshskills.repository.server.ReferralCouponDetailResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*


class EnterReferralCodeFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentEnterReferralCodeBinding

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
        setListeners()
        return binding.root
    }

    private fun setListeners() {
        binding.tvReferralCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (count != 0) binding.next.visibility = View.VISIBLE
                else binding.next.visibility = View.INVISIBLE
            }
        })
    }

    fun validateAndMoveToNextFragment() {
        CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
            binding.progressBar.visibility=View.VISIBLE
            try {
                val data = HashMap<String, String>()
                data["coupon"] = binding.tvReferralCode.text.toString()
                data["instance_id"] = PrefManager.getStringValue(INSTANCE_ID)

                val res =
                    AppObjectController.signUpNetworkService.validateAndGetReferralDetails(data)
                val res2= ReferralCouponDetailResponse(true,"Manjul","get as much as discount you want")
                if (res2.referralStatus) {
                    PrefManager.put(REFERRED_REFERRAL_CODE, data["coupon"].toString())
                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .replace(
                            R.id.parent_Container,
                            CongratulationFragment.newInstance(res),
                            "Congratulation"
                        )
                        .commit()
                }
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
