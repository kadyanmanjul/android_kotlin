package com.joshtalks.joshskills.ui.help

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.request.RequestOptions
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.progress.FlipProgressDialog
import com.joshtalks.joshskills.databinding.FragmentLodgeComplaintBinding
import com.joshtalks.joshskills.repository.server.RequestComplaint
import com.joshtalks.joshskills.repository.server.TypeOfHelpModel
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import com.muddzdev.styleabletoast.StyleableToast
import jp.wasabeef.glide.transformations.RoundedCornersTransformation


const val COMPLAINT_OBJECT = "complaint_object"

class ComplaintFragment : Fragment() {

    private lateinit var lodgeComplaintBinding: FragmentLodgeComplaintBinding
    private lateinit var typeOfHelpModel: TypeOfHelpModel
    private var attachmentPath: String? = null
    private lateinit var viewModel: HelpViewModel


    private lateinit var progressDialog: FlipProgressDialog

    private fun initProgressDialog() {
        progressDialog = FlipProgressDialog()
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setDimAmount(0.8f)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // typeOfHelpModel = arguments?.getSerializable(COMPLAINT_OBJECT) as TypeOfHelpModel
        viewModel = activity?.run { ViewModelProviders.of(this)[HelpViewModel::class.java] }
            ?: throw Exception("Invalid Activity")

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
        titleView?.text = "Sagar jain"
        viewModel.apiCallStatusLiveData.observe(viewLifecycleOwner, Observer {
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


    fun attachMedia() {
        val pickPhoto = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        pickPhoto.type = "image/*"
        val mimeTypes = arrayOf("image/jpeg", "image/png")
        pickPhoto.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        startActivityForResult(pickPhoto, 1)
    }

    fun removeAttachMedia() {
        lodgeComplaintBinding.imageContainer.visibility = View.GONE
        lodgeComplaintBinding.ivAttach.visibility = View.VISIBLE
        attachmentPath = null
    }

    @SuppressLint("Recycle")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            super.onActivityResult(requestCode, resultCode, data)
            if (resultCode == Activity.RESULT_OK && requestCode == 1) {
                val selectedImage: Uri = data?.data!!
                val filePathColumn =
                    arrayOf(MediaStore.Images.Media.DATA)
                val cursor: Cursor =
                    activity?.contentResolver?.query(
                        selectedImage,
                        filePathColumn,
                        null,
                        null,
                        null
                    )!!
                cursor.moveToFirst()
                val columnIndex: Int = cursor.getColumnIndex(filePathColumn[0])
                attachmentPath = cursor.getString(columnIndex)
                cursor.close()
                attachmentPath?.run {
                    setAttachmentThumbnail(this)
                }

            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    private fun setAttachmentThumbnail(path: String) {
        try {
            val cursor =
                MediaStore.Images.Thumbnails.queryMiniThumbnails(
                    activity?.contentResolver, Uri.parse(path),
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    null
                )
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                val uri = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA))
                setImageInImageView(uri)
                //  lodgeComplaintBinding.ivThumbnail.setImageURI(Uri.parse(uri))
                lodgeComplaintBinding.imageContainer.visibility = View.VISIBLE
                lodgeComplaintBinding.ivAttach.visibility = View.GONE
            } else {
                setImageInImageView(path)
                //  lodgeComplaintBinding.ivThumbnail.setImageURI(Uri.parse(path))
                lodgeComplaintBinding.imageContainer.visibility = View.VISIBLE
                lodgeComplaintBinding.ivAttach.visibility = View.GONE
            }
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


    fun submitComplaint() {
        if (lodgeComplaintBinding.etName.text.toString().isEmpty()) {
            showToast("Please enter name")
            return
        }

        if (lodgeComplaintBinding.etNumber.text.toString().isEmpty()) {
            showToast("Please enter valid phone number")
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
            3,
            lodgeComplaintBinding.etEmail.text.toString(),
            attachmentPath,
            lodgeComplaintBinding.etNumber.text.toString(),
            lodgeComplaintBinding.etName.text.toString(),
            lodgeComplaintBinding.etComplaint.text.toString(),
            "6260268380"
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
        fun newInstance() = ComplaintFragment().apply {
            arguments = Bundle().apply {
                //putSerializable(COMPLAINT_OBJECT, typeOfHelpModel)
            }

        }
    }

//    private fun validPhoneNumber(number: String): Boolean {
//        return PHONE_NUMBER_REGEX.containsMatchIn(input = number)
//    }

}
