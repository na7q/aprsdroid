package org.na7q.app

import android.Manifest

object LocationSource {
	val DEFAULT_CONNTYPE = "smartbeaconing"

	def instanciateLocation(service : AprsService, prefs : PrefsWrapper) : LocationSource = {
		prefs.getString("loc_source", DEFAULT_CONNTYPE) match {
			case "smartbeaconing" => new SmartBeaconing(service, prefs)
			case "periodic" => new PeriodicGPS(service, prefs)
			case "manual" => new FixedPosition(service, prefs)
		}
		
	}
	def instanciatePrefsAct(prefs : PrefsWrapper) = {
		val isMetric = prefs.getString("p.units", "1") == "1"
		prefs.getString("loc_source", DEFAULT_CONNTYPE) match {
			case "smartbeaconing" => if (isMetric) R.xml.location_smartbeaconing else R.xml.location_smartbeaconing_imperial
			case "periodic" => if (isMetric) R.xml.location_periodic else R.xml.location_periodic_imperial
			case "manual" => R.xml.location_manual
		}
	}
	def getPermissions(prefs : PrefsWrapper) = {
		prefs.getString("loc_source", DEFAULT_CONNTYPE) match {
			case "smartbeaconing" => Set(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
			case "periodic" => Set(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
			case "manual" => Set()
		}
	}
}

abstract class LocationSource {
	// the start function might be called multiple times!
	def start(singleShot : Boolean) : String
	def stop()
}
