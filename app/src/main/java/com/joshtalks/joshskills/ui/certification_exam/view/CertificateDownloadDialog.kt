package com.joshtalks.joshskills.ui.certification_exam.view

import android.app.Dialog
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
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.interfaces.FileDownloadCallback
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

const val CANCELABLE = "cancelable"

class CertificateDownloadDialog : DialogFragment(), FetchListener {

    private lateinit var binding: CertificateDownloadFragmentBinding
    private var certificateUrl: String = EMPTY
    private var mCancelable: Boolean = true

    private val fetch = AppObjectController.getFetchObject()
    private val TAG = CertificateDownloadDialog::class.java.simpleName
    private var listener: FileDownloadCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listener = requireActivity() as FileDownloadCallback
        arguments?.getString(CERTIFICATE_URL)?.run {
            certificateUrl = this
        }
        arguments?.getBoolean(CANCELABLE)?.run {
            mCancelable = this
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
            return
        }
        setupDownload()
    }

    fun updateDownloadUrl(certificateUrl: String) {
        this.certificateUrl = certificateUrl
        setupDownload()
    }

    private fun setupDownload() {
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
                            errorDismiss()
                            return
                        }
                        return
                    }
                    report?.isAnyPermissionPermanentlyDenied?.let {
                        PermissionUtils.permissionPermanentlyDeniedDialog(requireActivity())
                        errorDismiss()
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

                val fileDir = requireContext().getExternalFilesDir( Environment.DIRECTORY_DOWNLOADS)?.absolutePath
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
                errorDismiss()
                Timber.tag(TAG).e("error  ")
                it.throwable?.printStackTrace()
            }
        ).awaitFinishOrTimeout(120_000)
    }

    override fun onAdded(download: Download) {
        Timber.tag(TAG).e("onAdded     " + download.tag)
    }

    override fun onCancelled(download: Download) {
        Timber.tag(TAG).e("onCancelled     " + download.tag)
    }

    override fun onCompleted(download: Download) {
        Timber.tag(TAG).e("onCompleted     " + download.tag)
        listener?.onSuccessDismiss()
        if (mCancelable) {
            dismissAllowingStateLoss()
        }
        listener?.downloadedFile(download.file)
        listener?.webURL(certificateUrl, download.file)
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
        errorDismiss()
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

    fun errorDismiss() {
        dismissAllowingStateLoss()
        listener?.onCancel()
    }

    companion object {
        @JvmStatic
        private fun newInstance(certificateUrl: String, cancelable: Boolean = true) =
            CertificateDownloadDialog().apply {
                arguments = Bundle().apply {
                    putString(CERTIFICATE_URL, certificateUrl)
                    putBoolean(CANCELABLE, cancelable)
                }
            }

        @JvmStatic
        fun showDownloadCertificateDialog(
            fragmentManager: FragmentManager,
            certificateUrl: String,
            cancelable: Boolean = true
        ) {
            val prev =
                fragmentManager.findFragmentByTag(CertificateDownloadDialog::class.java.name)
            if (prev != null) {
                return
            }
            newInstance(certificateUrl, cancelable).show(
                fragmentManager,
                CertificateDownloadDialog::class.java.name
            )
        }

        fun hideProgressBar(activity: FragmentActivity) {
            try {
                val dialog =
                    activity.supportFragmentManager.findFragmentByTag(CertificateDownloadDialog::class.java.name)
                if (dialog != null && dialog is CertificateDownloadDialog) {
                    dialog.dismiss()
                }
            } catch (ignored: Exception) {
                ignored.printStackTrace()
            }
        }
    }
}
