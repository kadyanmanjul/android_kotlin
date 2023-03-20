package com.joshtalks.joshskills.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.joshtalks.joshskills.core.INACTIVE_DAYS
import com.joshtalks.joshskills.core.TOTAL_LOCAL_NOTIFICATIONS_SHOWN
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.LogException.catchException
import com.joshtalks.joshskills.core.notification.client_side.LocalAlarmUtils
import com.joshtalks.joshskills.repository.local.model.NotificationAction
import com.joshtalks.joshskills.repository.local.model.NotificationObject
import com.joshtalks.joshskills.repository.local.model.User
import kotlinx.coroutines.*

class
ScheduledLocalNotificationReceiver : BroadcastReceiver() {

    val NOTIFICATION_TITLE = arrayOf(
        "_username_, आपने कल की practice miss करदी." ,
        "_username_, You did not complete your lesson yesterday.",
        "_username_, We missed you yesterday.",
        "अपना promise पूरा करो!" ,
        "No No No. Missing classes is unacceptable.",
        "6 new english words are due from yesterday.",
        "Practice makes perfect.",
        "_username_, आपने कल की practice miss कर दी। ",
        "What happened to your promise?",
        "Every single day _username_. Every single day.",
        "Consistency का मतलब रोज़ आना होता है _username_.",
        "_username_, आपने कल class क्यों miss कर दी।",
        "_username_, use your phone time wisely.",
        "Learning 16 words are due since last _inactive_ days.",
        "Gotcha _username_!!",
        "आपने कहा था आप रोज़ आएंगे ",
        "आज फिर से class miss?  Unacceptable.",
        "Just 1 call. इतना तो हम कर ही सकते है।",
        "Daily. यह शब्द याद रखना।  हम daily practice करेंगे।",
        "Learning requires everyday practice.",
        "There is no excuse for not trying.",
        "_username_ ऐसा ही करना था तो course join ही क्यों किया?",
        "I am disappointed _username_!",
        "You are not serious! ",
        "The key to success is consistency.",
        "रोज़ाना ! हम रोज़ आ के practice करेंगे English की।",
        "_username_, Do your promises mean nothing?",
        "_username_, you have missed class _inactive_ days in a row.",
        "_username_, it's been _inactive_ days since you last practiced English.",
        "आपके promise का क्या हुआ?" ,
        "तुम्हारे पीछे बाघ बाघ के तक गया  हूँ। ",
        "Bas enough. I'm so tired.",
        "मैंने तुमसे तुम्हारी ज़िन्दगी के सिर्फ तीन ही महीने मांगे थे। ",
        "_username_, you have missed practice _inactive_ days in a row.",
        "_username_, it's been _inactive_ days since you last practiced English.",
        "Hey _username_, It's been _inactive_ days.",
        "Someone busier than you is practicing right now.",
        "If you are determined to learn, Nothing can stop you.",
        "Hey _username_, It's been _inactive_ days",
        "_username_, I haven't seen you in _inactive_ days.",
        "_username_, you skipped English practice for the _inactive_ th time in a row.",
        "_username_, you have missed _inactive_ days in a row",
        "Someone busier than you is practicing right now."

    )
    val NOTIFICATION_DESC = arrayOf(
        "Learnig English requires daily practice. Practice Today.",
        "Is everything Okay?",
        "Don't skip practice today.",
        "Is everything okay?",
        "मुझे लगा था आप कोर्स को लेकर serious हैं। आप एक भी दिन miss नहीं कर सकते।",
        "Come back today and practice English.",
        "Keep up! Practice Today!",
        "Do u want to perfect English? Practice Today.",
        "Is everything okay?",
        "मुझे लगा था आप कोर्स को लेकर serious हैं। आप एक भी दिन miss नहीं कर सकते। ",
        "We cannot even miss one day. Practice Today.",
        "Come let's practice today.",
        "Do not forget your promise. Practice today.",
        "Have a quick English practice now.",
        "Do not let your work pile up. Practice Today",
        "You skipped _inactive_ days of practice. Come back!",
        "हम जितने भी busy हैं, हम तब भी 10 minute निकाल सकते हैं अपना promise पूरा करने के लिए।",
        "Practice today without fail.",
        "हम life में बहुत busy हैं तो भी इतना हम कर सकते हैं। ",
        "एक भी दिन miss करना unacceptable है।  Practice today.",
        "There are no shortcuts. Practice today",
        "Practice English today for 10 minutes only.",
        "ऐसा ही करना था तो course join ही क्यों किया",
        "आपने कहा था आप रोज़ आओगे। हम कितने भी busy है हम अपना promise नहीं तोड़ सकते",
        "Come back and practice today.",
        "सिर्फ एक call ही क्यों नहीं, लेकिन हमे रोज़ आना है।",
        "Come back today and practice English.",
        "This is not done. Come back and practice today.",
        "This is unacceptable. Practice Today.",
        "हम जितने भी busy हैं, हम तब भी 20 minute निकाल सकते हैं अपना promise पूरा करने के लिए",
        "शायद मैं हार मानने लगा हूँ। ",
        "I cannot keep running after you like this. ",
        "वो भी नहीं दे सकते क्या तुम मुझे ?",
        "Is everything okay? Come back and practice English.",
        "Let's fix that?",
        "Complete your lesson from.",
        "What's stopping you? Practice now.",
        "क्या आप determined हो?? Practice Today.",
        "Complete your lesson ",
        "This is not done, Practice English now!!",
        "Come back and Practice now.",
        "You have a lot to catch up. Let's Start?",
        "What's stopping you? Practice now"
    )

    override fun onReceive(context: Context?, intent: Intent?) {

        if ("android.intent.action.BOOT_COMPLETED" == intent?.action && context!=null) {
            CoroutineScope(Dispatchers.IO).launch {
                LocalAlarmUtils.removeLocalNotifications(context)
                LocalAlarmUtils.scheduleNotifications(context,60*60*1000*1)
            }
        } else {
            try {
                val notificationId = intent?.extras?.getString("id", "Local_notification")
                context?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        val inactiveDays = PrefManager.getIntValue(INACTIVE_DAYS, false, 0).plus(1)
                        val totalNotifications =
                            PrefManager.getIntValue(TOTAL_LOCAL_NOTIFICATIONS_SHOWN, false, 0).plus(1)
                        PrefManager.put(INACTIVE_DAYS, inactiveDays)
                        PrefManager.put(TOTAL_LOCAL_NOTIFICATIONS_SHOWN, totalNotifications)
                        val title =
                            NOTIFICATION_TITLE.get(totalNotifications % (NOTIFICATION_TITLE.size))
                                .replace("_inactive_", inactiveDays.toString())
                                .replace("_username_", User.getInstance().firstName.toString())


                        val desc =
                            NOTIFICATION_DESC.get(totalNotifications % (NOTIFICATION_DESC.size))
                                .replace("_inactive_", inactiveDays.toString())
                                .replace("_username_", User.getInstance().firstName.toString())

                        NotificationUtils(it).sendNotification(NotificationObject().apply {
                            id = notificationId
                            contentTitle = title
                            contentText = desc
                            action = NotificationAction.OPEN_APP
                            actionData = null
                        })
                    }
                }
            }catch (ex:Exception){
                catchException(ex)
            }
        }
    }
}