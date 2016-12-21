/*
 * ServeStream: A HTTP stream browser/player for Android
 * Copyright 2016 William Seemann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.livemasjid.livemasjidandroid.activity;

import com.livemasjid.livemasjidandroid.adapter.NavigationDrawerAdapter.ItemAccess;
import com.livemasjid.livemasjidandroid.fragment.AlarmClockFragment;
import com.livemasjid.livemasjidandroid.fragment.BrowseFragment;
import com.livemasjid.livemasjidandroid.fragment.UrlListFragment;
import com.livemasjid.livemasjidandroid.fragment.UrlListFragment.BrowseIntentListener;
import com.livemasjid.livemasjidandroid.utils.DownloadScannerDialog;
import com.livemasjid.livemasjidandroid.utils.MusicUtils;
import com.livemasjid.livemasjidandroid.utils.MusicUtils.ServiceToken;
import com.livemasjid.livemasjidandroid.utils.MountsLoader;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.livemasjid.livemasjidandroid.R;

public class MainActivity extends AppCompatActivity implements
			ServiceConnection,
			BrowseIntentListener, ItemAccess,
		NavigationView.OnNavigationItemSelectedListener {

	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	private final static String DOWNLOAD_SCANNER_DIALOG = "download_scanner_dialog";

	private final static int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0;

	private String mTag;

    private CharSequence mTitle;

	private ServiceToken mToken;

    private int mCurrentSelectedPosition = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//setTheme(UserPreferences.getTheme());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		android.support.v7.app.ActionBarDrawerToggle toggle = new android.support.v7.app.ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.setDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		if (savedInstanceState == null) {
			openUri(getUri());

			navigationView = (NavigationView) findViewById(R.id.nav_view);
			navigationView.setCheckedItem(R.id.nav_urls);
			//selectItem(0);
		}

		mToken = MusicUtils.bindToService(this, this);

		requestPermission(this);

        MountsLoader loader = new MountsLoader(this);
        loader.execute();
	}

    @Override
    public void onNewIntent(Intent intent) {
    	super.onNewIntent(intent);

        setIntent(intent);

    	if (intent.hasExtra("restart_app")) {
    		Intent i = getIntent();
    		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		finish();
    		startActivity(i);
    	} else {
    		openUri(getUri());
    	}
    }

	@Override
	public void onDestroy() {
		super.onDestroy();

        MusicUtils.unbindFromService(mToken);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			mTag = savedInstanceState.getString(STATE_SELECTED_NAVIGATION_ITEM);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// Serialize the current dropdown position.
		outState.putString(STATE_SELECTED_NAVIGATION_ITEM, mTag);
	}

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         // The action bar home/up action should open or close the drawer.
         // ActionBarDrawerToggle will take care of this.

        // Handle action buttons
        switch (item.getItemId()) {
			case (R.id.menu_item_settings):
				startActivity(new Intent(this, SettingsActivity.class));
				return true;
			case (R.id.menu_item_scan):
				try {
					Intent intent = new Intent("com.google.zxing.client.android.SCAN");
					intent.setPackage("com.google.zxing.client.android");
					intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
					startActivityForResult(intent, 0);
				} catch (ActivityNotFoundException ex) {
					showDialog(DOWNLOAD_SCANNER_DIALOG);
				}
        		return true;
			default:
				return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void startActivity(Intent intent) {
        // check if search intent
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
        	Fragment fragment = getSupportFragmentManager().findFragmentByTag(mTag);
        	if (fragment != null && fragment instanceof BrowseFragment) {
    			intent.putParcelableArrayListExtra("uris", ((BrowseFragment) fragment).getUris());
        	}
        }

        super.startActivity(intent);
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	Fragment fragment = getSupportFragmentManager().findFragmentByTag(mTag);
			if (fragment != null && fragment instanceof BrowseFragment) {
				((BrowseFragment) fragment).onBackKeyPressed();
				return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    if (requestCode == 0) {
	        if (resultCode == RESULT_OK) {
	            String contents = intent.getStringExtra("SCAN_RESULT");
	            // Handle successful scan
	            openUri(contents);
	        } else if (resultCode == RESULT_CANCELED) {
	            // Handle cancel
	        }
	    }
	}

    private void selectItem(int position) {
    	mCurrentSelectedPosition = position;

    	FragmentManager fragmentManager = getSupportFragmentManager();
    	Fragment fragment = getSupportFragmentManager().findFragmentByTag(mTag);

    	if (fragment != null) {
    		fragmentManager.beginTransaction().detach(fragment).commit();
    	}

    	String tag = String.valueOf(position);
    	fragment = getSupportFragmentManager().findFragmentByTag(tag);

    	if (fragment == null) {
            if (position == 0) {
            	fragment = new UrlListFragment();
            	fragment.setArguments(new Bundle());
            } else if (position == 1) {
            	fragment = new BrowseFragment();
            	fragment.setArguments(new Bundle());
            } else if (position == 2) {
            	fragment = new AlarmClockFragment();
            }

    		fragmentManager.beginTransaction().add(R.id.content_frame, fragment, tag).commit();
    	} else {
    		fragmentManager.beginTransaction().attach(fragment).commit();
    	}

    	mTag = tag;
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
				SharedPreferences preferences = getSharedPreferences("Music", MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
				SharedPreferences.Editor editor = preferences.edit();

				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					// permission was granted, yay! Do the
					// contacts-related task you need to do.
					editor.putBoolean("read_external_storage", true);
				} else {

					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					editor.putBoolean("read_external_storage", false);
				}

				editor.commit();
				return;
			}

			// other 'case' lines to check for other
			// permissions this app might request
		}
	}

	private void requestPermission(Activity activity) {
		// Here, thisActivity is the current activity
		if (ContextCompat.checkSelfPermission(activity,
				Manifest.permission.READ_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {

			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
					Manifest.permission.READ_EXTERNAL_STORAGE)) {

				// Show an expanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.

			} else {

				// No explanation needed, we can request the permission.

				ActivityCompat.requestPermissions(activity,
						new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
						MainActivity.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

				// MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
				// app-defined int constant. The callback method gets the
				// result of the request.
			}
		}
	}

    private String getUri() {
		String intentUri = null;
		String contentType = null;

		Intent intent = getIntent();

		if (intent == null) {
			return null;
		}

        // check to see if we were called from a home screen shortcut
		if ((contentType = intent.getType()) != null) {
			if (contentType.contains("com.livemasjid.livemasjidandroid/")) {
				intentUri = intent.getType().toString().replace("com.livemasjid.livemasjidandroid/", "");
				setIntent(null);
				return intentUri;
			}
		}

		// check to see if we were called by clicking on a URL
		if (intent.getData() != null) {
			intentUri = intent.getData().toString();
		}

		// check to see if the application was opened from a share intent
		if (intent.getExtras() != null && intent.getExtras().getCharSequence(Intent.EXTRA_TEXT) != null) {
			intentUri = intent.getExtras().getCharSequence(Intent.EXTRA_TEXT).toString();
		}

		setIntent(null);

		return intentUri;
    }

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		//MusicUtils.updateNowPlaying(this);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		finish();
	}

	private void openUri(String uri) {
		mCurrentSelectedPosition = 0;

		Bundle args = new Bundle();
		args.putString(UrlListFragment.ARG_TARGET_URI, uri);

    	FragmentManager fragmentManager = getSupportFragmentManager();
    	Fragment fragment = getSupportFragmentManager().findFragmentByTag(mTag);

    	if (fragment != null) {
    		fragmentManager.beginTransaction().detach(fragment).commit();
    	}

    	String tag = String.valueOf(0);
    	fragment = getSupportFragmentManager().findFragmentByTag(tag);

    	if (fragment == null) {
           	fragment = new UrlListFragment();
           	fragment.setArguments(args);
    		fragmentManager.beginTransaction().add(R.id.content_frame, fragment, tag).commit();
    	} else {
           	fragment.getArguments().putString(UrlListFragment.ARG_TARGET_URI, uri);
    		fragmentManager.beginTransaction().attach(fragment).commit();
    	}

    	mTag = tag;
	}

	@Override
	public void browseToUri(Uri uri) {
		mCurrentSelectedPosition = 1;

		Bundle args = new Bundle();
		args.putString(UrlListFragment.ARG_TARGET_URI, uri.toString());

    	FragmentManager fragmentManager = getSupportFragmentManager();
    	Fragment fragment = getSupportFragmentManager().findFragmentByTag(mTag);

    	if (fragment != null) {
    		fragmentManager.beginTransaction().detach(fragment).commit();
    	}

    	String tag = String.valueOf(1);
    	fragment = getSupportFragmentManager().findFragmentByTag(tag);

    	if (fragment == null) {
           	fragment = new BrowseFragment();
           	fragment.setArguments(args);
    		fragmentManager.beginTransaction().add(R.id.content_frame, fragment, tag).commit();
    	} else {
           	fragment.getArguments().putString(UrlListFragment.ARG_TARGET_URI, uri.toString());
    		fragmentManager.beginTransaction().attach(fragment).commit();
    	}

    	mTag = tag;

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setCheckedItem(R.id.nav_browse);
	}

	private void showDialog(String tag) {
		// DialogFragment.show() will take care of adding the fragment
		// in a transaction.  We also want to remove any currently showing
		// dialog, so make our own transaction and take care of that here.
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag(tag);
		if (prev != null) {
			ft.remove(prev);
		}

		DialogFragment newFragment = null;

		// Create and show the dialog.
		newFragment = DownloadScannerDialog.newInstance();

		ft.add(0, newFragment, tag);
		ft.commit();
	}

	@Override
	public int getSelectedItemIndex() {
		return mCurrentSelectedPosition;
	}

	@Override
	public void onBackPressed() {
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	@SuppressWarnings("StatementWithEmptyBody")
	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		// Handle navigation view item clicks here.
		int id = item.getItemId();

		if (id == R.id.nav_urls) {
			selectItem(0);
		} else if (id == R.id.nav_browse) {
			selectItem(1);
		} else if (id == R.id.nav_alarm_clock) {
			selectItem(2);
		}

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return true;
	}
}