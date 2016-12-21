package com.livemasjid.livemasjidandroid.utils;

import android.net.Uri;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.ArrayList;
import android.util.Log;

import com.livemasjid.livemasjidandroid.bean.UriBean;
import com.livemasjid.livemasjidandroid.database.StreamDatabase;
import com.livemasjid.livemasjidandroid.transport.AbsTransport;
import com.livemasjid.livemasjidandroid.transport.TransportFactory;
import android.content.Context;

/**
 * Created by yusuf on 2016/12/21.
 */


public class MountsLoader extends AsyncTask<String, String, JSONObject> {
    private Context mContext;

    JSONParser jsonParser = new JSONParser();

    //private ProgressDialog pDialog;

    private static final String SERVER_URL = "http://livemajlis.com:8000/status-json.xsl";
    private static final String TAG_STATS = "icestats";
    private static final String TAG_SOURCE = "source";
    private static final String TAG_URL = "listenurl";

    /*@Override
    protected void onPreExecute() {
        pDialog = new ProgressDialog(MainActivity.this);
        pDialog.setMessage("Attempting login...");
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(true);
        pDialog.show();
    }*/

    public MountsLoader (Context context){
        mContext = context;
    }

    @Override
    protected JSONObject doInBackground(String... args) {

        try {

            HashMap<String, String> params = new HashMap<>();

            Log.d("request", "starting");

            JSONObject json = jsonParser.makeHttpRequest(
                    SERVER_URL, "GET", params);

            if (json != null) {
                Log.wtf("JSON result", json.toString());

                return json;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    protected void onPostExecute(JSONObject json) {

        JSONArray mounts;
        // Set up variables for API Call
        ArrayList<String> mount_url_list = new ArrayList<String>();
        String mount_url = "";

        /*if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
        }*/

        if (json != null) {

            try {
                JSONObject stats = json.getJSONObject(TAG_STATS);
                mounts = stats.getJSONArray(TAG_SOURCE);
                for (int i = 0; i < mounts.length(); i++) {
                    JSONObject jsonobject = mounts.getJSONObject(i);
                    if (jsonobject.has(TAG_URL)) {
                        mount_url = jsonobject.getString(TAG_URL);
                        addServerUri(mount_url, mount_url.substring(mount_url.lastIndexOf('/')+1));
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    private void addServerUri(String input, String nickname) {

        Uri uri = TransportFactory.getUri(input);

        StreamDatabase streamdb = new StreamDatabase(mContext);
        UriBean uriBean = TransportFactory.findUri(streamdb, uri);
        if (uriBean == null) {
            uriBean = TransportFactory.getTransport(uri.getScheme()).createUri(uri);

            if (!nickname.equals("")) {
                uriBean.setNickname(nickname);
            }

            AbsTransport transport = TransportFactory.getTransport(uriBean.getProtocol());
            transport.setUri(uriBean);
            streamdb.saveUri(uriBean);

        }
        streamdb.close();
    }

}