package com.joshtalks.joshskills.core.analytics;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;

import com.clevertap.android.sdk.CleverTapAPI;
import com.flurry.android.Constants;
import com.flurry.android.FlurryAgent;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.joshtalks.joshskills.BuildConfig;
import com.joshtalks.joshskills.core.AppObjectController;
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel;
import com.joshtalks.joshskills.repository.local.model.Mentor;
import com.joshtalks.joshskills.repository.local.model.User;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import timber.log.Timber;

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
        updateFlurryUser();
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

    private static void updateFlurryUser() {
        Timber.tag("Furry").d("updateFlurryUser() called");
        User user = User.getInstance();
        Mentor mentor = Mentor.getInstance();
        FlurryAgent.setUserId(mentor.getId());
        FlurryAgent.setVersionName(BuildConfig.VERSION_NAME);
        FlurryAgent.setAge(getAge(user.getDateOfBirth()));
        FlurryAgent.setGender((user.getGender().equals("M") ? Constants.MALE : Constants.FEMALE));

        //User Properties
        List<String> list = new ArrayList<>();
        list.add(user.getUsername());
        list.add(mentor.getId());
        list.add(user.getPhoneNumber());
        list.add(user.getDateOfBirth());
        list.add(user.getUserType());
        list.add(user.getGender());
        FlurryAgent.UserProperties.set("JoshSkills.User", list);


    }

    public static int getAge(String dobString) {
        if (dobString == null || dobString.isEmpty()) {
            return -1;
        }
        Date date = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            date = sdf.parse(dobString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (date == null) return 0;
        Calendar dob = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        dob.setTime(date);

        int year = dob.get(Calendar.YEAR);
        int month = dob.get(Calendar.MONTH);
        int day = dob.get(Calendar.DAY_OF_MONTH);

        dob.set(year, month + 1, day);

        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }


        return age;
    }

    public static void flush() {
        if (cleverTapAnalytics == null) {
            cleverTapAnalytics = CleverTapAPI.getDefaultInstance(AppObjectController.getJoshApplication());
        }
        cleverTapAnalytics.flush();

    }

    public AppAnalytics addBasicParam() {
        parameters.put(AnalyticsEvent.APP_VERSION_CODE.getNAME(), BuildConfig.VERSION_NAME);
        parameters.put(AnalyticsEvent.DEVICE_MANUFACTURER.getNAME(), Build.MANUFACTURER);
        parameters.put(AnalyticsEvent.DEVICE_MODEL.getNAME(), Build.MODEL);
        parameters.put(AnalyticsEvent.ANDROID_OR_IOS.getNAME(), Build.VERSION.SDK_INT);
        if (InstallReferrerModel.getPrefObject() != null && !Objects.requireNonNull(InstallReferrerModel.getPrefObject().getUtmSource()).isEmpty())
            parameters.put(AnalyticsEvent.SOURCE.getNAME(), InstallReferrerModel.getPrefObject().getUtmSource());

        if (
                InstallReferrerModel.getPrefObject() != null &&
                        InstallReferrerModel.getPrefObject().getUtmMedium() != null &&
                        !InstallReferrerModel.getPrefObject().getUtmMedium().isEmpty()
        )
            parameters.put(AnalyticsEvent.UTM_MEDIUM.getNAME(), InstallReferrerModel.getPrefObject().getUtmMedium());
        return this;
    }

    public AppAnalytics addUserDetails() {
        if (!Objects.requireNonNull(User.getInstance().getUsername()).isEmpty())
            parameters.put(AnalyticsEvent.USER_NAME.getNAME(), User.getInstance().getUsername());
        if (Objects.requireNonNull(User.getInstance().getEmail()).isEmpty())
            parameters.put(AnalyticsEvent.USER_EMAIL.getNAME(), User.getInstance().getEmail());
        return this;
    }

    public AppAnalytics addParam(String key, String value) {
        parameters.put(key, value);
        return this;
    }

    public AppAnalytics addParam(String key, int value) {
        parameters.put(key, value);
        return this;
    }

    public AppAnalytics addParam(String key, long value) {
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
        Timber.v(this.toString());
        if (BuildConfig.DEBUG) {
            //return;
        }
        formatParameters();
        pushToFirebase();
        pushToCleverTap();
        pushToFlurry(false);
    }

    public void push(boolean trackSession) {
        Timber.v(this.toString());
        if (BuildConfig.DEBUG) {
            //return;
        }
        formatParameters();
        pushToFirebase();
        pushToCleverTap();
        pushToFlurry(trackSession);
    }

    public void endSession() {
        FlurryAgent.endTimedEvent(event);
    }

    private void formatParameters() {

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (entry.getValue() == null || Objects.requireNonNull(entry.getValue()).toString().isEmpty()) {
                parameters.put(entry.getKey(), "null");
            }
        }
    }

    private void pushToFirebase() {
        firebaseAnalytics.logEvent(event, convertMapToBundle(parameters));
    }

    private void pushToFlurry(boolean trackSession) {
        if (trackSession)
            FlurryAgent.logEvent(event, typeCastMap(parameters), true);
        else
            FlurryAgent.logEvent(event, typeCastMap(parameters));
    }

    private void pushToCleverTap() {
        cleverTapAnalytics.pushEvent(event, parameters);
    }

    private Bundle convertMapToBundle(HashMap<String, Object> properties) {
        Bundle bundle = new Bundle();
        for (String o : properties.keySet()) {
            String key = format(o);
            String value = "" + properties.get(key);
            bundle.putString(key, value);
        }
        return bundle;
    }

    private Map<String, String> typeCastMap(Map<String, Object> properties) {
        Map<String, String> newMap = new HashMap<>();

        for (Map.Entry<String, Object> entry : (properties).entrySet()) {
            if (entry.getValue() instanceof String) {
                newMap.put(entry.getKey(), (String) entry.getValue());
            } else {
                newMap.put(entry.getKey(), "" + entry.getValue());
            }
        }
        return newMap;
    }

    @NotNull
    @Override
    public String toString() {
        return "JoshSkillsAnalytics{" +
                "Event Name='" + event + '\'' +
                ", Parameters=" + parameters +
                '}';
    }
}
