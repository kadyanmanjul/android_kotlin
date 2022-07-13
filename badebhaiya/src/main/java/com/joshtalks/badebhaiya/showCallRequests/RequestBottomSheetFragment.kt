package com.joshtalks.badebhaiya.showCallRequests

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.databinding.BottomSheetRequestBinding
import com.joshtalks.badebhaiya.showCallRequests.model.RequestData
import com.joshtalks.badebhaiya.showCallRequests.viewModel.RequestContentViewModel
import com.joshtalks.badebhaiya.showCallRequests.viewModel.RequestsViewModel
import com.joshtalks.badebhaiya.utils.Utils
import com.joshtalks.badebhaiya.utils.setUserImageOrInitials
import kotlinx.android.synthetic.main.bottom_sheet_request.view.*
import timber.log.Timber

class RequestBottomSheetFragment(
    private val callRequest: RequestData
) : BottomSheetDialogFragment() {

    private val viewModel by lazy {
        ViewModelProvider(this)[RequestContentViewModel::class.java]
    }

    private lateinit var binding: BottomSheetRequestBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = BottomSheetRequestBinding.inflate(inflater, container, false)
        Timber.d("CALL REQUEST => ${callRequest.user}")
        binding.callRequest = callRequest
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getRequestContent(callRequest.user.user_id)
//        binding.requestProfilePicture
        if (callRequest.user.photo_url.isNullOrEmpty().not())
            Utils.setImage(binding.requestProfilePicture, callRequest.user.photo_url)
        else
            Utils.setImage(binding.requestProfilePicture, callRequest.user.short_name)
        binding.requestProfilePicture.setUserImageOrInitials(callRequest.user.photo_url,
            callRequest.user.short_name[0],30)
        attachObservers()
    }

    private fun attachObservers() {
        viewModel.requestContent.observe(this){
           binding.requests.requests.adapter = RequestContentAdapter(it.reqeust_data, callRequest.user)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: Dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { p0 ->
            val bottomSheetDialog = p0 as BottomSheetDialog
            setupFullHeight(bottomSheetDialog)
        }
        return dialog
    }


    private fun setupFullHeight(bottomSheetDialog: BottomSheetDialog) {
        val bottomSheet: FrameLayout? =
            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
        val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(
            bottomSheet!!
        )
        val layoutParams: ViewGroup.LayoutParams = bottomSheet.getLayoutParams()
        val windowHeight = getWindowHeight()
        if (layoutParams != null) {
            layoutParams.height = windowHeight
        }
        bottomSheet.setLayoutParams(layoutParams)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun getWindowHeight(): Int {
        // Calculate window height for fullscreen use
        val displayMetrics = DisplayMetrics()
        (context as Activity?)?.getWindowManager()?.getDefaultDisplay()?.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }



    companion object {
        const val TAG = "RequestBottomSheetFragment"
    }
}