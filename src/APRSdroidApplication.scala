package org.na7q.app

import android.app.Application

class APRSdroidApplication extends Application {

	override def onCreate() {
		super.onCreate()
		ServiceNotifier.instance.setupChannels(this)
		MapModes.initialize(this)
	}
}
