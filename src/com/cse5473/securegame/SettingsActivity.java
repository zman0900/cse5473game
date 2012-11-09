package com.cse5473.securegame;

import java.util.Random;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	public static final String KEY_PREF_USERNAME = "pref_username";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);

		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		findPreference(KEY_PREF_USERNAME).setSummary(
				sharedPref.getString(KEY_PREF_USERNAME, ""));
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.d("prefs", "onSharedPreferenceChanged");
		if (key.equals(KEY_PREF_USERNAME)) {
			Preference usernamePref = findPreference(key);
			String username = sharedPreferences.getString(key, "");
			if (username.equals("")) {
				createRandomUsername(this);
				username = sharedPreferences.getString(key, "");
			}
			usernamePref.setSummary(username);
		}
	}

	public static void createRandomUsername(Context context) {
		Random r = new Random();
		String username = new String("android-"
				+ String.valueOf(r.nextInt(Integer.MAX_VALUE)));
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor e = sharedPref.edit();
		e.putString(KEY_PREF_USERNAME, username);
		e.commit();
		Toast.makeText(context, R.string.emptyusername, Toast.LENGTH_LONG)
				.show();
	}

	public static boolean isUsernameSet(Context context) {
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		return !sharedPref.getString(KEY_PREF_USERNAME, "").equals("");
	}
}
