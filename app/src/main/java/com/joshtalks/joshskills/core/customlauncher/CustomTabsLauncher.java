package com.joshtalks.joshskills.core.customlauncher;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.browser.customtabs.CustomTabsIntent;

import java.util.List;

import static com.joshtalks.joshskills.core.customlauncher.ChromePackage.CHROME_PACKAGES;

/**
 * A utility class that directly launches a browser that supports Custom Tabs.<br/>
 * <p>
 * This library try to launch Chrome(s) as the default behavior,
 * but if they are not installed you can launch a browser that supports other Custom Tabs using {@link LaunchNonChromeCustomTabs}.
 * </p>
 * <h3>Basic usage:</h3>
 * <pre>{@code
 *  final CustomTabsIntent customTabsIntent = ...;
 *  CustomTabsLauncher.launch(ctx, customTabsIntent, URI);
 * }</pre>
 * <h3>Launch non-Chrome browser as Fallback</h3>
 * <pre>{@code
 *  final CustomTabsIntent customTabsIntent = ...;
 *  final exampleNonChromePackages = Arrays.asList(
 *    "org.mozilla.firefox",
 *    "com.microsoft.emmx",
 *  );
 *  CustomTabsLauncher.launch(ctx, customTabsIntent, URI, exampleNonChromePackages);
 * }</pre>
 */
public final class CustomTabsLauncher {

    private static final CustomTabsLauncherImpl IMPL = new CustomTabsLauncherImpl();

    /**
     * Opens the URL on a Custom Tabs if possible.
     *
     * @param context          The source Context
     * @param customTabsIntent a CustomTabsIntent to be used if Custom Tabs is available
     * @param uri              the Uri to be opened.
     */
    public static void launch(
            @NonNull Context context,
            @NonNull CustomTabsIntent customTabsIntent,
            @NonNull Uri uri) {
        launch(context, customTabsIntent, uri, null);
    }

    /**
     * Opens the URL on a Custom Tabs if possible. Otherwise fallsback to opening it on a WebView.
     *
     * @param context          The source Context
     * @param customTabsIntent a CustomTabsIntent to be used if Custom Tabs is available
     * @param uri              the Uri to be opened
     * @param fallback         a {@link } to be used if Custom Tabs is not available.
     */
    public static void launch(
            @NonNull Context context,
            @NonNull CustomTabsIntent customTabsIntent,
            @NonNull Uri uri,
            @Nullable CustomTabsFallback fallback) {
        IMPL.launch(context, customTabsIntent, uri, CHROME_PACKAGES, fallback);
    }

    /**
     * Fallback for launch the browser installed on the device.
     */
    public static class LaunchBrowser implements CustomTabsFallback {

        @Override
        public void openUrl(
                @NonNull Context context,
                @NonNull Uri uri,
                @NonNull CustomTabsIntent customTabsIntent) {
            final Intent intent = new Intent(Intent.ACTION_VIEW, uri)
                    .setFlags(customTabsIntent.intent.getFlags());
            context.startActivity(intent);
        }
    }

    /**
     * Fallback for launch a non-Chrome browser that supports Custom Tabs.
     */
    public static class LaunchNonChromeCustomTabs implements CustomTabsFallback {

        private final List<String> customTabsPackages;

        /**
         * @param customTabsPackages Package list of non-Chrome browsers supporting Custom Tabs. The top of the list is used with the highest priority.
         */
        public LaunchNonChromeCustomTabs(
                @NonNull @Size(min = 1) List<String> customTabsPackages) {
            this.customTabsPackages = customTabsPackages;
        }

        @Override
        public void openUrl(
                @NonNull Context context,
                @NonNull Uri uri,
                @NonNull CustomTabsIntent customTabsIntent) {
            IMPL.launch(context, customTabsIntent, uri, customTabsPackages, new LaunchBrowser());
        }
    }
}
