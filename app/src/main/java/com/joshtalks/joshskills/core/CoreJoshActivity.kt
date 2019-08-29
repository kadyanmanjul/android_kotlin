package com.joshtalks.joshskills.core

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.location.SelectLocationActivity
import com.joshtalks.joshskills.ui.profile.CropImageActivity
import com.joshtalks.joshskills.ui.profile.ProfileActivity
import com.joshtalks.joshskills.ui.profile.SOURCE_IMAGE
import com.joshtalks.joshskills.ui.sign_up_old.OnBoardActivity
import io.github.inflationx.viewpump.ViewPumpContextWrapper

open class CoreJoshActivity : AppCompatActivity() {

    protected val TAG = javaClass.canonicalName

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { ViewPumpContextWrapper.wrap(it) })
    }


    companion object {
        var context: Context = AppObjectController.joshApplication

        fun getConfigIntent(): Intent {

            var intent = when {
                User.getInstance().token==null -> Intent(
                    context,
                    OnBoardActivity::class.java
                ).apply {
                }
                User.getInstance().dateOfBirth.isEmpty() -> Intent(
                    context,
                    ProfileActivity::class.java
                ).apply {
                }
                Mentor.getInstance().getLocality() == null -> Intent(
                    context,
                    SelectLocationActivity::class.java
                ).apply {
                }
                else -> {
                    Intent(context, InboxActivity::class.java)
                }
            }
            return intent.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        fun getCroppingActivity(filePath:String): Intent {
            return Intent(context,CropImageActivity::class.java).apply {
                putExtra(SOURCE_IMAGE,filePath)
            }
        }

    }



}


enum class ProfileStep {
    DATE_OF_BIRTH, LANGUAGE, INTEREST, LOCALITY
}