package com.joshtalks.joshskills.premium.util;

import android.text.format.DateFormat;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class Utils {
    public static String DATE_FORMAT = "yyyyMMdd_HHmmsyyyyMMdd_HHmmss";

    public static String getDateId(long var0) {
        Calendar var2 = Calendar.getInstance(Locale.ENGLISH);
        var2.setTimeInMillis(var0);
        return DateFormat.format("ddMMyyyy", var2).toString();
    }

    public static String getDate(long var0) {
        Calendar var2 = Calendar.getInstance(Locale.ENGLISH);
        var2.setTimeInMillis(var0);
        return DateFormat.format("dd/MM/yyyy", var2).toString();
    }

    public static Map<String, String> jsonToMap(JSONObject json) {
        if (json == null || json.length() == 0) return new HashMap<>();
        Map<String, String> map = new HashMap<>(json.length());
        Iterator<String> iterator = json.keys();
        try {
            while (iterator.hasNext()) {
                String key = iterator.next();
                map.put(key, json.getString(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
}
