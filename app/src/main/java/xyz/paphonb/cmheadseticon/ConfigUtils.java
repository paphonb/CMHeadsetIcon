package xyz.paphonb.cmheadseticon;

import de.robv.android.xposed.XSharedPreferences;
import xyz.paphonb.cmheadseticon.xposed.CMHeadsetIconXposed;

public class ConfigUtils {

    private static ConfigUtils mInstance;

    private XSharedPreferences mPrefs;
    public IconsConfig icons;

    private ConfigUtils() {
        mInstance = this;
        mPrefs = new XSharedPreferences(CMHeadsetIconXposed.PACKAGE_OWN);
        loadConfig();
    }

    private void loadConfig() {
        icons = new IconsConfig(mPrefs);
    }

    public static ConfigUtils getInstance() {
        if (mInstance == null)
            mInstance = new ConfigUtils();
        return mInstance;
    }

    public static IconsConfig icons() {
        return getInstance().icons;
    }

    public class IconsConfig {
        public int headset;
        public int headphone;

        public IconsConfig(XSharedPreferences prefs) {
            headset = MainActivity.ICONS[prefs.getInt("headseticon", 0)];
            headphone = MainActivity.ICONS[prefs.getInt("headphoneicon", 0)];
        }
    }

}
