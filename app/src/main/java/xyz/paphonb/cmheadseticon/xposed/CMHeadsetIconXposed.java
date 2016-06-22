package xyz.paphonb.cmheadseticon.xposed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import java.util.Objects;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.paphonb.cmheadseticon.ConfigUtils;
import xyz.paphonb.cmheadseticon.MainActivity;
import xyz.paphonb.cmheadseticon.R;

public class CMHeadsetIconXposed implements IXposedHookLoadPackage {
    public static final String PACKAGE_SYSTEMUI = "com.android.systemui";
    private static final String SLOT_HEADSET = "headset";
    private static final String SLOT_HEADPHONE = "headphone";
    private static final String PACKAGE_ANDROID = "android";
    public static final String PACKAGE_OWN = "xyz.paphonb.cmheadseticon";
    private static final String MAIN_ACTIVITY = PACKAGE_OWN + ".MainActivity";
    private static final String TAG = "CMHeadsetIconXposed";
    private int mState = 0;
    private int mMicrophone = 0;
    private Object mService;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                updateHeadset(intent);
            } else if (intent.getAction().equals(MainActivity.ICON_CHANGED)) {
                updateHeadsetIcon(intent);
            }
        }
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        hookSystemUI(lpparam);
        hookAndroid(lpparam);

        if (!lpparam.packageName.equals(PACKAGE_OWN)) return;
        XposedHelpers.findAndHookMethod(MAIN_ACTIVITY, lpparam.classLoader, "isActivated", XC_MethodReplacement.returnConstant(true));
    }

    private void hookAndroid(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals(PACKAGE_ANDROID)) return;

        Class<?> classStatusBarManagerService = XposedHelpers.findClass("com.android.internal.statusbar.StatusBarIconList", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(classStatusBarManagerService, "defineSlots", String[].class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String[] slots = (String[]) param.args[0];
                int N = slots.length;

                String[] newSlots = new String[N + 2];
                for (int i = 0, j = 0; i < N + 2; i++) {
                    newSlots[i] = slots[j];
                    if (Objects.equals(slots[j], "speakerphone")) {
                        newSlots[++i] = "headset";
                        newSlots[++i] = "headphone";
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

                mService = XposedHelpers.getObjectField(param.thisObject, "mService");

                setIcon(SLOT_HEADSET, ConfigUtils.icons().headset, 0, null);
                setIconVisibility(SLOT_HEADSET, false);

                setIcon(SLOT_HEADPHONE, ConfigUtils.icons().headphone, 0, null);
                setIconVisibility(SLOT_HEADPHONE, false);

                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_HEADSET_PLUG);
                filter.addAction(MainActivity.ICON_CHANGED);
                context.registerReceiver(mIntentReceiver, filter, null, (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler"));
            }
        });
    }

    public void setIcon(String slot, int iconId, int iconLevel, String contentDescription) {
        try {
            Object svc = XposedHelpers.callMethod(mService, "getService");
            if (svc != null) {
                XposedHelpers.callMethod(svc, "setIcon", slot, PACKAGE_OWN, iconId, iconLevel, contentDescription);
            }
        } catch (Throwable ex) {
            // system process is dead anyway.
            throw new RuntimeException(ex);
        }
    }

    public void setIconVisibility(String slot, boolean visible) {
        try {
            Object svc = XposedHelpers.callMethod(mService, "getService");
            if (svc != null) {
                XposedHelpers.callMethod(svc, "setIconVisibility", slot, visible);
            }
        } catch (Throwable ex) {
            // system process is dead anyway.
            throw new RuntimeException(ex);
        }
    }

    private void updateHeadset(Intent intent) {
        mState = intent.getIntExtra("state", 0);
        mMicrophone = intent.getIntExtra("microphone", 0);

        updateIconVisibilities();
    }

    private void updateIconVisibilities() {
        setIconVisibility(SLOT_HEADSET, mState != 0 && mMicrophone == 1);
        setIconVisibility(SLOT_HEADPHONE, mState != 0 && mMicrophone == 0);
    }

    private void updateHeadsetIcon(Intent intent) {
        int type = intent.getIntExtra(MainActivity.EXTRA_ICON_TYPE, 0);
        int value = intent.getIntExtra(MainActivity.EXTRA_ICON_VALUE, 0);

        setIcon(type == 0 ? SLOT_HEADSET : SLOT_HEADPHONE, MainActivity.ICONS[value], 0, null);

        updateIconVisibilities();
    }
}
