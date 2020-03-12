package com.joshtalks.joshskills.core.analytics;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.clevertap.android.sdk.CleverTapAPI;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.joshtalks.joshskills.BuildConfig;
import com.joshtalks.joshskills.core.AppObjectController;
import com.joshtalks.joshskills.repository.local.model.Mentor;
import com.joshtalks.joshskills.repository.local.model.User;

import java.util.HashMap;
import java.util.Objects;

public class AppAnalytics {

    @SuppressLint("StaticFieldLeak")
    private static CleverTapAPI cleverTapAnalytics;
    private static FirebaseAnalytics firebaseAnalytics;
    private final String event;
    private HashMap<String, Object> parameters = new HashMap<>();

    public AppAnalytics(String event) {
        init();
        this.event = format(event);
    }

    private static void init() {

        if (cleverTapAnalytics == null) {
            cleverTapAnalytics = CleverTapAPI.getDefaultInstance(AppObjectController.getJoshApplication());
            if (BuildConfig.DEBUG) {
                CleverTapAPI.setDebugLevel(CleverTapAPI.LogLevel.DEBUG);
            }

        }

        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(AppObjectController.getJoshApplication());
        }
    }


    public static AppAnalytics create(String title) {
        return new AppAnalytics(title);
    }

    public static void updateUser() {
        init();
        updateCleverTapUser();
        updateFabricUser();
    }

    private static void updateCleverTapUser() {
        User user = User.getInstance();
        Mentor mentor = Mentor.getInstance();
        HashMap<String, Object> profileUpdate = new HashMap<>();
        profileUpdate.put("Name", user.getFirstName());
        profileUpdate.put("Identity", mentor.getId());
        profileUpdate.put("Phone", user.getPhoneNumber());
        profileUpdate.put("MentorIdentity", mentor.getId());
        profileUpdate.put("Photo", user.getPhoto());
        profileUpdate.put("date_of_birth", user.getDateOfBirth());
        profileUpdate.put("Username", user.getUsername());
        profileUpdate.put("User Type", user.getUserType());
        cleverTapAnalytics.pushProfile(profileUpdate);
    }

    private static void updateFabricUser() {
        User user = User.getInstance();
        Crashlytics.setUserIdentifier(user.getId());
        Crashlytics.setUserName(user.getFirstName());
        Crashlytics.setString("cleverId", cleverTapAnalytics.getCleverTapID());
    }

    public static void flush() {
        if (cleverTapAnalytics == null) {
            cleverTapAnalytics = CleverTapAPI.getDefaultInstance(AppObjectController.getJoshApplication());
        }
        cleverTapAnalytics.flush();

    }

    public AppAnalytics addParam(String key, String value) {
        parameters.put(key, value);
        return this;
    }

    public AppAnalytics addParam(String key, int value) {
        parameters.put(key, value);
        return this;
    }

    public AppAnalytics addParam(String key, double value) {
        parameters.put(key, value);
        return this;
    }

    public AppAnalytics addParam(String key, boolean value) {
        parameters.put(key, value);
        return this;
    }

    private String format(String unformatted) {
        unformatted = unformatted.trim().toLowerCase();
        StringBuilder formatted = new StringBuilder();
        for (int index = 0; index < unformatted.length(); index++) {
            char c = unformatted.charAt(index);
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9')) {
                c = '_';
            }
            formatted.append(c);
        }
        return formatted.toString();
    }

    public void push() {
        formatParameters();
        pushToFirebase();
        pushToCleverTap();
    }

    private void formatParameters() {

        for (String key : parameters.keySet()) {
            if (parameters.get(key) == null || Objects.requireNonNull(parameters.get(key)).toString().isEmpty()) {
                parameters.put(key, "null");
            }
        }
    }

    private void pushToFirebase() {
        firebaseAnalytics.logEvent(event, convertMapToBundle(parameters));
    }

    private void pushToCleverTap() {
        cleverTapAnalytics.pushEvent(event, parameters);
    }

    private Bundle convertMapToBundle(HashMap properites) {
        Bundle bundle = new Bundle();
        for (Object o : properites.keySet()) {

            String key = format((String) o);
            String value = "" + properites.get(key);

            bundle.putString(key, value);
        }

        return bundle;
    }

}
