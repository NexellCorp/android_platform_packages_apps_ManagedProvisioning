/*
 * Copyright 2014, The Android Open Source Project
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
package com.android.managedprovisioning;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Xml;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/**
 * Helper class to load/save resume information from Intents into a SharedPreferences.
 */
public class IntentStore {
    private SharedPreferences mPrefs;
    private String mPrefsName; // Name of the file where mPrefs is stored.
    private Context mContext;
    private ComponentName mIntentTarget;

    // Key arrays should never be null.
    private String[] mStringKeys = new String[0];
    private String[] mLongKeys = new String[0];
    private String[] mIntKeys = new String[0];
    private String[] mBooleanKeys = new String[0];
    private String[] mPersistableBundleKeys = new String[0];

    private static final String TAG_PERSISTABLEBUNDLE = "persistable_bundle";

    private static final String IS_SET = "isSet";

    public IntentStore(Context context, ComponentName intentTarget, String preferencesName) {
        mContext = context;
        mPrefsName = preferencesName;
        mPrefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
        mIntentTarget = intentTarget;
    }

    public IntentStore setStringKeys(String[] keys) {
        mStringKeys = (keys == null) ? new String[0] : keys;
        return this;
    }

    public IntentStore setLongKeys(String[] keys) {
        mLongKeys = (keys == null) ? new String[0] : keys;
        return this;
    }

    public IntentStore setIntKeys(String[] keys) {
        mIntKeys = (keys == null) ? new String[0] : keys;
        return this;
    }

    public IntentStore setBooleanKeys(String[] keys) {
        mBooleanKeys = (keys == null) ? new String[0] : keys;
        return this;
    }

    public IntentStore setPersistableBundleKeys(String[] keys) {
        mPersistableBundleKeys = (keys == null) ? new String[0] : keys;
        return this;
    }

    public void clear() {
        mPrefs.edit().clear().commit();
    }

    public void save(Bundle data){
        SharedPreferences.Editor editor = mPrefs.edit();

        editor.clear();
        for (String key : mStringKeys) {
            editor.putString(key, data.getString(key));
        }
        for (String key : mLongKeys) {
            editor.putLong(key, data.getLong(key));
        }
        for (String key : mIntKeys) {
            editor.putInt(key, data.getInt(key));
        }
        for (String key : mBooleanKeys) {
            editor.putBoolean(key, data.getBoolean(key));
        }
        for (String key : mPersistableBundleKeys) {

            // Cast should be guaranteed to succeed by check in the provisioning activities.
            String bundleString = persistableBundleToString((PersistableBundle) data
                    .getParcelable(key));
            if (bundleString != null) {
                editor.putString(key, bundleString);
            }
        }
        editor.putBoolean(IS_SET, true);
        editor.commit();
    }

    public Intent load() {
        if (!mPrefs.getBoolean(IS_SET, false)) {
            return null;
        }

        Intent result = new Intent();
        result.setComponent(mIntentTarget);

        for (String key : mStringKeys) {
            String value = mPrefs.getString(key, null);
            if (value != null) {
                result.putExtra(key, value);
            }
        }
        for (String key : mLongKeys) {
            if (mPrefs.contains(key)) {
                result.putExtra(key, mPrefs.getLong(key, 0));
            }
        }
        for (String key : mIntKeys) {
            if (mPrefs.contains(key)) {
                result.putExtra(key, mPrefs.getInt(key, 0));
            }
        }
        for (String key : mBooleanKeys) {
            if (mPrefs.contains(key)) {
                result.putExtra(key, mPrefs.getBoolean(key, false));
            }
        }
        for (String key : mPersistableBundleKeys) {
            if (mPrefs.contains(key)) {
                PersistableBundle bundle = stringToPersistableBundle(mPrefs.getString(key, null));
                if (bundle != null) {
                    result.putExtra(key, bundle);
                }
            }
        }

        return result;
    }

    private String persistableBundleToString(PersistableBundle bundle) {
        if (bundle == null) {
            return null;
        }

        StringWriter writer = new StringWriter();
        XmlSerializer serializer = Xml.newSerializer();
        try {
            serializer.setOutput(writer);
            serializer.startDocument(null, true);
            serializer.startTag(null, TAG_PERSISTABLEBUNDLE);
            bundle.saveToXml(serializer);
            serializer.endTag(null, TAG_PERSISTABLEBUNDLE);
            serializer.endDocument();
        } catch (IOException|XmlPullParserException e) {
            ProvisionLogger.loge("Persistable bundle could not be stored as string.", e);
            return null;
        }

        return writer.toString();
    }

    private PersistableBundle stringToPersistableBundle(String string) {
        if (string == null) {
            return null;
        }

        XmlPullParserFactory factory;
        XmlPullParser parser;
        try {
            factory = XmlPullParserFactory.newInstance();

            parser = factory.newPullParser();
            parser.setInput(new StringReader(string));

            if (parser.next() == XmlPullParser.START_TAG) {
                if (TAG_PERSISTABLEBUNDLE.equals(parser.getName())) {
                    return PersistableBundle.restoreFromXml(parser);
                }
            }
        } catch (IOException|XmlPullParserException e) {
            ProvisionLogger.loge(e);
            // Fall through.
        }
        ProvisionLogger.loge("Persistable bundle could not be restored from string " + string);
        return null;
    }
}
