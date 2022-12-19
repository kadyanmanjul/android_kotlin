package com.joshtalks.joshskills.certificate.view

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.joshtalks.joshskills.certificate.constants.*
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.repository.local.model.User
import com.joshtalks.joshskills.certificate.CertificationExamViewModel
import com.joshtalks.joshskills.certificate.R
import com.joshtalks.joshskills.certificate.databinding.FragmentCertificateShareBinding
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlin.properties.Delegates

class CertificateShareFragment : CoreJoshFragment() {

    private lateinit var binding: FragmentCertificateShareBinding
    private val viewModel by lazy {
        ViewModelProvider(this)[CertificationExamViewModel::class.java]
    }
    private lateinit var url: String
    private var packageName =  NULL
    private var downloadId by Delegates.notNull<Long>()
    private var message = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            url = it.getString(CERTIFICATE_URL, EMPTY)
            viewModel.certificateExamId = it.getInt(CERTIFICATE_EXAM_ID)
        }
        observer()
        requireActivity().registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_certificate_share, container, false)
        binding.lifecycleOwner = this
        binding.vm = viewModel
        PrefManager.put(IS_FIRST_TIME_FLOW_CERTI, true)
        viewModel.typeOfExam()

        if(url.isEmpty()){
            showToast(getString(R.string.something_went_wrong))
        }

        Glide.with(binding.imgCertificate.context).load(url)
            .into(binding.imgCertificate)

        if (PrefManager.getBoolValue(IS_FIRST_TIME_CERTIFICATE, defValue = true)) {
            if (isPackageInstalled(PACKAGE_NAME_WHATSAPP)){
                viewModel.btnDownloadVisibility.set(false)
                viewModel.btnWhatsappVisibility.set(true)
            }
            else{
                viewModel.btnDownloadVisibility.set(true)
                viewModel.btnWhatsappVisibility.set(false)
            }
            viewModel.btnLinkedInVisibility.set(false)
            viewModel.btnFacebookVisibility.set(false)
            viewModel.btnInstaVisibility.set(false)
            PrefManager.put(IS_FIRST_TIME_CERTIFICATE, false)
        } else {
            viewModel.btnDownloadVisibility.set(true)
            viewModel.btnWhatsappVisibility.set(isPackageInstalled(PACKAGE_NAME_WHATSAPP))
            viewModel.btnFacebookVisibility.set(isPackageInstalled(PACKAGE_NAME_FACEBOOK))
            viewModel.btnInstaVisibility.set(isPackageInstalled(PACKAGE_NAME_INSTA))
            viewModel.btnLinkedInVisibility.set(isPackageInstalled(PACKAGE_NAME_LINKEDIN))
        }
        viewModel.certiShareHeadingText.set("Congratulations, ${User.getInstance().firstName}!")


        binding.btnShareWhatsapp.setOnClickListener {
            if (url.isEmpty()) {
                showToast(getString(R.string.something_went_wrong))
                return@setOnClickListener
            }
            if (Utils.isInternetAvailable()){
                packageName = PKG_AFTER_COM_WHATSAPP
                viewModel.progressBarVisibility.set(true)
                downloadImage(url)
                viewModel.saveImpression(CERTIFICATE_SHARED_WHATSAPP)
            }
            else {
                showToast(getString(R.string.internet_not_available_msz))
            }
        }

        binding.btnShareFacebook.setOnClickListener {
            if (url.isEmpty()) {
                showToast(getString(R.string.something_went_wrong))
                return@setOnClickListener
            }
            if (Utils.isInternetAvailable()){
                packageName = PKG_AFTER_COM_FACEBOOK
                viewModel.progressBarVisibility.set(true)
                downloadImage(url)
                viewModel.saveImpression(CERTIFICATE_SHARED_FB)
            }
            else {
                showToast(getString(R.string.internet_not_available_msz))
            }
        }

        binding.btnShareInsta.setOnClickListener {
            if (url.isEmpty()) {
                showToast(getString(R.string.something_went_wrong))
                return@setOnClickListener
            }
            if (Utils.isInternetAvailable()){
                packageName = PKG_AFTER_COM_INSTA
                viewModel.progressBarVisibility.set(true)
                downloadImage(url)
                viewModel.saveImpression(CERTIFICATE_SHARED_INSTA)
            }else {
                showToast(getString(R.string.internet_not_available_msz))
            }
        }

        binding.btnShareLinkedIn.setOnClickListener {
            if (url.isEmpty()) {
                showToast(getString(R.string.something_went_wrong))
                return@setOnClickListener
            }
            if (Utils.isInternetAvailable()){
                packageName = PKG_AFTER_COM_LINKEDIN
                viewModel.progressBarVisibility.set(true)
                downloadImage(url)
                viewModel.saveImpression(CERTIFICATE_SHARED_LINKED)
            }
            else{
                showToast(getString(R.string.internet_not_available_msz))
            }
        }

        binding.btnShareDownload.setOnClickListener {
            if (url.isEmpty()) {
                showToast(getString(R.string.something_went_wrong))
                return@setOnClickListener
            }
            if (Utils.isInternetAvailable()){
                packageName = NULL
                viewModel.progressBarVisibility.set(true)
                downloadImage(url)
                viewModel.saveImpression(CERTIFICATE_DOWNLOAD)
            }
            else {
                showToast(getString(R.string.internet_not_available_msz))
            }
        }
        return binding.root
    }

    fun observer(){
        viewModel.examType.observe(viewLifecycleOwner){
            when (it){
                EXAM_TYPE_BEGINNER ->{
                    PrefManager.put(IS_CERTIFICATE_GENERATED_BEGINNER, true)
                }
                EXAM_TYPE_INTERMEDIATE ->{
                    PrefManager.put(IS_CERTIFICATE_GENERATED_INTERMEDIATE, true)
                }
                EXAM_TYPE_ADVANCED ->{
                    PrefManager.put(IS_CERTIFICATE_GENERATED_ADVANCED, true)
                }
            }
        }
    }

    fun shareOn(packageName: String, message: String, uri: Uri) {
        if (isAdded && activity!=null) {
            PermissionUtils.storageReadAndWritePermission(requireContext(), object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.`package` = "com.$packageName"
                            intent.putExtra(Intent.EXTRA_TEXT, message)
                            intent.type = "image/*"
                            intent.putExtra(Intent.EXTRA_STREAM, uri)
                            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            try {
                                if (intent.resolveActivity(requireActivity().packageManager) == null) {
                                    showToast("$packageName not found on device", Toast.LENGTH_LONG)
                                }
                                startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            showToast("Permission denied", Toast.LENGTH_LONG)
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
        }
    }

    fun downloadImage(url: String) {
        if (url.isEmpty()) {
            showToast(getString(R.string.something_went_wrong))
            return
        }
        if (isAdded && activity!=null) {
            PermissionUtils.storageReadAndWritePermission(requireContext(), object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    try {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                val downloadManager: DownloadManager =
                                    AppObjectController.joshApplication.getSystemService(Context.DOWNLOAD_SERVICE) as (DownloadManager)

                                val downloadUri = Uri.parse(url)

                                val request = DownloadManager.Request(downloadUri)
                                val fileName = "${User.getInstance().firstName}.jpeg"
                                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                                    .setMimeType("image/jpeg")
                                    .setTitle("${User.getInstance().firstName}.jpeg")
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setDestinationInExternalPublicDir(
                                        Environment.DIRECTORY_DOWNLOADS, fileName
                                    )
                                downloadId = downloadManager.enqueue(request)
                                return
                            }
                            if (report.isAnyPermissionPermanentlyDenied) {
                                showToast("Permission denied", Toast.LENGTH_LONG)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
        }

    }

    val onComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                intent.extras?.let {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L)
                    if (id == downloadId) { // checking if the downloaded file is our certificate
                        //retrieving the file
                        viewModel.progressBarVisibility.set(false)
                        val downloadedFileId = it.getLong(DownloadManager.EXTRA_DOWNLOAD_ID)
                        val downloadManager = AppObjectController.joshApplication.getSystemService(Context.DOWNLOAD_SERVICE) as (DownloadManager)
                        val uri: Uri? = downloadManager.getUriForDownloadedFile(downloadedFileId)
                        when (packageName) {
                            PKG_AFTER_COM_WHATSAPP -> {
                                message = AppObjectController.getFirebaseRemoteConfig()
                                    .getString(FirebaseRemoteConfigKey.CERTIFICATE_SHARE_TEXT_WHATSAPP).replace("\\n", "\n")
                            }
                            PKG_AFTER_COM_FACEBOOK -> {
                                message = AppObjectController.getFirebaseRemoteConfig()
                                    .getString(FirebaseRemoteConfigKey.CERTIFICATE_SHARE_TEXT_FB).replace("\\n", "\n")
                            }
                            PKG_AFTER_COM_INSTA -> {
                                message = AppObjectController.getFirebaseRemoteConfig()
                                    .getString(FirebaseRemoteConfigKey.CERTIFICATE_SHARE_TEXT_INSTA).replace("\\n", "\n")
                            }
                            PKG_AFTER_COM_LINKEDIN -> {
                                message = AppObjectController.getFirebaseRemoteConfig()
                                    .getString(FirebaseRemoteConfigKey.CERTIFICATE_SHARE_TEXT_LINKEDIN).replace("\\n", "\n")
                            }
                            NULL -> {
                                message = ""
                                showToast("Certificate Downloaded", Toast.LENGTH_LONG)
                            }
                        }
                        if (message != "" && packageName != NULL) {
                            getDeepLinkAndShare(uri)
                        }
                    }
                }
            }
        }
    }

    private fun getDeepLinkAndShare(uri: Uri?) {
        com.joshtalks.joshskills.common.util.DeepLinkUtil(requireContext())
            .setReferralCode(Mentor.getInstance().referralCode)
            .setReferralCampaign()
            .setSharedItem(com.joshtalks.joshskills.common.util.DeepLinkUtil.SharedItem.CERTIFICATE)
            .setListener(object : com.joshtalks.joshskills.common.util.DeepLinkUtil.OnDeepLinkListener {
                override fun onDeepLinkCreated(deepLink: String) {
                    if (uri != null) {
                        shareOn(packageName, message + "\n" + deepLink, uri)
                    } else {
                        showToast("Something went wrong", Toast.LENGTH_LONG)
                    }
                }
            })
            .build()
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return Utils.isPackageInstalled(
            packageName,
            requireContext()
        )
    }

    companion object {
        fun newInstance(certificateUrl: String,certificateExamId:Int): CertificateShareFragment {
            val args = Bundle()
            args.putString(CERTIFICATE_URL, certificateUrl)
            args.putInt(CERTIFICATE_EXAM_ID, certificateExamId)
            val fragment = CertificateShareFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            requireActivity().unregisterReceiver(onComplete)
        }
        catch (e:Exception){
            e.printStackTrace()
        }
    }
}