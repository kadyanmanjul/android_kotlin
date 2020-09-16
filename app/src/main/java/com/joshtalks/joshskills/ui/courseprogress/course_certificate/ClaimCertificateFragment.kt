package com.joshtalks.joshskills.ui.courseprogress.course_certificate

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.interfaces.OnDismissClaimCertificateDialog
import com.joshtalks.joshskills.core.service.CONVERSATION_ID
import com.joshtalks.joshskills.databinding.FragmentClaimCertificateBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.CertificateDetail
import com.joshtalks.joshskills.repository.server.RequestCertificateGenerate
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val CERTIFICATE_DETAIL_OBJ = "certificate_detail"

class ClaimCertificateFragment : DialogFragment() {

    private lateinit var binding: FragmentClaimCertificateBinding
    private var compositeDisposable = CompositeDisposable()
    private var certificateDetail: CertificateDetail? = null
    private var conversationId: String = EMPTY
    private var downloadID: Long = -1
    private var listener: OnDismissClaimCertificateDialog? = null
    private lateinit var appAnalytics: AppAnalytics

    private var onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID == id) {
                appAnalytics.addParam(AnalyticsEvent.DOWNLOAD_CERTIFICATE.NAME, "Completed")
                binding.tvSuccessMessage.text = getString(R.string.downloading_complete)
                AppObjectController.uiHandler.postDelayed({
                    dismissAllowingStateLoss()
                }, 4500)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            certificateDetail = it.getParcelable(CERTIFICATE_DETAIL_OBJ)
            conversationId = it.getString(CONVERSATION_ID, EMPTY)
        }
        if (certificateDetail == null) {
            dismissAllowingStateLoss()
        }
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
        listener = requireActivity() as OnDismissClaimCertificateDialog
        appAnalytics = AppAnalytics.create(AnalyticsEvent.CLAIM_CERTIFICATE.NAME)
            .addUserDetails()
            .addBasicParam()
    }


    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            dialog.window?.setLayout(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            dialog.setCanceledOnTouchOutside(true)
            dialog.setCancelable(true)
            dialog.window?.setDimAmount(0.9F)
            dialog.window?.setBackgroundDrawableResource(android.R.color.white)
        }
    }

    override fun onStop() {
        appAnalytics.push()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
        try {
            requireContext().unregisterReceiver(onDownloadComplete)
        } catch (ex: Exception) {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_claim_certificate,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.downloadProgress.indeterminateDrawable.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                R.color.button_color
            ), android.graphics.PorterDuff.Mode.SRC_IN
        )
        val url = AppObjectController.getFirebaseRemoteConfig().getString("CERTIFICATE_URL")
        setCertificateUrl(url)
        if (certificateDetail?.name.isNullOrEmpty().not()) {
            binding.etName.setText(certificateDetail?.name ?: "")
            binding.etName.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.controls_panel_stroke
                )
            )
            disableView(binding.etName)
        }

        if (certificateDetail?.email.isNullOrEmpty().not()) {
            binding.etEmail.setText(certificateDetail?.email ?: "")
            binding.etEmail.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.controls_panel_stroke
                )
            )
            disableView(binding.etEmail)
        }
        if (certificateDetail?.url.isNullOrEmpty().not()) {
            binding.claimDownloadCertBtn.text = getText(R.string.download_certificate)
        }
        if (certificateDetail?.name.isNullOrEmpty()
                .not() && certificateDetail?.email.isNullOrEmpty()
                .not() && certificateDetail?.url.isNullOrEmpty()
        ) {
            binding.claimDownloadCertBtn.visibility = View.GONE
        }
    }

    private fun setCertificateUrl(url: String?) {
        Glide.with(AppObjectController.joshApplication)
            .load(url)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .override(Target.SIZE_ORIGINAL)
            .into(binding.imageView)
    }

    private fun disableView(view: View) {
        view.isFocusableInTouchMode = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.focusable = View.NOT_FOCUSABLE
        }
        view.isEnabled = true
        view.setOnClickListener {
            showToast(getString(R.string.details_not_change_message))
        }
    }

    fun onCertificateAction() {

        if (certificateDetail?.url.isNullOrEmpty()) {
            if (binding.etName.text.isNullOrEmpty()) {
                showToast(getString(R.string.please_enter_name))
                return
            }
            if (binding.etEmail.text.isNullOrEmpty() || Patterns.EMAIL_ADDRESS.matcher(binding.etEmail.text.toString())
                    .matches().not()
            ) {
                showToast(getString(R.string.please_enter_email))
                return
            }
            hideKeyboard(requireActivity(), binding.etEmail)
            binding.progressFl.visibility = View.VISIBLE
            requestForGenerateCertificate(
                binding.etName.text.toString(),
                binding.etEmail.text.toString()
            )
        } else {
            PermissionUtils.storageReadAndWritePermission(requireActivity(),
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                appAnalytics.addParam(
                                    AnalyticsEvent.DOWNLOAD_CERTIFICATE.NAME,
                                    "Clicked"
                                )
                                downloadDigitalCopy(certificateDetail?.url!!)
                                return
                            }
                            if (report.isAnyPermissionPermanentlyDenied) {
                                PermissionUtils.permissionPermanentlyDeniedDialog(requireActivity())
                                return
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

    private fun showDialog() {
        binding.progressFl.visibility = View.VISIBLE
        val scaleAnimation = ScaleAnimation(
            0f,
            1f,
            0f,
            1f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        scaleAnimation.interpolator = AccelerateDecelerateInterpolator()
        scaleAnimation.duration = 1000
        scaleAnimation.fillAfter = true

        scaleAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                binding.tvCongrats.visibility = View.VISIBLE
                binding.tvSuccessMessage.visibility = View.VISIBLE
                AppObjectController.uiHandler.postDelayed({
                    dismissAllowingStateLoss()
                }, 4500)
            }

            override fun onAnimationStart(animation: Animation?) {
                binding.progressBar.visibility = View.GONE
            }

        })
        binding.successIv.startAnimation(scaleAnimation)
    }

    private fun requestForGenerateCertificate(name: String, email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = AppObjectController.commonNetworkService.certificateGenerate(
                    RequestCertificateGenerate(
                        Mentor.getInstance().getId(),
                        conversationId,
                        name,
                        email
                    )
                )
                if (response.isSuccessful) {
                    if (certificateDetail == null || certificateDetail?.name.isNullOrEmpty()) {
                        certificateDetail = response.body()
                        listener?.onDismiss(certificateDetail)
                    }
                    delay(1000)
                    CoroutineScope(Dispatchers.Main).launch {
                        appAnalytics.addParam(AnalyticsEvent.GENERATE_CERTIFICATE.NAME, "Success")
                        appAnalytics.addParam("NAME", name)
                        appAnalytics.addParam("Email", email)
                        showDialog()
                    }
                    return@launch
                } else {
                    appAnalytics.addParam(AnalyticsEvent.GENERATE_CERTIFICATE.NAME, "Failed")
                    appAnalytics.addParam("NAME", name)
                    appAnalytics.addParam("Email", email)
                    showToast(getString(R.string.generic_message_for_error))
                }
            } catch (ex: Exception) {
                appAnalytics.addParam(AnalyticsEvent.GENERATE_CERTIFICATE.NAME, "Exception Occured")
                appAnalytics.addParam("NAME", name)
                appAnalytics.addParam("Email", email)
                showToast(getString(R.string.generic_message_for_error))
                ex.printStackTrace()
            }
            hideDialog()
        }
    }

    private fun hideDialog() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.progressFl.visibility = View.GONE
        }
    }

    override fun dismissAllowingStateLoss() {
        appAnalytics.addParam("BackButton pressed", "backButton pressed")
        super.dismissAllowingStateLoss()
    }

    companion object {
        @JvmStatic
        fun newInstance(conversationId: String, certificateDetail: CertificateDetail) =
            ClaimCertificateFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(CONVERSATION_ID, conversationId)
                        putParcelable(CERTIFICATE_DETAIL_OBJ, certificateDetail)
                    }
                }
    }


    private fun downloadDigitalCopy(url: String) {
        registerDownloadReceiver()
        var fileName = Utils.getFileNameFromURL(url)
        if (fileName.isEmpty()) {
            certificateDetail?.name?.run {
                fileName = this + "_certificate.pdf"
            }
        }
        val request: DownloadManager.Request =
            DownloadManager.Request(Uri.parse(url))
                .setTitle("Josh Talks")
                .setDescription("Downloading certificate")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            request.setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
        }

        val downloadManager =
            requireContext().getSystemService(DOWNLOAD_SERVICE) as (DownloadManager)
        downloadID = downloadManager.enqueue(request)
        binding.progressBar.visibility = View.GONE
        binding.progressFl.visibility = View.VISIBLE
        binding.downloadProgress.visibility = View.VISIBLE
        binding.tvSuccessMessage.text = getString(R.string.downloading_certificate)
        binding.tvSuccessMessage.visibility = View.VISIBLE
    }


    private fun registerDownloadReceiver() {
        requireContext().registerReceiver(
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

}