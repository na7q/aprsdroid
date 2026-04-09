package org.aprsdroid.app

import _root_.android.content.Context
import _root_.android.util.Log
import _root_.android.widget.Toast

import java.io.{BufferedInputStream, FileOutputStream}
import java.net.URL

// Downloads tocalls.yaml from a configurable URL and saves it to internal storage.
// All network I/O runs on a background thread.
object DeviceDbUpdater {
  val TAG = "APRSdroid.DeviceDbUpdater"

  val DEFAULT_TOCALLS_URL =
    "https://raw.githubusercontent.com/aprsorg/aprs-deviceid/main/tocalls.yaml"

  // Returns the URL from preferences, falling back to the default.
  def getUrl(prefs: PrefsWrapper): String = {
    val url = prefs.getString("device_id_url", "").trim
    if (url.isEmpty) DEFAULT_TOCALLS_URL else url
  }

  // Downloads tocalls.yaml if the preferences allow it right now.
  def updateIfAllowed(context: Context, prefs: PrefsWrapper): Unit = {
    val autoUpdate = prefs.getBoolean("device_id_auto_update", true)
    if (!autoUpdate) return
    update(context, prefs, silent = true)
  }

  // Downloads tocalls.yaml unconditionally and saves it to internal storage.
  // Pass silent=false to show a Toast when done (for manual "Update now" taps).
  def update(context: Context, prefs: PrefsWrapper, silent: Boolean = false): Unit = {
    val appContext = context.getApplicationContext
    val urlString  = getUrl(prefs)

    new Thread(new Runnable {
      override def run(): Unit = {
        try {
          Log.i(TAG, "Downloading tocalls.yaml from " + urlString)
          val url        = new URL(urlString)
          val connection = url.openConnection()
          connection.setConnectTimeout(15000)
          connection.setReadTimeout(30000)
          connection.connect()

          val outFile = DeviceIdentifier.tocallsFile(appContext)
          val tmpFile = new java.io.File(outFile.getParent, "tocalls.yaml.tmp")

          val input  = new BufferedInputStream(connection.getInputStream)
          val output = new FileOutputStream(tmpFile)
          try {
            val buf = new Array[Byte](8192)
            var len = input.read(buf)
            while (len != -1) {
              output.write(buf, 0, len)
              len = input.read(buf)
            }
          } finally {
            output.close()
            input.close()
          }

          // Atomic replace: rename tmp → final
          tmpFile.renameTo(outFile)
          DeviceIdentifier.invalidate()
          Log.i(TAG, "tocalls.yaml updated successfully")

          if (!silent) {
            new android.os.Handler(appContext.getMainLooper).post(new Runnable {
              override def run(): Unit =
                Toast.makeText(appContext, R.string.device_id_update_success,
                               Toast.LENGTH_SHORT).show()
            })
          }
        } catch {
          case e: Exception =>
            Log.e(TAG, "Failed to download tocalls.yaml", e)
            if (!silent) {
              new android.os.Handler(appContext.getMainLooper).post(new Runnable {
                override def run(): Unit =
                  Toast.makeText(appContext,
                    appContext.getString(R.string.device_id_update_failed, e.getMessage),
                    Toast.LENGTH_LONG).show()
              })
            }
        }
      }
    }).start()
  }
}
