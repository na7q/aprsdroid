package org.aprsdroid.app

import android.app.Application

class APRSdroidApplication extends Application {

	override def onCreate() {
		super.onCreate()
		MapModes.applyGoogleMapsKey(this)
		ServiceNotifier.instance.setupChannels(this)
		MapModes.initialize(this)
	}
}
