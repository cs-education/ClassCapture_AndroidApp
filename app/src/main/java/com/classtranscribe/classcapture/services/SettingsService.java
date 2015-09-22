package com.classtranscribe.classcapture.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.classtranscribe.classcapture.models.Section;

import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by sourabhdesai on 9/18/15.
 */
public class SettingsService {

    private static final String DEVICE_ID_KEY = "DEVICE_ID";

    private static String DeviceID; // Singleton representing device id, persisted in SharedPrefs

    /**
     * Get a Device ID that is unique to this phone...kinda
     * In reality, it is moreso linked with the users google account
     * So if user has a phone and a tablet both logged into the same google account, it will return the same ID
     * If for some reason the device is not linked with a google account, then a random UUID will be chosen and
     * persisted as the Device's ID
     * @param context current application context
     * @return String of the unique Device ID
     */
    public static String getDeviceID(Context context) {
        if (DeviceID != null) {
            // Check if the singleton has been loaded, if it has just return from memory
            return DeviceID;
        }

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean savedDeviceID = sharedPrefs.contains(DEVICE_ID_KEY);

        if (savedDeviceID) {
            String randomDefaultVal = String.valueOf(Math.random() * 10000);
            DeviceID = sharedPrefs.getString(DEVICE_ID_KEY, randomDefaultVal); // return the saved DeviceID
        } else {
            DeviceID = generateDeviceID(context);
            // Persist the DeviceID in SharedPrefs
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(DEVICE_ID_KEY, DeviceID);
            editor.apply();
        }

        return DeviceID;
    }

    private static String generateDeviceID(Context context) {
        // Android ID's are linked to the User's Google account,
        // so any device that is logged into their google account will have the same ID, pretty convenient
        String androidID = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        // If device is not logged into a google account, androidID would be null
        if (androidID == null) {
            // In this case, generate a random UUID and persist it in SharedPrefs
            androidID = UUID.randomUUID().toString();
        }

        return androidID;
    }

    public static boolean hasRegisteredForSections() {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Section> registeredSections = realm.allObjects(Section.class);
        realm.close();
        return !registeredSections.isEmpty();
    }
}
