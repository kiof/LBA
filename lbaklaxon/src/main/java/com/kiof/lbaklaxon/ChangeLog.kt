/**
 * Copyright (C) 2011, Karsten Priegnitz
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * @author: Karsten Priegnitz
 * @see: http://code.google.com/p/android-change-log/
 */
package com.kiof.lbaklaxon

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.preference.PreferenceManager

class ChangeLog(context: Context) {
    /**
     * @return The version name of the last installation of this app (as
     * described in the former manifest). This will be the same as
     * returned by `getThisVersion()` from the second time
     * this version of the app is launched (more precisely: the second
     * time ChangeLog is instantiated).
     */
    /**
     * manually set the last version name - for testing purposes only
     *
     * @param lastVersion
     */
    var lastVersion: String?

    /**
     * @return The version name of this app as described in the manifest.
     */
    var thisVersion: String? = null
        private set

    /**
     * Constructor
     *
     *
     * Retrieves the version names and stores the new version name in
     * SharedPreferences
     */
    init {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)

        // get version numbers
        this.lastVersion = sp.getString(VERSION_KEY, "")
        Log.d(TAG, "lastVersion: $lastVersion")
        try {
            this.thisVersion =
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            this.thisVersion = "?"
            Log.e(TAG, "could not get version name from manifest!")
            e.printStackTrace()
        }
        Log.d(TAG, "appVersion: " + this.thisVersion)

        // save new version number to preferences
        val editor = sp.edit()
        editor.putString(VERSION_KEY, this.thisVersion)
        editor.commit()
    }

    /**
     * @return `true` if this version of your app is started the
     * first time
     */
    fun firstRun(): Boolean {
        return this.lastVersion != this.thisVersion
    }

    /**
     * @return `true` if your app is started the first time ever.
     * Also `true` if your app was deinstalled and installed
     * again.
     */
    fun firstRunEver(): Boolean {
        return "" == this.lastVersion
    }

    companion object {
        private const val TAG = "ChangeLog"

        // this is the key for storing the version name in SharedPreferences
        private const val VERSION_KEY = "PREFS_VERSION_KEY"
    }
}