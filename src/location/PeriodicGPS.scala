package org.na7q.app

import _root_.android.content.Context
import _root_.android.location._
import _root_.android.os.{Bundle, Handler}
import _root_.android.util.Log
import _root_.android.widget.Toast

object PeriodicGPS {

	def bestProvider(locman : LocationManager) = {
		val cr = new Criteria()
		cr.setAccuracy(Criteria.ACCURACY_FINE)
		Log.d("FAILGPS", "all providers " + locman.getAllProviders())
		Log.d("FAILGPS", "best. provider. ever. " + locman.getBestProvider(cr, false))
		locman.getBestProvider(cr, false)
	}

	def bestProvider(context : Context) : String = {
		bestProvider(context.getSystemService(Context.LOCATION_SERVICE).asInstanceOf[LocationManager])
	}
}

class PeriodicGPS(service : AprsService, prefs : PrefsWrapper) extends LocationSource
		with LocationListener {
	val TAG = "APRSdroid.PeriodicGPS"

	val FAST_LANE_ACT = 30000

	lazy val locMan = service.getSystemService(Context.LOCATION_SERVICE).asInstanceOf[LocationManager]

	var lastLoc : Location = null
	var fastLaneLoc : Location = null

	def start(singleShot : Boolean) = {
		fastLaneLoc = null
		lastLoc = null
		stop()
		requestLocations(singleShot)
		service.getString(R.string.p_source_periodic)
	}

	def requestLocations(stay_on : Boolean) {
		// get update interval and distance
		val upd_int = prefs.getStringInt("interval", 10)
		val upd_dist = prefs.getStringInt("distance", 10)
		val gps_act = prefs.getString("gps_activation", "med")
		try {
		if (stay_on || (gps_act == "always")) {
			locMan.requestLocationUpdates(PeriodicGPS.bestProvider(locMan),
				0, 0, this)
		} else {
			// for GPS precision == medium, we use getGpsInterval()
			locMan.requestLocationUpdates(PeriodicGPS.bestProvider(locMan),
				upd_int * 60000 - getGpsInterval(), upd_dist * 1000, this)
		}
		if (prefs.getBoolean("netloc", false)) {
			locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
				upd_int * 60000, upd_dist * 1000, this)
		}
		} catch {
			case e @ (_: IllegalArgumentException | _: SecurityException) =>
				// we lack GPS or GPS permissions
				service.postAbort(service.getString(R.string.service_no_location)
					+ "\n" + e.getMessage())
		}
	}


	def stop() {
		locMan.removeUpdates(this);
	}

	def getGpsInterval() : Int = {
		val gps_act = prefs.getString("gps_activation", "med")
		if (gps_act == "med") FAST_LANE_ACT
				else  0
	}

	def startFastLane() {
		import AprsService.block2runnable
		Log.d(TAG, "switching to fast lane");
		// request fast update rate
		locMan.removeUpdates(this);
		requestLocations(true)
		service.handler.postDelayed({ stopFastLane(true) }, FAST_LANE_ACT)
	}

	def stopFastLane(post : Boolean) {
		if (!AprsService.running)
			return;
		Log.d(TAG, "switching to slow lane");
		if (post && fastLaneLoc != null) {
			Log.d(TAG, "stopFastLane: posting " + fastLaneLoc);
			postLocation(fastLaneLoc)
		}
		fastLaneLoc = null
		// reset update speed
		locMan.removeUpdates(this);
		requestLocations(false)
	}

	def goingFastLane(location : Location) : Boolean = {
		if (fastLaneLoc == null) {
			// need to set fastLaneLoc before re-requesting locations
			fastLaneLoc = location
			startFastLane()
		} else
			fastLaneLoc = location
		return true
	}

	// LocationListener interface
	override def onLocationChanged(location : Location) {
		val upd_int = prefs.getStringInt("interval", 10) * 60000
		val upd_dist = prefs.getStringInt("distance", 10) * 1000
		if (lastLoc != null &&
		    (location.getTime - lastLoc.getTime < (upd_int  - getGpsInterval()) ||
		     location.distanceTo(lastLoc) < upd_dist)) {
			//Log.d(TAG, "onLocationChanged: ignoring premature location")
			return
		}
		// check if we need to go fast lane
		val gps_act = prefs.getString("gps_activation", "med")
		if (gps_act == "med" && location.getProvider == LocationManager.GPS_PROVIDER) {
			if (goingFastLane(location))
				return
		}
		postLocation(location)
	}

	override def onProviderDisabled(provider : String) {
		Log.d(TAG, "onProviderDisabled: " + provider)
		val netloc_available = locMan.getProviders(true).contains(LocationManager.NETWORK_PROVIDER)
		val netloc_usable = netloc_available && prefs.getBoolean("netloc", false)
		if (provider == LocationManager.GPS_PROVIDER &&
			netloc_usable == false) {
			// GPS was our last data source, we have to complain!
			Toast.makeText(service, R.string.service_no_location, Toast.LENGTH_LONG).show()
		}
	}
	override def onProviderEnabled(provider : String) {
		Log.d(TAG, "onProviderEnabled: " + provider)
	}
	override def onStatusChanged(provider : String, st: Int, extras : Bundle) {
		Log.d(TAG, "onStatusChanged: " + provider)
	}


	def postLocation(location : Location) {
		lastLoc = location

		service.postLocation(location)
	}
}
