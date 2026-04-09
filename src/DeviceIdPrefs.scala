package org.aprsdroid.app

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.{EditTextPreference, PreferenceActivity}
import android.widget.Toast

class DeviceIdPrefs extends PreferenceActivity with SharedPreferences.OnSharedPreferenceChangeListener {

  lazy val prefs = new PrefsWrapper(this)

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    addPreferencesFromResource(R.xml.device_id_prefs)
    getPreferenceScreen().getSharedPreferences()
      .registerOnSharedPreferenceChangeListener(this)
    updateUrlSummary()

    // "Update now" — download tocalls.yaml immediately
    findPreference("device_id_update_now").setOnPreferenceClickListener(
      new android.preference.Preference.OnPreferenceClickListener {
        override def onPreferenceClick(pref: android.preference.Preference): Boolean = {
          DeviceDbUpdater.update(DeviceIdPrefs.this, prefs, silent = false)
          true
        }
      }
    )

    // "Reset to defaults" — restore the URL to the official source
    findPreference("device_id_reset_url").setOnPreferenceClickListener(
      new android.preference.Preference.OnPreferenceClickListener {
        override def onPreferenceClick(pref: android.preference.Preference): Boolean = {
          val editor = getPreferenceScreen().getSharedPreferences().edit()
          editor.putString("device_id_url", DeviceDbUpdater.DEFAULT_TOCALLS_URL)
          editor.apply()
          updateUrlSummary()
          Toast.makeText(DeviceIdPrefs.this, R.string.device_id_url_reset,
                         Toast.LENGTH_SHORT).show()
          true
        }
      }
    )
  }

  override def onDestroy() {
    super.onDestroy()
    getPreferenceScreen().getSharedPreferences()
      .unregisterOnSharedPreferenceChangeListener(this)
  }

  override def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String): Unit = {
    if (key == "device_id_url") updateUrlSummary()
  }

  // Show the current URL as the summary so the user can see what's set.
  private def updateUrlSummary(): Unit = {
    val urlPref = findPreference("device_id_url").asInstanceOf[EditTextPreference]
    val current = prefs.getString("device_id_url", DeviceDbUpdater.DEFAULT_TOCALLS_URL)
    urlPref.setSummary(if (current.isEmpty) DeviceDbUpdater.DEFAULT_TOCALLS_URL else current)
  }
}
