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
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.FragmentCertificateShareBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.certification_exam.CertificationExamViewModel
import com.joshtalks.joshskills.ui.certification_exam.constants.*
import com.joshtalks.joshskills.util.DeepLinkUtil
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.moengage.core.internal.utils.getSystemService
import kotlin.properties.Delegates

private const val PKG_AFTER_COM_WHATSAPP ="whatsapp"
private const val PKG_AFTER_COM_FACEBOOK ="facebook.android"
private const val PKG_AFTER_COM_LINKEDIN ="linkedin.android"
private const val PKG_AFTER_COM_INSTA ="instagram.android"
private const val NULL ="null"
class CertificateShareFragment : CoreJoshFragment() {

    private lateinit var binding: FragmentCertificateShareBinding
    private val viewModel by lazy {
        ViewModelProvider(this).get(CertificationExamViewModel::class.java)
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
        binding.txtYouHaveEarned.visibility = View.GONE

        viewModel.typeOfExam()

        if(url.isEmpty()){
            showToast("Oops! something went wrong")
        }

        Glide.with(binding.imgCertificate.context).load(url)
            .into(binding.imgCertificate)

        if (PrefManager.getBoolValue(IS_FIRST_TIME_CERTIFICATE, defValue = true)) {
            with(binding) {
                if (isPackageInstalled(PACKAGE_NAME_WHATSAPP)){
                    btnShareDownload.visibility = View.GONE
                    btnShareWhatsapp.visibility = View.VISIBLE
                }
                else{
                    btnShareWhatsapp.visibility = View.GONE
                    btnShareDownload.visibility = View.VISIBLE
                }
                btnShareFacebook.visibility = View.GONE
                btnShareInsta.visibility = View.GONE
                btnShareLinkedIn.visibility = View.GONE
            }
            PrefManager.put(IS_FIRST_TIME_CERTIFICATE, false)
        } else {
            with(binding) {
                btnShareDownload.isVisible = true
                btnShareWhatsapp.isVisible = isPackageInstalled(PACKAGE_NAME_WHATSAPP)
                btnShareFacebook.isVisible = isPackageInstalled(PACKAGE_NAME_FACEBOOK)
                btnShareInsta.isVisible = isPackageInstalled(PACKAGE_NAME_INSTA)
                btnShareLinkedIn.isVisible = isPackageInstalled(PACKAGE_NAME_LINKEDIN)
            }
        }
        viewModel.certiShareHeadingText.set("Congratulations, ${User.getInstance().firstName}!")


        binding.btnShareWhatsapp.setOnClickListener {
            if (Utils.isInternetAvailable()){
            packageName = PKG_AFTER_COM_WHATSAPP
            binding.progressBar2.visibility = View.VISIBLE
            downloadImage(url)
            viewModel.saveImpression(CERTIFICATE_SHARED_WHATSAPP)
            }
            else {
                showToast("No Internet Available")
            }
        }

        binding.btnShareFacebook.setOnClickListener {
            if (Utils.isInternetAvailable()){
            packageName = PKG_AFTER_COM_FACEBOOK
            binding.progressBar2.visibility = View.VISIBLE
            downloadImage(url)
            viewModel.saveImpression(CERTIFICATE_SHARED_FB)
            }
            else {
                showToast("No Internet Available")
            }
        }

        binding.btnShareInsta.setOnClickListener {
            if (Utils.isInternetAvailable()){
            packageName = PKG_AFTER_COM_INSTA
            binding.progressBar2.visibility = View.VISIBLE
            downloadImage(url)
            viewModel.saveImpression(CERTIFICATE_SHARED_INSTA)
            }else {
                showToast("No Internet Available")
            }
        }

        binding.btnShareLinkedIn.setOnClickListener {
            if (Utils.isInternetAvailable()){
            packageName = PKG_AFTER_COM_LINKEDIN
            binding.progressBar2.visibility = View.VISIBLE
            downloadImage(url)
            viewModel.saveImpression(CERTIFICATE_SHARED_LINKED)
            }
            else{
                showToast("No Internet Available")
            }
        }

        binding.btnShareDownload.setOnClickListener {
            if (Utils.isInternetAvailable()){
            packageName = NULL
            binding.progressBar2.visibility = View.VISIBLE
            downloadImage(url)
            viewModel.saveImpression(CERTIFICATE_DOWNLOAD)
            }
            else {
                showToast("No Internet Available")
            }
        }


        return binding.root
    }

    fun observer(){
        viewModel.examType.observe(viewLifecycleOwner){
            when (it){
                EXAM_TYPE_BEGINNER->{
                    PrefManager.put(IS_CERTIFICATE_GENERATED_BEGINNER, true)
                }
                EXAM_TYPE_INTERMEDIATE->{
                    PrefManager.put(IS_CERTIFICATE_GENERATED_INTERMEDIATE, true)
                }
                EXAM_TYPE_ADVANCED->{
                    PrefManager.put(IS_CERTIFICATE_GENERATED_ADVANCED, true)
                }
            }
        }
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
                            NULL-> {
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

    private fun getDeepLinkAndShare(uri: Uri) {
        DeepLinkUtil(requireActivity())
            .setReferralCode(Mentor.getInstance().referralCode)
            .setReferralCampaign()
            .setCampaign("certificate")
            .setListener(object : DeepLinkUtil.OnDeepLinkListener {
                override fun onDeepLinkCreated(deepLink: String) {
                    shareOn(packageName, message + "\n" + deepLink, uri)
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