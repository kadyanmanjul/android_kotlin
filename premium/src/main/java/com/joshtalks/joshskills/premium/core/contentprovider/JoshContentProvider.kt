package com.joshtalks.joshskills.premium.core.contentprovider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.joshtalks.joshskills.premium.BuildConfig
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.base.constants.COURSE_ID
import com.joshtalks.joshskills.base.model.ApiHeader
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.core.io.AppDirectory
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.repository.local.model.User
import com.joshtalks.joshskills.premium.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.utils.isBlocked
import com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.viewmodels.voipLog
import kotlinx.coroutines.sync.Mutex
import kotlin.Exception

private const val TAG = "JoshContentProvider"

class JoshContentProvider : ContentProvider() {
    val mutex = Mutex(false)

    override fun onCreate(): Boolean {
        voipLog?.log("On Create Content Provider $context")
        context?.let { VoipPref.initVoipPref(it) }
        Log.d(TAG, "onCreate: ")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        when (uri.path) {

        }
        return null
    }

    private fun getNotificationTitle(courseId: String, blockedOrFTEnded: Boolean): String {
        val name = Mentor.getInstance().getUser()?.firstName ?: "User"
        if (blockedOrFTEnded) return "${name}, JUST ₹2/DAY !!!"
        return when (courseId) {
            "151", "1214" -> "$name, English बोलने से आती हैं."
            "1203"-> "$name, ইংলিশ প্রাকটিস করলে তবেই বলতে পারবেন।"
            "1206"-> "$name, English ਬੋਲਣ ਨਾਲ ਆਉਂਦੀ ਹੈ।"
            "1207"-> "$name, English बोलल्याने येते."
            "1209"-> "$name, English സംസാരിച്ചു  പഠിക്കാം."
            "1210"-> "$name, பேசினால் தான் ஆங்கிலம் வரும்"
            "1211"-> "$name, English మాట్లాడితేనే వస్తుంది."
            else -> "$name, You will learn English by speaking."
        }
    }

    private fun getNotificationBody(courseId: String, blockedOrFtEnded: Boolean): String {
        if (!blockedOrFtEnded) return "Call now"
        return when(courseId) {
            "151", "1214" -> "Unlimited Calling, जब चाहें, जहाँ चाहें,  जितना चाहें !!!"
            "1203"-> "Unlimited Calling, যে কোন সময় যে কোন জায়গায় যতটা আপনি চান"
            "1206"-> "Unlimited Calling, ਜਦੋਂ ਚਾਹੋ, ਜਿੱਥੇ ਚਾਹੋ, ਜਿਹਨਾਂ ਚਾਹੋ !!!"
            "1207"-> "Unlimited Calling, केव्हाही, कुठेही, पाहिजे तितके!!!"
            "1209"-> "Unlimited Calling, എപ്പോൾ വേണമെങ്കിലും,എവിടെ വേണമെങ്കിലും,എത്ര വേണമെങ്കിലും!!!"
            "1210"-> "Unlimited Calling, எங்கும், எந்நேரத்திலும், அளவில்லாமல்!!!"
            "1211"-> "Unlimited Calling, ఎప్పుడు కావాలన్న, ఎక్కడ కావాలన్న, ఎంత కావాలన్న !!!"
            else -> "Unlimited Calling, Anytime, Anywhere!!!"
        }
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return uri
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0
    }
}