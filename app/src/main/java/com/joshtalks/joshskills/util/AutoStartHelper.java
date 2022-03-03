package com.joshtalks.joshskills.util;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.util.List;

public class AutoStartHelper {

    public final String TAG = "AutoStartHelper";
    private String PACKAGE_JOSHSKILLS = "com.joshtalks.joshskills";

    // Xiaomi and Redmi
    private final String BRAND_XIAOMI = "xiaomi";
    private final String BRAND_REDMI = "redmi";
    private String PACKAGE_XIAOMI_MAIN = "com.miui.securitycenter";
    private String PACKAGE_XIAOMI_COMPONENT = "com.miui.permcenter.autostart.AutoStartManagementActivity";

    // Letv
    private final String BRAND_LETV = "letv";
    private String PACKAGE_LETV_MAIN = "com.letv.android.letvsafe";
    private String PACKAGE_LETV_COMPONENT = "com.letv.android.letvsafe.AutobootManageActivity";

    // ASUS ROG
    private final String BRAND_ASUS = "asus";
    private String PACKAGE_ASUS_MAIN = "com.asus.batterysoh";
    private String PACKAGE_ASUS_COMPONENT = "com.asus.mobilemanager.powersaver.PowerSaverSettings";

    // Honor
    private final String BRAND_HONOR = "honor";
    private String PACKAGE_HONOR_MAIN = "com.huawei.systemmanager";
    private String PACKAGE_HONOR_COMPONENT = "com.huawei.systemmanager.optimize.process.ProtectActivity";

    // realme
    private final String BRAND_REALME = "realme";

    // Oppo
    private final String BRAND_OPPO = "oppo";
    private String PACKAGE_OPPO_MAIN = "com.coloros.safecenter";
    private String PACKAGE_OPPO_FALLBACK = "com.oppo.safe";
    private String PACKAGE_OPPO_COMPONENT = "com.coloros.safecenter.permission.startup.StartupAppListActivity";
    private String PACKAGE_OPPO_COMPONENT_FALLBACK = "com.oppo.safe.permission.startup.StartupAppListActivity";
    private String PACKAGE_OPPO_COMPONENT_FALLBACK_A = "com.coloros.safecenter.startupapp.StartupAppListActivity";

    // Vivo
    private final String BRAND_VIVO = "vivo";
    private String PACKAGE_VIVO_MAIN = "com.iqoo.secure";
    private String PACKAGE_VIVO_FALLBACK = "com.vivo.permissionmanager";
    private String PACKAGE_VIVO_COMPONENT = "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity";
    private String PACKAGE_VIVO_COMPONENT_FALLBACK = "com.vivo.permissionmanager.activity.BgStartUpManagerActivity";
    private String PACKAGE_VIVO_COMPONENT_FALLBACK_A = "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager";

    // Nokia
    private final String BRAND_NOKIA = "nokia";
    private String PACKAGE_NOKIA_MAIN = "com.evenwell.powersaving.g3";
    private String PACKAGE_NOKIA_COMPONENT = "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity";

    private AutoStartHelper() {
    }

    public static AutoStartHelper getInstance() {
        return new AutoStartHelper();
    }

    public void getAutoStartPermission(Context context) {
        String buildBrand = Build.BRAND.toLowerCase();
        switch (buildBrand) {
            case BRAND_ASUS:
                autoStartAsus(context);
                break;
            case BRAND_XIAOMI:
            case BRAND_REDMI:
                autoStartXiaomi(context);
                break;
//            case BRAND_LETV:
//                autoStartLetv(context);
//                break;
            case BRAND_HONOR:
                autoStartHonor(context);
                break;
            case BRAND_REALME:
                autoStartRealme(context);
                break;
            case BRAND_OPPO:
                autoStartOppo(context);
                break;
            case BRAND_VIVO:
                autoStartVivo(context);
                break;
//            case BRAND_NOKIA:
//                autoStartNokia(context);
//                break;
        }
    }

    private void autoStartAsus(final Context context) {
        if (isPackageExists(context, PACKAGE_ASUS_MAIN)) {
            showAlert(context, (dialog, which) -> {
                try {
                    startIntent(context, PACKAGE_ASUS_MAIN, PACKAGE_ASUS_COMPONENT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                dialog.dismiss();
            });
        }
    }

    private void showAlert(Context context, DialogInterface.OnClickListener onClickListener) {
        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setMessage("To receive calls and notifications please turn on the switch in the settings")
                .setPositiveButton("Settings", onClickListener)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .setCancelable(false)
                .show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTypeface(null, Typeface.BOLD);
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#107BE5"));

        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTypeface(null, Typeface.BOLD);
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#8D8D8D"));
    }

    private void autoStartXiaomi(final Context context) {
        if (isPackageExists(context, PACKAGE_XIAOMI_MAIN)) {
            showAlert(context, (dialog, which) -> {
                try {
                    startIntent(context, PACKAGE_XIAOMI_MAIN, PACKAGE_XIAOMI_COMPONENT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void autoStartLetv(final Context context) {
        if (isPackageExists(context, PACKAGE_LETV_MAIN)) {
            showAlert(context, (dialog, which) -> {
                try {
                    startIntent(context, PACKAGE_LETV_MAIN, PACKAGE_LETV_COMPONENT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void autoStartHonor(final Context context) {
        if (isPackageExists(context, PACKAGE_HONOR_MAIN)) {
            showAlert(context, (dialog, which) -> {
                try {
                    startIntent(context, PACKAGE_HONOR_MAIN, PACKAGE_HONOR_COMPONENT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void autoStartRealme(final Context context) {
        if (isPackageExists(context, PACKAGE_JOSHSKILLS))
            showAlert(context, (dialog, which) -> openAppInfoSettings(context));
    }

    private void autoStartOppo(final Context context) {
        if (isPackageExists(context, PACKAGE_OPPO_MAIN) || isPackageExists(context, PACKAGE_OPPO_FALLBACK)) {
            showAlert(context, (dialog, which) -> {
                try {
                    startIntent(context, PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT);
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        startIntent(context, PACKAGE_OPPO_FALLBACK, PACKAGE_OPPO_COMPONENT_FALLBACK);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        try {
                            startIntent(context, PACKAGE_OPPO_MAIN, PACKAGE_OPPO_COMPONENT_FALLBACK_A);
                        } catch (Exception exx) {
                            exx.printStackTrace();
                        }
                    }
                }
            });
        } else if (isPackageExists(context, PACKAGE_JOSHSKILLS))
            showAlert(context, (dialog, which) -> openAppInfoSettings(context));
    }

    private void autoStartVivo(final Context context) {
        if (isPackageExists(context, PACKAGE_VIVO_MAIN) || isPackageExists(context, PACKAGE_VIVO_FALLBACK)) {
            showAlert(context, (dialog, which) -> {
                try {
                    startIntent(context, PACKAGE_VIVO_MAIN, PACKAGE_VIVO_COMPONENT);
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        startIntent(context, PACKAGE_VIVO_FALLBACK, PACKAGE_VIVO_COMPONENT_FALLBACK);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        try {
                            startIntent(context, PACKAGE_VIVO_MAIN, PACKAGE_VIVO_COMPONENT_FALLBACK_A);
                        } catch (Exception exp) {
                            exp.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    private void autoStartNokia(final Context context) {
        if (isPackageExists(context, PACKAGE_NOKIA_MAIN)) {
            Log.e(TAG, "autoStartNokia: 1");
            showAlert(context, (dialog, which) -> {
                try {
                    startIntent(context, PACKAGE_NOKIA_MAIN, PACKAGE_NOKIA_COMPONENT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void startIntent(Context context, String packageName, String componentName) throws Exception {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(packageName, componentName));
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private Boolean isPackageExists(Context context, String targetPackage) {
        List<ApplicationInfo> packages;
        PackageManager pm = context.getPackageManager();
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            Log.e(TAG, packageInfo.packageName);
            if (packageInfo.packageName.equals(targetPackage))
                return true;
        }
        return false;
    }

    private void openAppInfoSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri = Uri.fromParts("package", PACKAGE_JOSHSKILLS, null);
            intent.setData(uri);
            context.startActivity(intent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}