package com.joshtalks.joshskills.ui.certification_exam.view

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.FragmentCertificateShareBinding
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.certification_exam.CertificationExamViewModel
import com.joshtalks.joshskills.ui.certification_exam.constants.*
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.moengage.core.internal.utils.getSystemService
import kotlin.properties.Delegates

class CertificateShareFragment : CoreJoshFragment() {

    private lateinit var binding: FragmentCertificateShareBinding
    private val viewModel by lazy {
        ViewModelProvider(this).get(CertificationExamViewModel::class.java)
    }
    private lateinit var url: String
    private var packageName = "null"
    private var downloadId by Delegates.notNull<Long>()
    private var message = ""
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            url = it.getString(CERTIFICATE_URL, EMPTY)
        }

        viewModel.certificationQuestionLiveData.observe(requireActivity()){

        }
        requireActivity().registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_certificate_share, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.txtYouHaveEarned.visibility = View.GONE
        viewModel.certificateExamId = arguments?.getInt(CERTIFICATE_EXAM_ID)
        when (viewModel.certificationQuestionLiveData.value?.type){
            "beginner"->{
                PrefManager.put(IS_CERTIFICATE_GENERATED_BEGINNER, false)
            }
            "intermediate"->{
                PrefManager.put(IS_CERTIFICATE_GENERATED_INTERMEDIATE, false)
            }
            "advanced"->{
                PrefManager.put(IS_CERTIFICATE_GENERATED_ADVANCED, false)
            }
        }
        Glide.with(binding.imgCertificate.context).load(url)
            .into(binding.imgCertificate)

        if (isPackageInstalled(PACKAGE_NAME_WHATSAPP) && PrefManager.getBoolValue(IS_FIRST_TIME_CERTIFICATE, defValue = true)) {
            with(binding) {
                btnShareWhatsapp.visibility = View.VISIBLE
                btnShareFacebook.visibility = View.GONE
                btnShareInsta.visibility = View.GONE
                btnShareLinkedIn.visibility = View.GONE
                btnShareDownload.visibility = View.GONE
            }
            PrefManager.put(IS_FIRST_TIME_CERTIFICATE, false)
        } else {
            with(binding) {
                btnShareDownload.isVisible = true
                btnShareFacebook.isVisible = isPackageInstalled(PACKAGE_NAME_FACEBOOK)
                btnShareInsta.isVisible = isPackageInstalled(PACKAGE_NAME_INSTA)
                btnShareLinkedIn.isVisible = isPackageInstalled(PACKAGE_NAME_LINKEDIN)
            }
        }
        binding.txtCongratulations.text = "Congratulations, ${User.getInstance().firstName}!"

        binding.btnShareWhatsapp.setOnClickListener {
            packageName = "whatsapp"
            binding.progressBar2.visibility = View.VISIBLE
            downloadImage(url)
            viewModel.saveImpression(CERTIFICATE_SHARED_WHATSAPP)
        }

        binding.btnShareFacebook.setOnClickListener {
            packageName = "facebook.android"
            binding.progressBar2.visibility = View.VISIBLE
            downloadImage(url)
            viewModel.saveImpression(CERTIFICATE_SHARED_FB)
        }

        binding.btnShareInsta.setOnClickListener {
            packageName = "instagram.android"
            binding.progressBar2.visibility = View.VISIBLE
            downloadImage(url)
            viewModel.saveImpression(CERTIFICATE_SHARED_INSTA)
        }

        binding.btnShareLinkedIn.setOnClickListener {
            packageName = "linkedin.android"
            binding.progressBar2.visibility = View.VISIBLE
            downloadImage(url)
            viewModel.saveImpression(CERTIFICATE_SHARED_LINKED)
        }

        binding.btnShareDownload.setOnClickListener {
            packageName = "null"
            binding.progressBar2.visibility = View.VISIBLE
            downloadImage(url)
            viewModel.saveImpression(CERTIFICATE_DOWNLOAD)
        }
        return binding.root
    }

    fun shareOn(packageName: String, message: String, uri: Uri) {
        PermissionUtils.storageReadAndWritePermission(requireContext(), object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                report?.areAllPermissionsGranted()?.let { flag ->
                    if (flag) {
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.`package` = "com.$packageName"
                        intent.putExtra(Intent.EXTRA_TEXT, message)
                        intent.type = "image/*"
                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
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

    fun downloadImage(url: String) {
        PermissionUtils.storageReadAndWritePermission(requireContext(), object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                report?.areAllPermissionsGranted()?.let { flag ->
                    if (flag) {
                        val downloadManager: DownloadManager = getSystemService(requireContext(), DOWNLOAD_SERVICE) as DownloadManager

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
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                token?.continuePermissionRequest()
            }
        })

    }

    val onComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                intent.extras?.let {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L)
                    if (id == downloadId) { // checking if the downloaded file is our certificate
                        //retrieving the file
                        binding.progressBar2.visibility = View.GONE
                        val downloadedFileId = it.getLong(DownloadManager.EXTRA_DOWNLOAD_ID)
                        val downloadManager = getSystemService(requireContext(), DOWNLOAD_SERVICE) as DownloadManager
                        val uri: Uri = downloadManager.getUriForDownloadedFile(downloadedFileId)
                        when (packageName) {
                            "whatsapp" -> {
                                message = AppObjectController.getFirebaseRemoteConfig()
                                    .getString(FirebaseRemoteConfigKey.CERTIFICATE_SHARE_TEXT_WHATSAPP)
                            }
                            "facebook.android" -> {
                                message = AppObjectController.getFirebaseRemoteConfig()
                                    .getString(FirebaseRemoteConfigKey.CERTIFICATE_SHARE_TEXT_FB)
                            }
                            "instagram.android" -> {
                                message = AppObjectController.getFirebaseRemoteConfig()
                                    .getString(FirebaseRemoteConfigKey.CERTIFICATE_SHARE_TEXT_INSTA)
                            }
                            "linkedin.android" -> {
                                message = AppObjectController.getFirebaseRemoteConfig()
                                    .getString(FirebaseRemoteConfigKey.CERTIFICATE_SHARE_TEXT_LINKEDIN)
                            }
                            "null" -> {
                                message = ""
                                showToast("Certificate Downloaded", Toast.LENGTH_LONG)
                            }
                        }
                        if (message != "" && packageName != "null") {
                            shareOn(packageName, message, uri)
                        }
                    }
                }
            }
        }
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