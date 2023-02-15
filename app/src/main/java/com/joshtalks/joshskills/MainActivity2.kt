package com.joshtalks.joshskills

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.viewpager.widget.ViewPager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.joshtalks.joshskills.databinding.ActivityMain2Binding
import java.util.*
import kotlin.math.ceil

class MainActivity2 : AppCompatActivity() {
    val binding by lazy<ActivityMain2Binding> {
        DataBindingUtil.setContentView(this, R.layout.activity_main2)
    }

    private val splitInstallManager by lazy {
        SplitInstallManagerFactory.create(this)
    }

    val adapter  = SliderImagesViewPager()
    private var dotsCount = 0
    private lateinit var dots: Array<ImageView?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        splitInstallManager.registerListener(listener)
        setSliderImage()

        binding.btnFirstCall.setOnClickListener {
            showMsg("Work In Progress")
        }

        if(splitInstallManager.installedModules.contains("premium")){
            binding.btnDownloadPremiumFeature.text = "Open Premium App"
        }else{
            binding.btnDownloadPremiumFeature.text = "Download Premium App"
        }

        binding.btnDownloadPremiumFeature.setOnClickListener {
            if(binding.btnDownloadPremiumFeature.text == "Open Premium App"){
                openPremiumFeature()
            }else{
                downloadPremiumFeatureDialog()
            }
        }
    }

    private fun openPremiumFeature() {
        try {
           val dist = Class.forName("com.joshtalks.joshskills.premium.ui.launch.LauncherActivity")
            startActivity(Intent(this, dist))
        } catch (e : Exception) {
           e.printStackTrace()
           showMsg("Failed to Open Launcher - ${e.cause.toString()}")
       }
    }

    private fun setSliderImage(){
        val list = mutableListOf<SliderImage>()
        list.add(SliderImage("https://s3.ap-south-1.amazonaws.com/www.static.skills.com/buy_course_page_content/images/Group+32588.png"))
        list.add(SliderImage("https://s3.ap-south-1.amazonaws.com/www.static.skills.com/buy_course_page_content/images/Group+32589.png"))
        list.add(SliderImage("https://s3.ap-south-1.amazonaws.com/www.static.skills.com/buy_course_page_content/images/Group+32591.png"))
        list.add(SliderImage("https://s3.ap-south-1.amazonaws.com/www.static.skills.com/buy_course_page_content/images/Group+32592.png"))
        list.add(SliderImage("https://s3.ap-south-1.amazonaws.com/www.static.skills.com/buy_course_page_content/images/Group+32593.png"))
        list.add(SliderImage("https://s3.ap-south-1.amazonaws.com/www.static.skills.com/buy_course_page_content/images/Group+27.png"))
        setSlider(SliderImageList(list))
    }

    private fun setSlider(sliderImage: SliderImageList){
        adapter.addListOfImages(sliderImage.images)
        binding.sliderViewPager.adapter = adapter

        dotsCount = (binding.sliderViewPager.adapter as SliderImagesViewPager).count
        dots = arrayOfNulls(dotsCount)

        for (i in 0 until dotsCount) {
            dots[i] = ImageView(this)
            dots[i]!!.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.non_active_dot))
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(8, 0, 8, 0)
            binding.indicator.addView(dots[i], params)
        }

        dots[0]!!.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.active_dot))

        binding.sliderViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                for (i in 0 until dotsCount) {
                    dots[i]!!.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.non_active_dot))
                }
                dots[position]!!.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.active_dot))
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        Timer().schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    var i = binding.sliderViewPager.currentItem
                    if (i == sliderImage.images.size - 1){
                        i=0
                        binding.sliderViewPager.setCurrentItem(i,true)
                    }else{
                        i++
                        binding.sliderViewPager.setCurrentItem(i,true)
                    }
                }
            }
        }, 4000,4000)
    }

    private val listener = SplitInstallStateUpdatedListener { state ->
        val multiInstall = state.moduleNames().size > 1
        val names = state.moduleNames().joinToString(" - ")
        when (state.status()) {
            SplitInstallSessionStatus.DOWNLOADING -> {
                displayLoadingState(state)
               // showMsg("Downloading Module")
            }
            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                startIntentSender(state.resolutionIntent()?.intentSender, null, 0, 0, 0)
                showMsg("REQUIRES_USER_CONFIRMATION")
            }
            SplitInstallSessionStatus.INSTALLED -> {
                displayButtons()
                binding.btnDownloadPremiumFeature.text = "Open Premium App"
                showMsg("INSTALLED")
            }

            SplitInstallSessionStatus.INSTALLING -> {
                //showMsg("INSTALLING")
            }
            SplitInstallSessionStatus.FAILED -> {
                showMsg("FAILED")
            }
            SplitInstallSessionStatus.DOWNLOADED -> {
                displayButtons()
                binding.btnDownloadPremiumFeature.text = "Open Premium App"
                showMsg("ALREADY DOWNLOADED")
            }else -> {

            }
        }
    }

    private fun startPremiumDownload(){
        // Creates a request to install a module.
        val request =
            SplitInstallRequest
                .newBuilder()
                .addModule("premium")
                .build()

        splitInstallManager
            .startInstall(request)
            .addOnSuccessListener { sessionId ->
                showMsg("Downloaded --- $sessionId")
            }
            .addOnFailureListener { e ->
                showMsg("Error while downloading -- ${e.message.toString()}")
                e.printStackTrace()
            }
        showMsg("Downloading Started")
    }

    private fun downloadPremiumFeatureDialog() {
        val dialogView = showCustomDialog(R.layout.ask_download_dialog)
        val btnConfirm = dialogView.findViewById<MaterialTextView>(R.id.yes_button)
        val btnNotNow = dialogView.findViewById<MaterialTextView>(R.id.not_now)
        btnConfirm
            .setOnClickListener {
                startPremiumDownload()
                dialogView.dismiss()
            }
        btnNotNow.setOnClickListener {
            dialogView.dismiss()
        }
    }

    private fun showCustomDialog(view: Int): Dialog {
        val dialogView = Dialog(this)
        dialogView.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogView.setCancelable(false)
        dialogView.setContentView(view)
        dialogView.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.show()
        return dialogView
    }

    private fun displayProgress() {
        binding.welcomeContainer.visibility = View.VISIBLE
        binding.progressCardView.visibility = View.VISIBLE
        binding.btnDownloadPremiumFeature.visibility = View.GONE
    }

    private fun displayButtons() {
        binding.welcomeContainer.visibility = View.GONE
        binding.progressCardView.visibility = View.GONE
        binding.btnDownloadPremiumFeature.visibility = View.VISIBLE
    }

    private fun displayLoadingState(state: SplitInstallSessionState) {
        displayProgress()
        val totalBytes = state.totalBytesToDownload()
        val totalDownloaded = state.bytesDownloaded()
        binding.downloadProgressBar.max = totalBytes.toInt()
        binding.downloadProgressBar.progress = totalDownloaded.toInt()

        try {
            val downloadPercentage = ceil(((1.0 * totalDownloaded) / totalBytes) * 100).toInt()
            binding.downloadPercent.text = "$downloadPercentage%"
        } catch (e: Exception) {
            showMsg(e.message.toString())
            e.printStackTrace()
        }
    }

    override fun onPause() {
        // Make sure to dispose of the listener once it's no longer needed.
        splitInstallManager.unregisterListener(listener)
        super.onPause()
    }

}