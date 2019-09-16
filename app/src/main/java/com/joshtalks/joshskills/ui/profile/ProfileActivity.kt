package com.joshtalks.joshskills.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ActivityPersonalDetailBinding
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.Toast
import com.andrefrsousa.superbottomsheet.SuperBottomSheetFragment
import com.esafirm.imagepicker.features.ImagePicker
import com.esafirm.imagepicker.features.ReturnMode
import com.joshtalks.joshskills.databinding.FragmentMediaSelectBinding
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.florent37.singledateandtimepicker.dialog.SingleDateAndTimePickerDialog
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.service.UploadWorker
import com.joshtalks.joshskills.repository.local.model.ImageModel
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.toolbar_login_register.*
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

const val CROPPING_IMAGE_CODE = 1717


class ProfileActivity : BaseActivity(), MediaSelectCallback {

    private lateinit var layout: ActivityPersonalDetailBinding
    private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd")
    private val DATE_FORMATTER_2 = SimpleDateFormat("dd - MMM - yyyy")

    private var userDob = Date()
    private var imageModel: ImageModel? = null
    private var pickerStatus = false


    private var picker = SingleDateAndTimePickerDialog.Builder(this)
        .bottomSheet()
        .curved()
        .displayMinutes(false)
        .displayHours(false)
        .displayDays(false)
        .displayMonth(true)
        .displayYears(true)
        .displayDaysOfMonth(true)
        .listener {
            userDob = it
            layout.etDOB.setText(DATE_FORMATTER_2.format(userDob))
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        layout = DataBindingUtil.setContentView(
            this,
            R.layout.activity_personal_detail
        )

        layout.lifecycleOwner = this
        layout.handler = this

        ivBackButton.setOnClickListener { this@ProfileActivity.finish() }


    }

    override fun onSelect(media: Media) {


        if (media == Media.CAMERA) {
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            ImagePicker.cameraOnly().start(this@ProfileActivity)
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        token?.continuePermissionRequest();

                    }

                }).withErrorListener {
                    openSettings()
                }
                .onSameThread()
                .check();
        } else if (media == Media.GALLERY) {

            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE

                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            ImagePicker.create(this@ProfileActivity)
                                .returnMode(ReturnMode.GALLERY_ONLY) // set whether pick and / or camera action should return immediate result or not.
                                .folderMode(true) // folder mode (false by default)
                                .toolbarFolderTitle("Folder") // folder selection title
                                .toolbarImageTitle("Tap to select") // image selection title
                                .toolbarArrowColor(Color.BLACK) // Toolbar 'up' arrow color
                                .includeVideo(false) // Show video on image picker
                                .single() // single mode
                                .limit(1) // max images can be selected (99 by default)
                                .imageDirectory("Camera") // directory name for captured image  ("Camera" folder by default)
                                // .theme(R.style.CustomImagePickerTheme) // must inherit ef_BaseTheme. please refer to sample
                                .enableLog(false) // disabling log
                                .start() // start
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        token?.continuePermissionRequest();

                    }

                }).withErrorListener {
                    openSettings()
                }
                .onSameThread()
                .check();

        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            val images = ImagePicker.getFirstImageOrNull(data)
            startActivityForResult(getCroppingActivity(images.path), CROPPING_IMAGE_CODE)
        } else if (requestCode == CROPPING_IMAGE_CODE && resultCode == Activity.RESULT_OK) {
            val image = data?.getStringExtra(SOURCE_IMAGE)
            imageModel = image?.let { ImageModel(it) }
            Glide.with(applicationContext)
                .load(Uri.fromFile(File(image)))
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false

                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        layout.ivPlaceholderPic.visibility = GONE
                        imageModel?.let {
                            UploadWorker.uploadProfile(it)
                        }
                        return false
                    }

                })
                .into(layout.ivUploadedPic)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    fun onProfilePicClicked() {
        val sheet = MediaPickerFragment()
        sheet.show(supportFragmentManager, "MediaPickerFragment")
    }

    fun onNextPressed() {

        if (layout.etName.text.toString().isEmpty()) {
            showStatus("Name cannot be empty!", STATUS_TYPE.ERROR)
            return
        }

        if (layout.etDOB.text.isEmpty()) {
            showStatus("Please enter Date of birth", STATUS_TYPE.ERROR)
            return
        }

        if (getGender().isEmpty()) {
            showStatus("Please select Gender", STATUS_TYPE.ERROR)
            return
        }
        if (validAge()) {

            Toast.makeText(applicationContext, "Age is not less then 12 years", Toast.LENGTH_SHORT)
                .show()
        }

        updateProfile()
    }

    private fun validAge(): Boolean {
        val dob = Calendar.getInstance();
        dob.time = userDob
        val today = Calendar.getInstance();
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return false
    }

    private fun showStatus(text: String, type: STATUS_TYPE) {

        layout.tvStatus.visibility = View.VISIBLE
        layout.tvStatus.text = text

        if (type == STATUS_TYPE.ERROR) {
            layout.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.textError))
        } else if (type == STATUS_TYPE.SUCCESS) {
            layout.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.textSuccess))
        }

    }

    fun selectDateOfBirth() {
        picker.display()
        pickerStatus = true
    }


    private fun getGender(): String {

        return when {
            layout.rbMale.isChecked -> "M"
            layout.rbfemale.isChecked -> "F"
            layout.rbOther.isChecked -> "O"
            else -> ""
        }

    }


    override fun onBackPressed() {
        if (pickerStatus) {
            picker.dismiss()
            return
        }
        super.onBackPressed()

    }

    private fun updateProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val obj = UpdateUserPersonal(
                    layout.etName.text.toString(),
                    DATE_FORMATTER.format(userDob), getGender()
                )

                val updateProfileResponse: UpdateProfileResponse =
                    AppObjectController.signUpNetworkService.updateUserAsync(
                        User.getInstance().id,
                        obj
                    )
                        .await()

                User.getInstance().updateFromResponse(updateProfileResponse)
                if (imageModel != null) {
                    //   WorkMangerPapa.startUploadProfileinWorker(imageModel!!)
                }
                AppAnalytics.updateUser()
                withContext(Dispatchers.Main) {
                    startActivity(getInboxActivityIntent())
                    finish()
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

    }


}

class MediaPickerFragment : SuperBottomSheetFragment() {

    lateinit var callback: MediaSelectCallback
    private lateinit var fragmentMediaSelectBinding: FragmentMediaSelectBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as MediaSelectCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        fragmentMediaSelectBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_media_select, container, false)
        fragmentMediaSelectBinding.lifecycleOwner = this
        fragmentMediaSelectBinding.handler = this
        return fragmentMediaSelectBinding.root
    }

    fun onSelectGallery() {
        callback.onSelect(Media.GALLERY)
        dismiss()
    }

    fun onSelectCamera() {
        callback.onSelect(Media.CAMERA)
        dismiss()

    }

}


interface MediaSelectCallback {
    fun onSelect(media: Media)
}

enum class Media {
    CAMERA, GALLERY
}

private enum class STATUS_TYPE {
    ERROR, SUCCESS
}




