/*
 * Copyright (c) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.tvleanback.data;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.example.android.tvleanback.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import static com.example.android.tvleanback.data.VideoContract.VideoEntry.COLUMN_VIDEO_URL;
import static com.example.android.tvleanback.data.VideoContract.VideoEntry.CURRENT_DB_LAST_VIDEO_STR;
import static com.example.android.tvleanback.data.VideoContract.VideoEntry.CURRENT_DB_WAS_REFRESHED;

/**
 * FetchVideoService is responsible for fetching the videos from the Internet and inserting the
 * results into a local SQLite database.
 */
public class FetchVideoService extends IntentService {
    private static final String TAG = "FetchVideoService";
    boolean wasRefreshed = false;
    String oldLastVideoStr = "";


    /**
     * Creates an IntentService with a default name for the worker thread.
     */
    public FetchVideoService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //oldVideosLength = prefs.getInt(CURRENT_DB_SIZE_KEY, 1);
        oldLastVideoStr = prefs.getString(CURRENT_DB_LAST_VIDEO_STR, "") + "";

        VideoDbBuilder builder = new VideoDbBuilder(getApplicationContext());

        try {
            List<ContentValues> contentValuesList = builder.fetch(getResources().getString(R.string.catalog_url), getResources().getString(R.string.videos_url));
            ContentValues[] downloadedVideoContentValues = contentValuesList.toArray(new ContentValues[contentValuesList.size()]);

            ContentValues lastVideo = downloadedVideoContentValues[downloadedVideoContentValues.length-1];
            String lastVideoStr = lastVideo.get(COLUMN_VIDEO_URL).toString();
            if (!lastVideoStr.equals(oldLastVideoStr)) {
                getApplicationContext().getContentResolver().bulkInsert(VideoContract.VideoEntry.CONTENT_URI, downloadedVideoContentValues);

                wasRefreshed = true;

                oldLastVideoStr = lastVideoStr;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(CURRENT_DB_LAST_VIDEO_STR, oldLastVideoStr);
                editor.putBoolean(CURRENT_DB_WAS_REFRESHED, wasRefreshed);
                editor.apply();
            }

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error occurred in downloading videos");
            e.printStackTrace();
        }
    }
}
