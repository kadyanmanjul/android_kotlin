package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.core.isValidFullNumber
import com.joshtalks.joshskills.databinding.FragmentBookACallBinding
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.SalesReasonList
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.viewmodel.BuyPageViewModel
import com.joshtalks.joshskills.ui.special_practice.utils.REASON_SUBMITTED_BACK
import com.joshtalks.joshskills.ui.special_practice.utils.SUPPORT_REASON_LIST

class BookACallFragment : BaseFragment() {

    lateinit var binding: FragmentBookACallBinding
    var listOfReason: MutableList<String> = mutableListOf()
    var salesReasonList:SalesReasonList?=null

    val vm by lazy {
        ViewModelProvider(requireActivity())[BuyPageViewModel::class.java]
    }

    override fun initViewBinding() {
        binding.vm = vm
        binding.executePendingBindings()
    }

    override fun initViewState() {
        liveData.observe(this) {
            when (it.what) {
                REASON_SUBMITTED_BACK -> requireActivity().onBackPressed()
                SUPPORT_REASON_LIST -> {
                    (it.obj as List<String>).forEach { obj ->
                        listOfReason.add(obj)
                    }

                    val arrayAdapter = ArrayAdapter(
                        requireContext(),
                        R.layout.dropdown_item,
                        listOfReason
                    )
                    binding.autoCompleteTextViewFirst.setAdapter(arrayAdapter)

                    if (vm.alreadyReasonSelected!=null){
                        binding.nameEditText.isEnabled = false
                        binding.phoneNumberEt.isEnabled = false
                        binding.autoCompleteTextViewFirst.isEnabled = false
                        binding.autoCompleteTextViewFirst.isClickable = false
                        binding.autoCompleteTextViewFirst.setText(vm.alreadyReasonSelected.toString())
                        binding.btnSubmitData.isEnabled = false
                        binding.btnSubmitData.backgroundTintList =
                            AppCompatResources.getColorStateList(requireActivity(), R.color.disabled)
                    }
                }
            }
        }
    }

    override fun setArguments() {}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_book_a_call, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.getSupportReason()
        initUI()
    }

    fun initUI() {
        val user = User.getInstance()

        buttonEnabledOrDisable(user)

        if (user.firstName.isNullOrEmpty().not())
            binding.nameEditText.setText(user.firstName)

        if (user.phoneNumber.isNullOrEmpty().not())
            binding.phoneNumberEt.setText(user.phoneNumber)

        binding.btnSubmitData.setOnClickListener {
            submitReason()
        }

    }

    fun buttonEnabledOrDisable(user: User) {
        if (user.firstName.isNullOrEmpty().not() || user.phoneNumber.isNullOrEmpty().not()) {
            binding.btnSubmitData.isEnabled = true
            binding.btnSubmitData.backgroundTintList =
                AppCompatResources.getColorStateList(requireActivity(), R.color.colorPrimary)
        } else {
            binding.btnSubmitData.isEnabled = false
            binding.btnSubmitData.backgroundTintList =
                AppCompatResources.getColorStateList(requireActivity(), R.color.disabled)
        }
    }

    fun submitReason() {
        if (binding.phoneNumberEt.text.isNullOrEmpty() || isValidFullNumber("+91", binding.phoneNumberEt.text.toString()).not()
        ) {
            showToast(getString(R.string.please_enter_valid_number))
            return
        }
        val param = HashMap<String, String>()
        if (binding.nameEditText.text.toString().isNotEmpty() && binding.phoneNumberEt.text.toString().isNotEmpty() && binding.autoCompleteTextViewFirst.text.toString().isNotEmpty() && (binding.autoCompleteTextViewFirst.text.toString() !="Reason for Call")) {
            param["user_name"] = binding.nameEditText.text.toString()
            param["phone"] = binding.phoneNumberEt.text.toString()
            param["reason_to_call"] = binding.autoCompleteTextViewFirst.text.toString()
            vm.setSupportReason(param)
        } else {
            showToast("Please fill all required field")
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            BookACallFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}