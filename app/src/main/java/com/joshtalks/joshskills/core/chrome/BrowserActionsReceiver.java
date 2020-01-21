package com.joshtalks.joshskills.core.chrome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * A {@link BroadcastReceiver} that handles the callback if default menu items are chosen from
 * Browser Actions.
 */
public class BrowserActionsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String toastMsg = "Chosen item Id: " + intent.getDataString();
        // Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show();
        Log.e("BrowserActionsReceiver", toastMsg);
    }
}