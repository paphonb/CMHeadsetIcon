package xyz.paphonb.cmheadseticon;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.support.design.widget.Snackbar;

import xyz.paphonb.cmheadseticon.xposed.CMHeadsetIconXposed;

import static xyz.paphonb.cmheadseticon.xposed.CMHeadsetIconXposed.PACKAGE_OWN;

public class MainActivity extends AppCompatActivity implements IconSelectionView.IconSelectionListener, View.OnClickListener {

    public static final String ICON_CHANGED = "xyz.paphonb.cmheadseticon.action.ICON_CHANGED";
    public static final String EXTRA_ICON_TYPE = "extra.ICON_TYPE";
    public static final String EXTRA_ICON_VALUE = "extra.ICON_VALUE";
    private static final String HIDE_APP_ICON = "hide_app_icon";
    private SharedPreferences mPreferences;
    private static final String TAG = "MainActivity";
    public static int[] ICONS;

    static {
        ICONS = new int[] {
                R.drawable.ic_earbuds,
                R.drawable.ic_headphone,
                R.drawable.ic_headset,
                R.drawable.ic_headset_dock,
                R.drawable.ic_speaker
        };
    }

    @SuppressWarnings("ConstantConditions")
    @SuppressLint("WorldReadableFiles")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preferences = getPreferences();

        IconSelectionView headsetChooseView = (IconSelectionView) findViewById(R.id.headset_choose);
        headsetChooseView.setTitle(getString(R.string.with_mic));
        headsetChooseView.setStrings(getResources().getStringArray(R.array.icons_name));
        headsetChooseView.setIcons(ICONS);
        headsetChooseView.setListener(this);
        headsetChooseView.setSharedPreferences(preferences);
        headsetChooseView.setKey("headseticon");

        IconSelectionView headphoneChooseView = (IconSelectionView) findViewById(R.id.headphone_choose);
        headphoneChooseView.setTitle(getString(R.string.without_mic));
        headphoneChooseView.setStrings(getResources().getStringArray(R.array.icons_name));
        headphoneChooseView.setIcons(ICONS);
        headphoneChooseView.setListener(this);
        headphoneChooseView.setSharedPreferences(preferences);
        headphoneChooseView.setKey("headphoneicon");

        if (!isActivated()) {
            headsetChooseView.setVisibility(View.GONE);
            headphoneChooseView.setVisibility(View.GONE);

            View warning = findViewById(R.id.warning);
            warning.setVisibility(View.VISIBLE);

            TextView warningText = (TextView) findViewById(R.id.warningText);

            if (hasXposedInstaller()) {
                warningText.setText(R.string.module_not_activated);
                warning.setClickable(true);
                warning.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(getXposedInstallerLaunchIntent());
                    }
                });
            } else {
                warningText.setText(R.string.xposed_required);
            }
        }

        if (isFromPlayStore()) {
            if (!hasXposedInstaller()) {
                Snackbar.make(findViewById(R.id.main), R.string.refund_warning, Snackbar.LENGTH_LONG).setAction(R.string.play_store, this).show();
            } else if (preferences.getBoolean("firstStart", true)) {
                preferences.edit().putBoolean("firstStart", false).apply();
                Snackbar.make(findViewById(R.id.main), R.string.thanks, Snackbar.LENGTH_LONG).setAction(R.string.rate, this).show();
            }
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("WorldReadableFiles")
    public SharedPreferences getPreferences() {
        if (mPreferences != null)
            return mPreferences;
        mPreferences = getSharedPreferences("xyz.paphonb.cmheadseticon_preferences", MODE_WORLD_READABLE);
        return mPreferences;
    }

    @Override
    public void onSelect(IconSelectionView view, int position) {
        Intent intent = new Intent(ICON_CHANGED);
        intent.putExtra(EXTRA_ICON_TYPE, view.getId() == R.id.headset_choose ? 0 : 1);
        intent.putExtra(EXTRA_ICON_VALUE, position);
        intent.setPackage(CMHeadsetIconXposed.PACKAGE_SYSTEMUI);
        sendBroadcast(intent);
    }

    public boolean isFromPlayStore() {
        List<String> validInstallers = new ArrayList<>(Arrays.asList("com.android.vending", "com.google.android.feedback"));
        String installer = getPackageManager().getInstallerPackageName(getPackageName());
        return installer != null && validInstallers.contains(installer);
    }

    public boolean isActivated() {
        return false;
    }

    public boolean hasXposedInstaller() {
        return isActivated() || getXposedInstallerLaunchIntent() != null;
    }

    public Intent getXposedInstallerLaunchIntent() {
        return getPackageManager().getLaunchIntentForPackage("de.robv.android.xposed.installer");
    }

    @Override
    public void onClick(View v) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.hide_app_icon).setChecked(getPreferences().getBoolean(HIDE_APP_ICON, false));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.hide_app_icon) {
            boolean checked = !item.isChecked();
            item.setChecked(checked);
            getPreferences().edit().putBoolean(HIDE_APP_ICON, checked).apply();
            int mode = checked ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            getPackageManager().setComponentEnabledSetting(new ComponentName(this,PACKAGE_OWN + ".MainShortcut" ), mode, PackageManager.DONT_KILL_APP);
        }
        return super.onOptionsItemSelected(item);
    }
}
