package com.joshtalks.joshskills.core.service

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log


class NotificationListener : NotificationListenerService() {
    private var nlservicereciver: NLServiceReceiver? = null
    protected val TAG = javaClass.canonicalName

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG,"ONCREATE")
        nlservicereciver = NLServiceReceiver()

        val filter = IntentFilter()
        filter.addAction("com.joshtalks.joshskills.core.service.NOTIFICATION_LISTENER_SERVICE_EXAMPLE")
        registerReceiver(nlservicereciver, filter)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        Log.e(TAG,"onNotificationPosted")

        super.onNotificationPosted(sbn, rankingMap)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        Log.e(TAG,"onNotificationRemoved")

        super.onNotificationRemoved(sbn, rankingMap)
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int
    ) {
        super.onNotificationRemoved(sbn, rankingMap, reason)
    }

    override fun onNotificationChannelGroupModified(
        pkg: String?,
        user: UserHandle?,
        group: NotificationChannelGroup?,
        modificationType: Int
    ) {
        super.onNotificationChannelGroupModified(pkg, user, group, modificationType)
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) {
        super.onNotificationRankingUpdate(rankingMap)
    }

    override fun onInterruptionFilterChanged(interruptionFilter: Int) {
        super.onInterruptionFilterChanged(interruptionFilter)
    }

    override fun onListenerHintsChanged(hints: Int) {
        super.onListenerHintsChanged(hints)
    }

    override fun onListenerConnected() {
        Log.e(TAG,"onListenerConnected")

        super.onListenerConnected()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onNotificationChannelModified(
        pkg: String?,
        user: UserHandle?,
        channel: NotificationChannel?,
        modificationType: Int
    ) {
        super.onNotificationChannelModified(pkg, user, channel, modificationType)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
    }

    override fun getActiveNotifications(): Array<StatusBarNotification> {
        return super.getActiveNotifications()
    }

    override fun getActiveNotifications(keys: Array<out String>?): Array<StatusBarNotification> {
        return super.getActiveNotifications(keys)
    }

    override fun getCurrentRanking(): RankingMap {
        return super.getCurrentRanking()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(nlservicereciver)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.e(TAG, "**********  onNotificationPosted")
        Log.e(
            TAG,
            "ID :" + sbn.getId().toString() + "\t" + sbn.getNotification().tickerText.toString() + "\t" + sbn.getPackageName()
        )
        val i = Intent("com.kpbird.nlsexample.NOTIFICATION_LISTENER_EXAMPLE")
        i.putExtra(
            "notification_event",
            "onNotificationPosted :" + sbn.getPackageName().toString() + "\n"
        )
       // sendBroadcast(i)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.e(TAG, "********** onNOtificationRemoved")
        Log.e(
            TAG,
            "ID :" + sbn.getId().toString() + "\t" + sbn.getNotification().tickerText.toString() + "\t" + sbn.getPackageName()
        )
        val i = Intent("com.kpbird.nlsexample.NOTIFICATION_LISTENER_EXAMPLE")
        i.putExtra(
            "notification_event",
            "onNotificationRemoved :" + sbn.getPackageName().toString() + "\n"
        )
      //  sendBroadcast(i)
    }

    internal inner class NLServiceReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.getStringExtra("command").equals("clearall")) {
               // this@NLService.cancelAllNotifications()
            } else if (intent.getStringExtra("command").equals("list")) {
                val i1 = Intent("com.kpbird.nlsexample.NOTIFICATION_LISTENER_EXAMPLE")
                i1.putExtra("notification_event", "=====================")
                sendBroadcast(i1)
                var i = 1
                for (sbn in this@NotificationListener.activeNotifications) {
                    val i2 = Intent("com.kpbird.nlsexample.NOTIFICATION_LISTENER_EXAMPLE")
                    i2.putExtra(
                        "notification_event",
                        i.toString() + " " + sbn.getPackageName() + "\n"
                    )
                    sendBroadcast(i2)
                    i++
                }
                val i3 = Intent("com.kpbird.nlsexample.NOTIFICATION_LISTENER_EXAMPLE")
                i3.putExtra("notification_event", "===== Notification List ====")
               // sendBroadcast(i3)
            }
        }
    }
}