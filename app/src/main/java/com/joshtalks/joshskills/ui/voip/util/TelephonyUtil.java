package com.joshtalks.joshskills.ui.voip.util;

import android.content.Context;
import android.telephony.TelephonyManager;

public class TelephonyUtil {
    private TelephonyUtil() {
    }

    public static TelephonyManager getManager(final Context context) {
        return (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }
}
