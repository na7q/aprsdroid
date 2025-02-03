package org.aprsdroid.app

import _root_.android.content.Context
import _root_.android.os.{Handler, Looper}
import _root_.net.ab0oo.aprs.parser._
import java.util.Date

class CustomPacketService(service: AprsService, prefs: PrefsWrapper) {

  private var handler: Handler = _
  private var runnable: Runnable = _

  // Method to start the custom packet loop
  def startCustomPacketLoop(): Unit = {
    // Retrieve the interval from preferences (in milliseconds)
    val interval = 3000 //prefs.getInt("custompacketinterval", 30000) // Default to 30 seconds if not set
    
    // Create the Runnable that sends the custom packet
    runnable = new Runnable {
      override def run(): Unit = {
        startCustomPacket() // Call the method to send the packet
        // Reschedule the task after the specified interval
        handler.postDelayed(this, interval)
      }
    }

    // Initialize the Handler if it's not already initialized
    if (handler == null) {
      handler = new Handler(Looper.getMainLooper())  // Ensure it's using the main thread's looper
    }

    // Start the loop by posting the first task
    handler.postDelayed(runnable, interval)
  }

  // Method to stop the custom packet loop
  def stopCustomPacketLoop(): Unit = {
    if (handler != null && runnable != null) {
      handler.removeCallbacks(runnable)  // Remove any pending tasks
    }
  }

  // Method to send a custom packet
  private def startCustomPacket(): Unit = {
    if (prefs.getBoolean("objects", false)) {
      val packet = prefs.getString("object_value", "")
      service.sendcustomPacket(packet)
    }
  }
}
