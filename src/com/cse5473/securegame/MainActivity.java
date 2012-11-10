package com.cse5473.securegame;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.os.Bundle;
import android.os.StrictMode;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {

	private static int DEFAULT_PORT = 12345;

	private PeerManager peer;
	private String username;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// Set a random username if none set
		if (!SettingsActivity.isUsernameSet(this)) {
			SettingsActivity.createRandomUsername(this);
		}

		setupPeer();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void setupPeer() {
		username = SettingsActivity.getUsername(this);
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			peer = new PeerManager(new String(md.digest(username.getBytes())),
					username, DEFAULT_PORT);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

}
