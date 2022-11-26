package com.joshtalks.joshskills.common.core.analytics;

import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import com.freshchat.consumer.sdk.FreshchatUser;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.joshtalks.joshskills.common.BuildConfig;
import com.joshtalks.joshskills.common.core.AppObjectController;
import com.joshtalks.joshskills.common.core.JoshSkillExecutors;
import com.joshtalks.joshskills.common.core.PrefManager;
import static com.joshtalks.joshskills.common.core.PrefManagerKt.IS_FREE_TRIAL;
import static com.joshtalks.joshskills.common.core.PrefManagerKt.USER_UNIQUE_ID;
import static com.joshtalks.joshskills.common.core.StaticConstantKt.EMPTY;
import static com.joshtalks.joshskills.common.core.UtilsKt.getPhoneNumber;
import com.joshtalks.joshskills.common.core.Utils;
import com.joshtalks.joshskills.common.repository.local.model.InstallReferrerModel;
import com.joshtalks.joshskills.common.repository.local.model.Mentor;
import com.joshtalks.joshskills.common.repository.local.model.User;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

public class AppAnalytics {

    private static FirebaseAnalytics firebaseAnalytics;
    private final String event;
    private final HashMap<String, Object> parameters = new HashMap<>();

    public AppAnalytics(String event) {
        init();
        this.event = format(event);
    }

    private static void init() {
        JoshSkillExecutors.getBOUNDED().submit(() -> {
            try {
                if (firebaseAnalytics == null) {
                    firebaseAnalytics = FirebaseAnalytics.getInstance(AppObjectController.getJoshApplication());
                    firebaseAnalytics.setAnalyticsCollectionEnabled(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void updateUser() {
        init();
        updateMixPanelUser();
        updateFreshchatSdkUser();
        updateFreshchatSdkUserProperties();
        updateFirebaseSdkUser();
    }

    public static AppAnalytics create(String title) {
        return new AppAnalytics(title);
    }

    private static void updateFreshchatSdkUser() {
        try {
            FreshchatUser freshchatUser = AppObjectController.getFreshChat().getUser();
            freshchatUser.setFirstName(User.getInstance().getFirstName());
            freshchatUser.setEmail(User.getInstance().getEmail());
            String mobileNumber = getPhoneNumber();
            if (!mobileNumber.isEmpty()) {
                int length = mobileNumber.length();
                if (length > 10) {
                    freshchatUser.setPhone(mobileNumber.substring(0, length - 10), mobileNumber.substring(length - 10));
                }
            } else freshchatUser.setPhone("+91", "XXXXXXXXXX");

            AppObjectController.getFreshChat().setUser(freshchatUser);
        } catch (Exception e) {
            //   e.printStackTrace();
        }
    }

    private static void updateFreshchatSdkUserProperties() {
        Map<String, String> userMeta = new HashMap<>();
        userMeta.put("Username", User.getInstance().getFirstName());
        userMeta.put("Email_id", User.getInstance().getEmail());
        userMeta.put("Mobile_no", getPhoneNumber());
        userMeta.put("Age", String.valueOf(getAge(User.getInstance().getDateOfBirth())));
        userMeta.put("Gender", User.getInstance().getGender());
        if (Mentor.getInstance().hasId()) {
            userMeta.put("Mentor_id", Mentor.getInstance().getId());
            userMeta.put("Login_type", "yes");
            userMeta.put("Subscribed_user", "yes");

            try {
                List<String> allConversationId = AppObjectController.getAppDatabase().courseDao().getAllConversationId();
                userMeta.put("courses_availed", String.valueOf(allConversationId.size()));
                for (int i = 0; i < allConversationId.size(); i++) {
                    userMeta.put("courses_" + i, AppObjectController.getAppDatabase().courseDao().chooseRegisterCourseMinimal(allConversationId.get(i)).getCourse_name());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

//Call setUserProperties to sync the user properties with Freshchat's servers
        try {
            AppObjectController.getFreshChat().setUserProperties(userMeta);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateMixPanelUser() {
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
        profileUpdate.put("Gender", user.getGender());

//        MixPanelTracker.INSTANCE.getMixPanel().alias(PrefManager.INSTANCE.getStringValue(USER_UNIQUE_ID, false, EMPTY), PrefManager.INSTANCE.getStringValue(USER_UNIQUE_ID, false, EMPTY));
//        MixPanelTracker.INSTANCE.getMixPanel().identify(PrefManager.INSTANCE.getStringValue(USER_UNIQUE_ID, false, EMPTY));
//        MixPanelTracker.INSTANCE.getMixPanel().getPeople().identify(PrefManager.INSTANCE.getStringValue(USER_UNIQUE_ID, false, EMPTY));

        JSONObject obj = new JSONObject();
        try {
            obj.put("gaid", PrefManager.INSTANCE.getStringValue(USER_UNIQUE_ID, false, EMPTY));
            obj.put("mentor id", Mentor.getInstance().getId());
            obj.put("gender", User.getInstance().getGender());
            obj.put("date of birth", User.getInstance().getDateOfBirth());
            obj.put("created source", User.getInstance().getSource());
            obj.put("is verified", User.getInstance().isVerified());
            obj.put("email", User.getInstance().getEmail());
            obj.put("phone number", User.getInstance().getPhoneNumber());

        } catch (JSONException e) {
            e.printStackTrace();
        }

//        MixPanelTracker.INSTANCE.getMixPanel().getPeople().set(obj);

//        JSONObject props = new JSONObject();
//        try {
//            props.put("is paid", !PrefManager.INSTANCE.getBoolValue(IS_FREE_TRIAL, false, true));
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
       // MixPanelTracker.INSTANCE.getMixPanel().registerSuperProperties(props);
    }

    private static void updateFirebaseSdkUser() {
        try {
            User user = User.getInstance();
            Mentor mentor = Mentor.getInstance();
            String gaid = PrefManager.INSTANCE.getStringValue(USER_UNIQUE_ID, false, EMPTY);

            firebaseAnalytics.setUserId(gaid);
            firebaseAnalytics.setUserProperty("gaid", gaid);
            firebaseAnalytics.setUserProperty("mentor_id", mentor.getId());
            firebaseAnalytics.setUserProperty("phone", getPhoneNumber());
            firebaseAnalytics.setUserProperty("first_name", user.getFirstName());
            firebaseAnalytics.setUserProperty("email", user.getEmail());
            firebaseAnalytics.setUserProperty("age", getAge(user.getDateOfBirth()) + "");
            firebaseAnalytics.setUserProperty("date_of_birth", user.getDateOfBirth());
            firebaseAnalytics.setUserProperty("gender", (user.getGender().equals("M") ? "MALE" : "FEMALE"));
            firebaseAnalytics.setUserProperty("username", user.getUsername());
            firebaseAnalytics.setUserProperty("user_type", user.getUserType());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setLocation(double latitude, double longitude) {
        if (latitude != 0.0d) {
            Location location = new Location("Location");
            location.setLatitude(latitude);
            location.setLongitude(longitude);
        }
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
            Timber.e(e);
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

    public AppAnalytics addBasicParam() {
        try {
            parameters.put(AnalyticsEvent.APP_VERSION_CODE.getNAME(),  AppObjectController.applicationDetails.versionName());
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
        } catch (Exception e) {
        }
        return this;
    }

    public AppAnalytics addDeviceId(){
        try {
            parameters.put(AnalyticsEvent.DEVICE_ID.getNAME(), Utils.INSTANCE.getDeviceId());
        } catch (Exception e) {
        }
        return this;
    }

    public AppAnalytics addUserDetails() {
        try {
            if (PrefManager.INSTANCE != null && !PrefManager.INSTANCE.getStringValue(USER_UNIQUE_ID, false, EMPTY).isEmpty())
                parameters.put(AnalyticsEvent.USER_GAID.getNAME(), PrefManager.INSTANCE.getStringValue(USER_UNIQUE_ID, false, EMPTY));
            if (Mentor.getInstance().hasId())
                parameters.put(AnalyticsEvent.USER_MENTOR_ID.getNAME(), Mentor.getInstance().getId());
            if (!User.getInstance().getFirstName().isEmpty())
                parameters.put(AnalyticsEvent.USER_NAME.getNAME(), User.getInstance().getFirstName());
            if (!User.getInstance().getEmail().isEmpty())
                parameters.put(AnalyticsEvent.USER_EMAIL.getNAME(), User.getInstance().getEmail());
            if (!User.getInstance().getPhoneNumber().isEmpty())
                parameters.put(AnalyticsEvent.USER_PHONE_NUMBER.getNAME(), User.getInstance().getPhoneNumber());
        } catch (Exception ignored) {
        }
        return this;
    }

    public AppAnalytics addParam(String key, String value) {
        try {
            parameters.put(key, value);
            return this;
        } catch (Exception ex) {
            ex.printStackTrace();
            return this;
        }
    }

    public AppAnalytics addParam(String key, List<String> value) {
        if (value == null || value.isEmpty() || value.size() == 0)
            return this;
        for (int i = 0; i < value.size(); i++) {
            parameters.put(key + "_" + i, value.get(i));
        }
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

    public void push() {
        Timber.v(this.toString());
        if (BuildConfig.DEBUG) {
            return;
        }
        JoshSkillExecutors.getBOUNDED().submit(() -> {
            try {
                formatParameters();
                pushToFirebase();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    private void formatParameters() {

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (entry.getValue() == null || Objects.requireNonNull(entry.getValue()).toString().isEmpty()) {
                parameters.put(entry.getKey(), "null");
            }
        }
    }

    private void pushToFirebase() {
        try {
            if (firebaseAnalytics != null)
                firebaseAnalytics.logEvent(event, convertMapToBundle(parameters));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @NotNull
    @Override
    public String toString() {
        return "JoshSkillsAnalytics{" +
                "Event Name='" + event + '\'' +
                ", Parameters=" + parameters +
                '}';
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
}
