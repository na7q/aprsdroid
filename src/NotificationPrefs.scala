package org.aprsdroid.app

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.{PreferenceActivity, CheckBoxPreference}
import android.util.Log

class NotificationPrefs extends PreferenceActivity with SharedPreferences.OnSharedPreferenceChangeListener {

  lazy val prefs = new PrefsWrapper(this)

  def loadXml() {
    addPreferencesFromResource(R.xml.notifications)
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    loadXml()
    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this)

  }

  override def onDestroy() {
    super.onDestroy()
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this)
  }


  override def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String): Unit = {
  }
}
