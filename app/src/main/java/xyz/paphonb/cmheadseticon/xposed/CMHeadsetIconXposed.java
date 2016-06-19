package xyz.paphonb.cmheadseticon.xposed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.os.Handler;
import android.util.Log;

import java.util.Objects;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.paphonb.cmheadseticon.R;

public class CMHeadsetIconXposed implements IXposedHookLoadPackage, IXposedHookInitPackageResources, IXposedHookZygoteInit {
    public static final String PACKAGE_SYSTEMUI = "com.android.systemui";
    private static final Object SLOT_HEADSET = "headset";
    private static final String PACKAGE_ANDROID = "android";
    private static String MODULE_PATH = null;
    private Object mService;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                updateHeadset(intent);
            }
        }
    };

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        hookSystemUI(lpparam);
        hookAndroid(lpparam);
    }

    private void hookAndroid(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals(PACKAGE_ANDROID)) return;

        Class<?> classStatusBarManagerService = XposedHelpers.findClass("com.android.internal.statusbar.StatusBarIconList", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(classStatusBarManagerService, "defineSlots", String[].class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String[] slots = (String[]) param.args[0];
                int N = slots.length;

                String[] newSlots = new String[N + 1];
                for (int i = 0, j = 0; i < N + 1; i++) {
                    newSlots[i] = slots[j];
                    if (Objects.equals(slots[j], "speakerphone")) {
                        i++;
                        newSlots[i] = "headset";
                    }
                    j++;
                }
                param.args[0] = newSlots;
            }
        });
    }

    private void hookSystemUI(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals(PACKAGE_SYSTEMUI)) return;

        Class<?> classPhoneStatusBarPolicy = XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBarPolicy", lpparam.classLoader);
        XposedBridge.hookAllConstructors(classPhoneStatusBarPolicy, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                Resources res = context.getResources();

                mService = XposedHelpers.getObjectField(param.thisObject, "mService");
                XposedHelpers.callMethod(mService, "setIcon", SLOT_HEADSET, res.getIdentifier("stat_sys_ethernet", "drawable", PACKAGE_SYSTEMUI), 0, null);
                XposedHelpers.callMethod(mService, "setIconVisibility", SLOT_HEADSET, false);

                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_HEADSET_PLUG);
                context.registerReceiver(mIntentReceiver, filter, null, (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler"));
            }
        });
    }

    private void updateHeadset(Intent intent) {
        int state = intent.getIntExtra("state", 0);
        XposedHelpers.callMethod(mService, "setIconVisibility", SLOT_HEADSET, state == 1);
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals(PACKAGE_SYSTEMUI)) return;

        XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        resparam.res.setReplacement(PACKAGE_SYSTEMUI, "drawable", "stat_sys_ethernet", modRes.fwd(R.drawable.stat_sys_headset));
    }
}
