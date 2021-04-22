package com.joshtalks.joshskills.ui.certification_exam.view

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.CertificateDownloadFragmentBinding
import com.joshtalks.joshskills.ui.certification_exam.report.udetail.CERTIFICATE_URL
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.DownloadBlock
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class CertificateDownloadDialog : DialogFragment(), FetchListener {

    private lateinit var binding: CertificateDownloadFragmentBinding
    private var certificateUrl: String = EMPTY
    private val fetch = AppObjectController.getFetchObject()
    private val TAG = CertificateDownloadDialog::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(CERTIFICATE_URL)?.run {
            certificateUrl = this
        }
        fetch.addListener(this)
    }

    override fun onStart() {
        super.onStart()
        dialog?.apply {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            window?.setLayout(width, height)
            setCanceledOnTouchOutside(false)
            setCancelable(false)
            val lp: WindowManager.LayoutParams? = window?.attributes
            lp?.dimAmount = 0.9f
            window?.attributes = lp
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), R.style.full_dialog) {
            override fun onBackPressed() {
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.certificate_download_fragment,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes.windowAnimations = R.style.DialogAnimation
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (certificateUrl.isEmpty()) {
            dismissAllowingStateLoss()
        }
        if (PermissionUtils.isStoragePermissionEnabled(requireContext()).not()) {
            askStoragePermission()
            return
        }
        initDownload()
    }

    private fun askStoragePermission() {
        PermissionUtils.storageReadAndWritePermission(
            requireContext(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            initDownload()
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(requireActivity())
                            dismissAllowingStateLoss()
                            return
                        }
                        return
                    }
                    report?.isAnyPermissionPermanentlyDenied?.let {
                        PermissionUtils.permissionPermanentlyDeniedDialog(requireActivity())
                        dismissAllowingStateLoss()
                        return
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }
        )
    }

    private fun initDownload() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fileDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
                val destination = File(
                    fileDir + File.separator + Utils.getFileNameFromURL(certificateUrl)
                )
                destination.createNewFile()
                addDownload(certificateUrl, destination.absolutePath)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    private fun addDownload(source: String, destination: String) {
        val request = Request(source, destination)
        request.priority = Priority.HIGH
        request.networkType = NetworkType.ALL
        request.tag = Random(5).nextInt().toString()
        fetch.remove(request.id)
        fetch.enqueue(
            request,
            {
                Timber.tag(TAG).e("Request   " + it.file + "  " + it.url)
            },
            {
                Timber.tag(TAG).e("error  ")
                it.throwable?.printStackTrace()
            }
        ).awaitFinishOrTimeout(60_000)
    }

    override fun onAdded(download: Download) {
        Timber.tag(TAG).e("onAdded     " + download.tag)
    }

    override fun onCancelled(download: Download) {
        Timber.tag(TAG).e("onCancelled     " + download.tag)
    }

    override fun onCompleted(download: Download) {
        Timber.tag(TAG).e("onCompleted     " + download.tag)
        // updateDownloadStatus(download.file, download.extras, download.tag)
        dismissAllowingStateLoss()
        try {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.setDataAndType(download.fileUri, "application/pdf")
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            // no Activity to handle this kind of files
        }
    }

    override fun onDeleted(download: Download) {
        Timber.tag(TAG).e("onDeleted     " + download.tag)
    }

    override fun onDownloadBlockUpdated(
        download: Download,
        downloadBlock: DownloadBlock,
        totalBlocks: Int
    ) {
        Timber.tag(TAG).e("onDownloadBlockUpdated     " + download.tag)
    }

    override fun onError(download: Download, error: Error, throwable: Throwable?) {
        Timber.tag(TAG).e("onError     " + download.tag)
    }

    override fun onPaused(download: Download) {
        Timber.tag(TAG).e("onPaused     " + download.tag)
    }

    override fun onProgress(
        download: Download,
        etaInMilliSeconds: Long,
        downloadedBytesPerSecond: Long
    ) {
        Timber.tag(TAG).e("onProgress     " + download.tag)
    }

    override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
        Timber.tag(TAG).e("onQueued     " + download.tag)
    }

    override fun onRemoved(download: Download) {
        Timber.tag(TAG).e("onRemoved     " + download.tag)
    }

    override fun onResumed(download: Download) {
        Timber.tag(TAG).e("onResumed     " + download.tag)
    }

    override fun onStarted(
        download: Download,
        downloadBlocks: List<DownloadBlock>,
        totalBlocks: Int
    ) {
        Timber.tag(TAG).e("onStarted     " + download.tag)
    }

    override fun onWaitingNetwork(download: Download) {
        Timber.tag(TAG).e("onWaitingNetwork     " + download.tag)
    }

    companion object {
        @JvmStatic
        private fun newInstance(certificateUrl: String) =
            CertificateDownloadDialog().apply {
                arguments = Bundle().apply {
                    putString(CERTIFICATE_URL, certificateUrl)
                }
            }

        @JvmStatic
        fun showDownloadCertificateDialog(
            fragmentManager: FragmentManager,
            certificateUrl: String
        ) {
            val prev =
                fragmentManager.findFragmentByTag(CertificateDownloadDialog::class.java.name)
            if (prev != null) {
                return
            }
            newInstance(certificateUrl).show(
                fragmentManager,
                CertificateDownloadDialog::class.java.name
            )
        }
    }
}
