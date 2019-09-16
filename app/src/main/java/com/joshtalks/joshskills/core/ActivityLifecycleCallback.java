package com.joshtalks.joshskills.core;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent;
import com.joshtalks.joshskills.core.analytics.AppAnalytics;

public class ActivityLifecycleCallback {
    public static synchronized void register(Application application) {
        if (application == null) {
            return;
        }
        application.registerActivityLifecycleCallbacks(
                new Application.ActivityLifecycleCallbacks() {

                    @Override
                    public void onActivityCreated(Activity activity, Bundle bundle) {

                    }

                    @Override
                    public void onActivityStarted(Activity activity) {
                    }

                    @Override
                    public void onActivityResumed(Activity activity) {
                        AppAnalytics.create(AnalyticsEvent.ACTIVITY_OPENED.getNAME())
                                .addParam("name", activity.getClass().getSimpleName())
                                .push();
                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                    }

                    @Override
                    public void onActivityStopped(Activity activity) {
                    }

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {
                    }
                }

        );
    }

}
