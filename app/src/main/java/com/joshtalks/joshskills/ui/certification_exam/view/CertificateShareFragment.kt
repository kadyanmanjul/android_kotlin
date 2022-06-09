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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.FragmentCertificateShareBinding
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.certification_exam.constants.CERTIFICATE_URL
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.moengage.core.internal.utils.getSystemService
import kotlin.properties.Delegates

class CertificateShareFragment : CoreJoshFragment() {

    private lateinit var binding: FragmentCertificateShareBinding
    private lateinit var url: String
    private var packageName = "whatsapp"
    private var DownloadId by Delegates.notNull<Long>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            url = it.getString(CERTIFICATE_URL, EMPTY)
        }
        requireActivity().registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_certificate_share, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        Glide.with(binding.imgCertificate.context).load("https://picsum.photos/200/300").into(binding.imgCertificate)
        PrefManager.put(IS_CERTIFICATE_GENERATED, false)
        if (PrefManager.getBoolValue(IS_FIRST_TIME_CERTIFICATE, defValue = true)) {
            binding.btnShareLinkedIn.visibility = View.GONE
            binding.btnShareFacebook.visibility = View.GONE
            binding.btnShareDownload.visibility = View.GONE
            binding.btnShareInsta.visibility = View.GONE
            PrefManager.put(IS_FIRST_TIME_CERTIFICATE, false)
        }
        binding.txtCongratulations.text = "Congratulations, ${User.getInstance().firstName}!"

        binding.btnShareWhatsapp.setOnClickListener {
            Log.i("TAG", "onCreateView: $url")
            packageName = "whatsapp"
            downloadImage("https://picsum.photos/200/300")
        }
        binding.btnShareFacebook.setOnClickListener {
            packageName="facebook"
            downloadImage(url)
        }
        binding.btnShareInsta.setOnClickListener {
            packageName ="instagram.android"
            downloadImage(url)
        }
        binding.btnShareLinkedIn.setOnClickListener {
            packageName= "linkedin.android"
            downloadImage(url)
        }
        binding.btnShareDownload.setOnClickListener {
            downloadImage(url)
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
                        try{
                            if (intent.resolveActivity(requireActivity().packageManager) == null) {
                                Toast.makeText(requireContext(), "$packageName not found on device", Toast.LENGTH_LONG).show()
                            }
                            startActivity(intent)
                        }catch (e:Exception){
                            e.printStackTrace()
                        }
                        return
                    }
                    if (report.isAnyPermissionPermanentlyDenied) {
                        Toast.makeText(context, "Permission denied", Toast.LENGTH_LONG).show()
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
        val downloadManager: DownloadManager = getSystemService(requireContext(), DOWNLOAD_SERVICE) as DownloadManager

        val downloadUri = Uri.parse(url)

        val request = DownloadManager.Request(downloadUri)
        val fileName = URLUtil.guessFileName(url, null, "image/png")
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setMimeType("image/*")
            .setTitle(URLUtil.guessFileName(url, null, "image/png"))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, fileName
            )
        DownloadId = downloadManager.enqueue(request)
    }

    val onComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE){
                intent.extras?.let {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == DownloadId){
                        //retrieving the file
                        val downloadedFileId = it.getLong(DownloadManager.EXTRA_DOWNLOAD_ID)
                        val downloadManager = getSystemService(requireContext(), DOWNLOAD_SERVICE) as DownloadManager
                        val uri: Uri = downloadManager.getUriForDownloadedFile(downloadedFileId)
                        Toast.makeText(requireContext(), "Downloaded!", Toast.LENGTH_LONG).show()
                        shareOn(packageName,"check this out",uri)
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance(certificateUrl: String): CertificateShareFragment {
            val args = Bundle()
            args.putString(CERTIFICATE_URL, certificateUrl)
            val fragment = CertificateShareFragment()
            fragment.arguments = args
            return fragment
        }
    }
}