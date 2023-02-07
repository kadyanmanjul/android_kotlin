package com.joshtalks.joshskills

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.google.firebase.FirebaseApp

private const val TAG = "Bhaskar"
var toast : Toast? = null

fun Context.showMsg(msg : String) {
    toast?.cancel()
    toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT)
    Log.e(TAG, "showMsg: $msg")
    toast?.show()
}

class MainActivity : AppCompatActivity() {

    val downloadBtn by lazy {
        findViewById<Button>(R.id.btn_download_module)
    }
    val openPremiumBtn by lazy {
        findViewById<Button>(R.id.btn_open_premium)
    }
    val downloadFreeBtn by lazy {
        findViewById<Button>(R.id.btn_download_free_module)
    }
    val openFreeTrialBtn by lazy {
        findViewById<Button>(R.id.btn_open_free_module)
    }

    val splitInstallManager by lazy {
        SplitInstallManagerFactory.create(this)
    }

    private val moduleAssets by lazy { getString(R.string.title_premium) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        splitInstallManager.registerListener(listener)
        downloadBtn.setOnClickListener {
            // Creates a request to install a module.
            val request =
                SplitInstallRequest
                    .newBuilder()
                    // You can download multiple on demand modules per
                    // request by invoking the following method for each
                    // module you want to install.
                    .addModule("premium")
                    .build()

            splitInstallManager
                // Submits the request to install the module through the
                // asynchronous startInstall() task. Your app needs to be
                // in the foreground to submit the request.
                .startInstall(request)
                // You should also be able to gracefully handle
                // request state changes and errors. To learn more, go to
                // the section about how to Monitor the request state.
                .addOnSuccessListener { sessionId ->
                    showMsg("Downloaded --- $sessionId")
                }
                .addOnFailureListener { e ->
                    showMsg("Error while downloading -- ${e.cause.toString()}")
                    e.printStackTrace()
                }

            showMsg("Downloading Started")
        }
        downloadFreeBtn.setOnClickListener {
            // Creates a request to install a module.
            val request =
                SplitInstallRequest
                    .newBuilder()
                    // You can download multiple on demand modules per
                    // request by invoking the following method for each
                    // module you want to install.
                    .addModule("freetrial")
                    .build()

            splitInstallManager
                // Submits the request to install the module through the
                // asynchronous startInstall() task. Your app needs to be
                // in the foreground to submit the request.
                .startInstall(request)
                // You should also be able to gracefully handle
                // request state changes and errors. To learn more, go to
                // the section about how to Monitor the request state.
                .addOnSuccessListener { sessionId ->
                    showMsg("Downloaded --- $sessionId")
                }
                .addOnFailureListener { e ->
                    showMsg("Error while downloading -- ${e.cause.toString()}")
                    e.printStackTrace()
                }

            showMsg("Downloading Started")
        }
        openPremiumBtn.setOnClickListener {
            try {
                val dist = Class.forName("com.joshtalks.joshskills.premium.PremiumMainActivity")
                startActivity(Intent(this, dist))
            } catch (e : Exception) {
                e.printStackTrace()
                showMsg("Failed to Open Launcher - ${e.cause.toString()}")
            }
        }

        openFreeTrialBtn.setOnClickListener {
            try {
                val dist = Class.forName("com.joshtalks.joshskills.freetrial.FreeTrialMainActivity")
                startActivity(Intent(this, dist))
            } catch (e : Exception) {
                e.printStackTrace()
                showMsg("Failed to Open Launcher - ${e.cause.toString()}")
            }
        }
    }

    private val listener = SplitInstallStateUpdatedListener { state ->
        val multiInstall = state.moduleNames().size > 1
        val names = state.moduleNames().joinToString(" - ")
        when (state.status()) {
            SplitInstallSessionStatus.DOWNLOADING -> {
                //  In order to see this, the application has to be uploaded to the Play Store.
                showMsg("Downloading Module")
            }
            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                /*
                  This may occur when attempting to download a sufficiently large module.

                  In order to see this, the application has to be uploaded to the Play Store.
                  Then features can be requested until the confirmation path is triggered.
                 */
                startIntentSender(state.resolutionIntent()?.intentSender, null, 0, 0, 0)
                showMsg("REQUIRES_USER_CONFIRMATION")
            }
            SplitInstallSessionStatus.INSTALLED -> {
                showMsg("INSTALLED")
            }

            SplitInstallSessionStatus.INSTALLING -> showMsg("INSTALLING")
            SplitInstallSessionStatus.FAILED -> {
                showMsg("FAILED")
            }
        }
    }
}