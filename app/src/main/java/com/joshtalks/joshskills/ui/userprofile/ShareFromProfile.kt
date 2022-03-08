package com.joshtalks.joshskills.ui.userprofile

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.text.Html
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.ActivityShareFromProfileBinding
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.referral.*
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Defines
import io.branch.referral.util.LinkProperties
import kotlinx.android.synthetic.main.activity_gif.view.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception

//lateinit var referralCount: String
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

        //referralCount = intent.getIntExtra(VIEWER_REFERRAL, 0)!!
        referralCount = 5
        if(referralCount != 0){
            //binding.helpTv.text = "You have helped" + referralCount + "people start learning English"
            binding.helpTv.text = "You have helped" + Html.fromHtml("<font color='#E58638'><b>referralCount<b></font>") + "people start learning English"
        }else{
            binding.helpTv.text = "You have not helped anyone start learning English yet"
        }
//        binding.helpTv.text = referralCount

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

            val imageFile: File = getImageFile()
            val imagePath: String = imageFile.path
            val uri = Uri.parse("file://$imagePath")

            val waIntent = Intent(Intent.ACTION_SEND)
            waIntent.type = "image/*"
            if (packageString.isNullOrEmpty().not()) {
                waIntent.setPackage(packageString)
            }

            waIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            waIntent.putExtra(Intent.EXTRA_STREAM, uri)
            waIntent.putExtra(Intent.EXTRA_TEXT, "मैं English सीख रहा हूँ ,जहाँ enviroment मिलता है बेझिझक नए लोगों से बात करने का !तुम भी सीख सकते हो.\n" +
                    "Link :\n" + dynamicLink)
            waIntent.type = "image/*"
            startActivity(Intent.createChooser(waIntent, "Share with"))

        } catch (e: PackageManager.NameNotFoundException) {
            showToast(getString(R.string.whatsApp_not_installed))
        }
    }

    private fun getAppShareUrl(): String {
        return "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "&referrer=utm_source%3D$userReferralCode"
    }

    private fun getImageFile(): File{
        val bitmap = BitmapFactory.decodeResource(
            resources, R.drawable.for_testing
        )
        val path: String =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString() + "/Share.jpg"
        var out: OutputStream? = null
        val file = File(path)

        try {
            out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return file
    }
}