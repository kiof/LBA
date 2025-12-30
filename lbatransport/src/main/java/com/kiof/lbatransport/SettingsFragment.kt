package com.kiof.lbatransport

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        // Get Preferences
        PreferenceManager.setDefaultValues(requireContext(), R.xml.settings, false)
        // 2. Initialize the property
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        // Get Preferences and log them
        sharedPreferences.all.forEach {
            Log.d(MainActivity.Companion.TAG, "${it.key} -> ${it.value}")
        }
    }
}