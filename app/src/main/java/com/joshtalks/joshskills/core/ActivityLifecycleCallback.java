package com.joshtalks.joshskills.core;

import static com.joshtalks.joshskills.core.CountUpTimer.TAG;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.joshtalks.joshskills.core.analytics.AnalyticsEvent;
import com.joshtalks.joshskills.core.analytics.AppAnalytics;
import com.joshtalks.joshskills.engage_notification.AppActivityModel;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import timber.log.Timber;

public class ActivityLifecycleCallback {
    private static final ExecutorService executor =
            JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-ActivityLifecycler-Service");
    public static Activity currentActivity = null;

    private ActivityLifecycleCallback() {
    }

    public static synchronized void register(Application application) {
        if (application == null) {
            return;
        }
        application.registerActivityLifecycleCallbacks(
                new Application.ActivityLifecycleCallbacks() {

                    @Override
                    public void onActivityCreated(@NotNull Activity activity, Bundle bundle) {
                        currentActivity=activity;
                        AppAnalytics.create(AnalyticsEvent.ACTIVITY_CREATED.getNAME())
                                .addParam("name", activity.getClass().getSimpleName())
                                .push();
                        Timber.tag("Josh_Activity_Created").d(activity.getClass().getSimpleName());
                    }

                    @Override
                    public void onActivityStarted(@NotNull Activity activity) {
                        currentActivity=activity;
                        AppAnalytics.create(AnalyticsEvent.ACTIVITY_START.getNAME())
                                .addParam("name", activity.getClass().getSimpleName())
                                .push();
                        Timber.tag("Josh_Activity_Started").d(activity.getClass().getSimpleName());
                    }

                    @Override
                    public void onActivityResumed(@NotNull Activity activity) {
                        currentActivity=activity;
                        AppAnalytics.create(AnalyticsEvent.ACTIVITY_RESUME.getNAME())
                                .addParam("name", activity.getClass().getSimpleName())
                                .push();
                        Timber.tag("Josh_Activity_Resumed").d(activity.getClass().getSimpleName());
                        AppObjectController.setCurrentActivityClass(activity.getClass().getSimpleName());
                        executor.execute(
                                () ->
                                        AppObjectController.getAppDatabase()
                                                .appActivityDao()
                                                .insertIntoAppActivity(
                                                        new AppActivityModel(activity.getClass().getSimpleName())

                                                ));
                    }

                    @Override
                    public void onActivityPaused(@NotNull Activity activity) {
                        AppAnalytics.create(AnalyticsEvent.ACTIVITY_PAUSE.getNAME())
                                .addParam("name", activity.getClass().getSimpleName())
                                .push();
                        AppObjectController.setCurrentActivityClass(null);

                        Timber.tag("Josh_Activity_Paused").d(activity.getClass().getSimpleName());
                    }

                    @Override
                    public void onActivityStopped(@NotNull Activity activity) {
                        AppAnalytics.create(AnalyticsEvent.ACTIVITY_STOP.getNAME())
                                .addParam("name", activity.getClass().getSimpleName())
                                .push();
                        Timber.tag("Josh_Activity_Stopped").d(activity.getClass().getSimpleName());
                    }

                    @Override
                    public void onActivityPostStopped(@NonNull Activity activity) {
                    }

                    @Override
                    public void onActivitySaveInstanceState(@NotNull Activity activity, @NotNull Bundle bundle) {
                    }

                    @Override
                    public void onActivityDestroyed(@NotNull Activity activity) {
                        AppAnalytics.create(AnalyticsEvent.ACTIVITY_DESTROY.getNAME())
                                .addParam("name", activity.getClass().getSimpleName())
                                .push();

                        Timber.tag("Josh_Activity_Destroyed").d(activity.getClass().getSimpleName());
                    }
                });
    }
}
