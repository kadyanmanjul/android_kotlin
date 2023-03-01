package com.joshtalks.joshskills.ui.help

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.util.Patterns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.custom_ui.progress.FlipProgressDialog
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.databinding.FragmentLodgeComplaintBinding
import com.joshtalks.joshskills.repository.server.RequestComplaint
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import com.joshtalks.joshskills.util.DeviceInfoUtils
import com.muddzdev.styleabletoast.StyleableToast
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import timber.log.Timber
import java.io.File


const val COMPLAINT_ID = "complaint_id"
const val COMPLAINT_CATEGORY = "complaint_category"

class ComplaintFragment : Fragment() {

    private lateinit var lodgeComplaintBinding: FragmentLodgeComplaintBinding
    private var attachmentPath: String? = null
    private val viewModel by lazy {
        ViewModelProvider(requireActivity())[HelpViewModel::class.java]
    }
    private var complaintID : Int = 0
    private var complaintCategory: String = "Report Complaint"


    private lateinit var progressDialog: FlipProgressDialog

    private fun initProgressDialog() {
        progressDialog = FlipProgressDialog()
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setDimAmount(0.8f)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        complaintID = arguments?.getInt(COMPLAINT_ID) as Int
        complaintCategory = arguments?.getString(COMPLAINT_CATEGORY) as String
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        lodgeComplaintBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_lodge_complaint, container, false)
        lodgeComplaintBinding.lifecycleOwner = this
        lodgeComplaintBinding.handler = this
        initProgressDialog()
        return lodgeComplaintBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val titleView = activity?.findViewById<AppCompatTextView>(R.id.text_message_title)
        titleView?.text = complaintCategory
        viewModel.apiCallStatusLiveDataComplaint.observe(viewLifecycleOwner, Observer {
            if (it == ApiCallStatus.SUCCESS) {
                progressDialog.dismissAllowingStateLoss()
                MaterialDialog(requireActivity()).show {
                    message(R.string.complaint_message, viewModel.complaintResponse.ticketId) {
                        lineSpacing(1.2f)
                    }
                    positiveButton(R.string.ok) {
                        requireActivity().finish()
                    }
                }
            } else {
                progressDialog.dismissAllowingStateLoss()

            }
        })
    }


    fun attachMedia(v:View) {
        ImagePicker.with(this)
            .crop()          //Crop image(Optional), Check Customization for more option
            .galleryOnly()
            .saveDir(File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ImagePicker"))
            .start(ImagePicker.REQUEST_CODE)
    }

    fun removeAttachMedia(v:View) {
        lodgeComplaintBinding.imageContainer.visibility = View.GONE
        lodgeComplaintBinding.ivAttach.visibility = View.VISIBLE
        attachmentPath = null
    }

    @SuppressLint("Recycle")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            super.onActivityResult(requestCode, resultCode, data)
            if (resultCode == Activity.RESULT_OK) {
                val url = data?.data?.path ?: EMPTY
                if (url.isNotBlank()) {
                    addUserImageInView(url)
                }
            } else if (resultCode == ImagePicker.RESULT_ERROR) {
                Timber.e(ImagePicker.getError(data))
            } else {
                Timber.e("Task Cancelled")
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    private fun addUserImageInView(imagePath: String) {
        val imageUpdatedPath = AppDirectory.getImageSentFilePath()
        AppDirectory.copy(imagePath, imageUpdatedPath)
        setAttachmentThumbnail(imagePath)
        attachmentPath = imageUpdatedPath
    }

    private fun setAttachmentThumbnail(path: String) {
        try {
            setImageInImageView(path)
            lodgeComplaintBinding.imageContainer.visibility = View.VISIBLE
            lodgeComplaintBinding.ivAttach.visibility = View.GONE
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    private fun setImageInImageView(path: String) {
        val multi = MultiTransformation<Bitmap>(
            RoundedCornersTransformation(
                Utils.dpToPx(ROUND_CORNER),
                0,
                RoundedCornersTransformation.CornerType.ALL
            )
        )
        Glide.with(requireActivity())
            .load(path)
            .apply(RequestOptions.bitmapTransform(multi))
            .into(lodgeComplaintBinding.ivThumbnail)
    }


    fun submitComplaint(v:View) {
        if (lodgeComplaintBinding.etName.text.toString().isEmpty()) {
            showToast("Please enter name")
            return
        }

        if (lodgeComplaintBinding.etNumber.text.isNullOrEmpty() || isValidFullNumber(
                "+91",
                lodgeComplaintBinding.etNumber.text.toString()
            ).not()
        ) {
            showToast(getString(R.string.please_enter_valid_number))
            return
        }

        if (lodgeComplaintBinding.etEmail.text.toString().isEmpty() || Patterns.EMAIL_ADDRESS.matcher(
                lodgeComplaintBinding.etEmail.text.toString()
            ).matches().not()
        ) {
            showToast("Please enter valid email")
            return
        }

        if (lodgeComplaintBinding.etComplaint.text.toString().isEmpty()) {
            showToast("Please type in your problem")
            return
        }
        if (Utils.isInternetAvailable().not()) {
            Toast.makeText(
                AppObjectController.joshApplication,
                getString(R.string.internet_not_available_msz),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        progressDialog.show(requireFragmentManager(), "ProgressDialog")
        val requestComplaint = RequestComplaint(
            complaintID,
            lodgeComplaintBinding.etEmail.text.toString(),
            attachmentPath,
            lodgeComplaintBinding.etNumber.text.toString(),
            lodgeComplaintBinding.etName.text.toString(),
            lodgeComplaintBinding.etComplaint.text.toString(),
            DeviceInfoUtils.getMobileDetails()
        )
        viewModel.requestComplaint(requestComplaint)
    }

    private fun showToast(message: String) {
        StyleableToast.Builder(requireActivity()).gravity(Gravity.BOTTOM)
            .text(message).cornerRadius(16).length(Toast.LENGTH_LONG)
            .solidBackground().show()
    }


    companion object {
        @JvmStatic
        fun newInstance(id: Int, category: String) = ComplaintFragment().apply {
            arguments = Bundle().apply {
                putInt(COMPLAINT_ID, id)
                putString(COMPLAINT_CATEGORY, category)
            }

        }
    }
}
