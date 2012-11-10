package com.cse5473.securegame;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final int DEFAULT_PORT = 12345;
	private static final String LOG_TAG = "main";

	private PeerManager peer;
	private String username;
	private ArrayAdapter<String> adapter;

	private TextView message;
	private ListView listView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		message = (TextView) findViewById(R.id.message);
		listView = (ListView) findViewById(R.id.listView);

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
	protected void onDestroy() {
		super.onDestroy();
		Log.d(LOG_TAG, "onDestroy");
		peer.halt();
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
		case R.id.menu_refresh:
			if (peer != null) {
				peer.doBootstrap();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private static class MainHandler extends Handler {
		private static MainActivity main;

		public MainHandler(MainActivity m) {
			main = m;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PeerManager.RECEIVED_PEER_LIST:
				main.message.setVisibility(View.INVISIBLE);
				main.adapter = new ArrayAdapter<String>(main,
						android.R.layout.simple_list_item_1,
						main.peer.getPeerList());
				main.listView.setAdapter(main.adapter);
				Log.i(LOG_TAG, "peer list ready");
				break;
			case PeerManager.INIT_FAILED:
				Log.e(LOG_TAG, "init failed!");
				main.message.setVisibility(View.INVISIBLE);
				break;
			case PeerManager.PEER_LIST_UPDATED:
				main.adapter = new ArrayAdapter<String>(main,
						android.R.layout.simple_list_item_1,
						main.peer.getPeerList());
				main.listView.setAdapter(main.adapter);
				Log.i(LOG_TAG, "peer list updated");
				break;
			}
		}
	};

	private void setupPeer() {
		username = SettingsActivity.getUsername(this);
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			peer = new PeerManager(new String(md.digest(username.getBytes())),
					username, DEFAULT_PORT, new MainHandler(this), this);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

}
