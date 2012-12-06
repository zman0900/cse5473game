package com.cse5473.securegame;

import java.util.Random;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This class is simply to allow selection of a username when you first connect
 * to the bootstrap, while slightly pointless it give a nice athstetic feel to
 * the program.
 * 
 */
@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
	/**
	 * The string representation of the parameter of the username.
	 */
	public static final String KEY_PREF_USERNAME = "pref_username";

	/**
	 * On the creating of the activity it gets a default name for the user.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);

		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		findPreference(KEY_PREF_USERNAME).setSummary(
				sharedPref.getString(KEY_PREF_USERNAME, ""));
	}

	/**
	 * This method allows for the user to input a custom identifier as their
	 * name and handles the onResume portion of the activity.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	/**
	 * Handles if the activity is paused.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	/**
	 * Handles when the user already has a username and wishes to change it.
	 */
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

	/**
	 * Creates a random default username for the user provided that they didn't
	 * want to make a username based on the provided context.
	 * 
	 * @param context
	 *            The context to glean information from and create the username.
	 */
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

	/**
	 * Prompts for the user's desired username using an AlertDialog. If the user
	 * inputs a username and does not hit cancel it sets up teh peer.
	 * 
	 * @param main
	 *            The MainActivity to have the peer setup in.
	 */
	public static void promptForUsernameThenSetupPeer(MainActivity main) {
		AlertDialog.Builder alert = new AlertDialog.Builder(main);
		alert.setTitle(R.string.username);
		alert.setMessage(R.string.username_prompt);
		final EditText input = new EditText(main);
		alert.setView(input);
		alert.setCancelable(false);
		final MainActivity m = main;
		alert.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String username = input.getText().toString();
						if (username.equals("")) {
							createRandomUsername(m);
						} else {
							SharedPreferences sharedPref = PreferenceManager
									.getDefaultSharedPreferences(m);
							SharedPreferences.Editor e = sharedPref.edit();
							e.putString(KEY_PREF_USERNAME, input.getText()
									.toString());
							e.commit();
							Toast.makeText(m, R.string.username_set,
									Toast.LENGTH_LONG).show();
						}
						m.userNameSet();
					}
				});
		alert.show();
	}
	
	/**
	 * Checks to see if the username is already defined within the given
	 * context.
	 * 
	 * @param context
	 *            The context to glean information from.
	 * @return True iff the username is already defined.
	 */
	public static boolean isUsernameSet(Context context) {
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		return !sharedPref.getString(KEY_PREF_USERNAME, "").equals("");
	}

	/**
	 * Gets the username of the provided context.
	 * 
	 * @param context
	 *            the context to glean information from.
	 * @return the string representation of the username.
	 */
	public static String getUsername(Context context) {
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		return sharedPref.getString(KEY_PREF_USERNAME, "");
	}
}
