/*
 * ServeStream: A HTTP stream browser/player for Android
 * Copyright 2014 William Seemann
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

import com.livemasjid.livemasjidandroid.fragment.UriEditorFragment;
import com.livemasjid.livemasjidandroid.preference.UserPreferences;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

public class UriEditorActivity extends ActionBarActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
		super.onCreate(savedInstanceState);
		
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		if (savedInstanceState == null) {
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			transaction.add(android.R.id.content, new UriEditorFragment());
			transaction.commit();
		}
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
    		case android.R.id.home:
    			Intent intent = new Intent(this, MainActivity.class);
    			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    			startActivity(intent);
    			return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}