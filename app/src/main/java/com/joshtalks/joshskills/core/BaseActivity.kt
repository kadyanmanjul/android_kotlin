package com.joshtalks.joshskills.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.location.SelectLocationActivity
import com.joshtalks.joshskills.ui.profile.CropImageActivity
import com.joshtalks.joshskills.ui.profile.ProfileActivity
import com.joshtalks.joshskills.ui.profile.SOURCE_IMAGE
import com.joshtalks.joshskills.ui.sign_up_old.OnBoardActivity
import io.github.inflationx.viewpump.ViewPumpContextWrapper


abstract class BaseActivity : AppCompatActivity() {

    protected val TAG = javaClass.canonicalName


    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        AppObjectController.screenHeight = displayMetrics.heightPixels
        AppObjectController.screenWidth = displayMetrics.widthPixels


    }

    fun getIntentForState(): Intent? {

        val intent = when {
            User.getInstance().token == null -> Intent(
                this,
                OnBoardActivity::class.java
            ).apply {
            }
            User.getInstance().dateOfBirth.isNullOrEmpty() -> Intent(
                this,
                ProfileActivity::class.java
            ).apply {
            }
            Mentor.getInstance().getLocality() == null -> Intent(
                this,
                SelectLocationActivity::class.java
            ).apply {
            }
            else -> {
                null
            }
        }
        return intent?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getCroppingActivity(filePath: String): Intent {
        return Intent(this, CropImageActivity::class.java).apply {
            putExtra(SOURCE_IMAGE, filePath)
        }
    }

    fun getInboxActivityIntent(): Intent {
        return Intent(this, InboxActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }


}