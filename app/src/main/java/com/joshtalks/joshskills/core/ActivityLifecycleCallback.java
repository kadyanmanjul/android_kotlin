package com.joshtalks.joshskills.core;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.joshtalks.joshskills.core.analytics.AnalyticsEvent;
import com.joshtalks.joshskills.core.analytics.AppAnalytics;

import timber.log.Timber;

public class ActivityLifecycleCallback {
    public static synchronized void register(Application application) {
        if (application == null) {
            return;
        }
        application.registerActivityLifecycleCallbacks(
                new Application.ActivityLifecycleCallbacks() {

                    @Override
                    public void onActivityCreated(Activity activity, Bundle bundle) {
                        AppAnalytics.create(AnalyticsEvent.ACTIVITY_OPENED.getNAME())
                                .addParam("name", activity.getClass().getSimpleName())
                                .push();
                        Timber.tag("Josh_Activity_Created").d(activity.getClass().getSimpleName());

                    }

                    @Override
                    public void onActivityStarted(Activity activity) {
                        Timber.tag("Josh_Activity_Started").d(activity.getClass().getSimpleName());
                    }

                    @Override
                    public void onActivityResumed(Activity activity) {
                        AppAnalytics.create(AnalyticsEvent.ACTIVITY_REOPEN.getNAME())
                                .addParam("name", activity.getClass().getSimpleName())
                                .push();
                        Timber.tag("Josh_Activity_Resumed").d(activity.getClass().getSimpleName());

                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                        Timber.tag("Josh_Activity_Paused").d(activity.getClass().getSimpleName());

                    }

                    @Override
                    public void onActivityStopped(Activity activity) {
                        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.getNAME())
                                .addParam("name", activity.getClass().getSimpleName())
                                .push();
                        Timber.tag("Josh_Activity_Stopped").d(activity.getClass().getSimpleName());

                    }

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {
                        Timber.tag("Josh_Activity_Destroyed").d(activity.getClass().getSimpleName());

                    }

                    @Override
                    public void onActivityPostStopped(@NonNull Activity activity) {

                    }
                }

        );
    }

}
