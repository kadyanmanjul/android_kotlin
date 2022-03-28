package com.joshtalks.joshskills.ui.userprofile

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.databinding.ActivityShareFromProfileBinding
import com.joshtalks.joshskills.ui.referral.*
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Defines
import io.branch.referral.util.LinkProperties
import kotlinx.android.synthetic.main.activity_gif.view.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception

const val WHATSAPP_PACKAGE_STRING = "com.whatsapp"

class ShareFromProfile : AppCompatActivity() {
    private val binding by lazy<ActivityShareFromProfileBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_share_from_profile)
    }

    private val viewModel: ShareFromProfileViewModel by lazy {
        ViewModelProvider(this).get(ShareFromProfileViewModel::class.java)
    }
    private var referralCount: Int = 0

    private var userReferralCode: String = EMPTY

    companion object{
        fun startShareFromProfile(
            activity: Activity,
            viewerReferral: Int
        ){
            Intent(activity, ShareFromProfile::class.java).apply{
                putExtra(VIEWER_REFERRAL, viewerReferral)
            }.run {
                activity.startActivity(this)
            }
        }

        const val VIEWER_REFERRAL= "viewerReferral"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.lifecycleOwner = this
        binding.handler = this

        referralCount = intent.getIntExtra(VIEWER_REFERRAL, 0)
        if(referralCount != 0){
            binding.helpTv.text = getString(R.string.referCntText, referralCount.toString())
            var substr:String = referralCount.toString() + " people"
            binding.helpTv.setColorize(substr)
        }else{
            binding.helpTv.text = getString(R.string.noReferral)
        }
    }

    fun TextView.setColorize(subStringToColorize: String) {
        val spannable: Spannable = SpannableString(text)
        spannable.setSpan(
            ForegroundColorSpan( Color.parseColor("#E58638")),
            16,
            16 + subStringToColorize.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        setText(spannable, TextView.BufferType.SPANNABLE)
    }


    fun shareWithFriends(){
        getDeepLinkAndInviteFriends(WHATSAPP_PACKAGE_STRING)
    }

    fun getDeepLinkAndInviteFriends(packageString: String? = null) {
        val referralTimestamp = System.currentTimeMillis()
        val branchUniversalObject = BranchUniversalObject()
            .setCanonicalIdentifier(userReferralCode.plus(referralTimestamp))
            .setTitle("Invite Friend")
            .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
            .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
        val lp = LinkProperties()
            .setChannel(userReferralCode)
            .setFeature("sharing")
            .setCampaign(userReferralCode.plus(referralTimestamp))
            .addControlParameter(Defines.Jsonkey.ReferralCode.key, userReferralCode)
            .addControlParameter(
                Defines.Jsonkey.UTMCampaign.key,
                userReferralCode.plus(referralTimestamp)
            )
            .addControlParameter(Defines.Jsonkey.UTMMedium.key, "referral")

        branchUniversalObject
            .generateShortUrl(this, lp) { url, error ->
                if (error == null)
                    inviteFriends(
                        packageString = packageString,
                        dynamicLink = url
                    )
                else
                    inviteFriends(
                        packageString = packageString,
                        dynamicLink = if (PrefManager.hasKey(USER_SHARE_SHORT_URL))
                            PrefManager.getStringValue(USER_SHARE_SHORT_URL)
                        else
                            getAppShareUrl()
                    )
            }
    }

    fun inviteFriends(packageString: String? = null, dynamicLink: String) {
        try {
            viewModel.getDeepLink(
                dynamicLink,
                userReferralCode.plus(System.currentTimeMillis())
            )

            val waIntent = Intent(Intent.ACTION_SEND)
            waIntent.type = "image/*"
            if (packageString.isNullOrEmpty().not()) {
                waIntent.setPackage(packageString)
            }

            waIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            waIntent.putExtra(Intent.EXTRA_TEXT, "Mai har roz angrezi mei baat karke angrezi seekh rha hu. Mai chahta hu aap bhi mere saath angrezi seekhe. Is link ko click karke yeh app download kare -\n" + dynamicLink)
            waIntent.type = "text/plain"
            startActivity(Intent.createChooser(waIntent, "Share with"))
            viewModel.postGoal(GoalKeys.HELP_COUNT.name, CampaignKeys.PEOPLE_HELP_COUNT.name)

        } catch (e: PackageManager.NameNotFoundException) {
            showToast(getString(R.string.whatsApp_not_installed))
        }
    }

    private fun getAppShareUrl(): String {
        return "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "&referrer=utm_source%3D$userReferralCode"
    }

}