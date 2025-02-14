package org.aprsdroid.app

import android.bluetooth.{BluetoothAdapter, BluetoothDevice}
import android.bluetooth.le.{ScanCallback, ScanResult}
import android.util.AttributeSet
import _root_.android.content.{Context, SharedPreferences, DialogInterface}
import _root_.android.preference.{Preference, PreferenceManager}
import android.app.AlertDialog
import android.bluetooth.le.BluetoothLeScanner
import android.os.Handler
import android.os.Looper
import android.content.pm.PackageManager
import android.util.Log
import android.widget.ArrayAdapter

class BleScannerPreference(context: Context, attrs: AttributeSet) extends Preference(context, attrs) {

  private val bluetoothAdapter: Option[BluetoothAdapter] = Option(BluetoothAdapter.getDefaultAdapter)
  private val handler = new Handler(Looper.getMainLooper())
  private var bleScanner: Option[BluetoothLeScanner] = bluetoothAdapter.map(_.getBluetoothLeScanner)
  private var scanResults: Map[String, BluetoothDevice] = Map()

  init()

  private def init(): Unit = {
    setOnPreferenceClickListener(new Preference.OnPreferenceClickListener {
      override def onPreferenceClick(preference: Preference): Boolean = {
        startBleScan()
        true
      }
    })
  }

	private def startBleScan(): Unit = {
	  bluetoothAdapter match {
		case Some(adapter) if adapter.isEnabled =>
		  bleScanner.foreach { scanner =>
			val deviceList = scala.collection.mutable.ListBuffer.empty[BluetoothDevice]

			// Check permissions
			if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			  Log.e("BleScanner", "Location permission not granted.")
			  return
			}

			// Show "Scanning..." dialog first
			val scanningDialog = new AlertDialog.Builder(context)
			  .setTitle("Scanning for Devices")
			  .setMessage("Please wait...")
			  .setCancelable(false) // Prevent selection until scan is done
			  .show()

			val scanCallback = new ScanCallback() {
			  override def onScanResult(callbackType: Int, result: ScanResult): Unit = {
				val device = result.getDevice
				Log.d("BleScanner", s"Device found: ${device.getName} - ${device.getAddress}")

				// Add unique devices
				if (!deviceList.contains(device)) {
				  deviceList += device
				}
			  }

			  override def onScanFailed(errorCode: Int): Unit = {
				Log.e("BleScanner", s"Scan failed with error code $errorCode")
			  }
			}

			// Start scanning
			scanner.startScan(scanCallback)

			// Stop scanning and show device selection dialog after 5 seconds
			handler.postDelayed(new Runnable {
			  override def run(): Unit = {
				scanner.stopScan(scanCallback)
				scanningDialog.dismiss() // Close scanning message

				// Show device selection dialog
				showDeviceSelectionDialog(deviceList.toSeq)
			  }
			}, 5000) // Scan for 5 seconds
		  }

		case Some(adapter) if !adapter.isEnabled =>
		  Log.e("BleScanner", "Bluetooth is disabled, enabling now.")
		  adapter.enable()

		case None =>
		  Log.e("BleScanner", "Bluetooth adapter not available on this device.")
	  }
	}

	private def showDeviceSelectionDialog(devices: Seq[BluetoothDevice]): Unit = {
	  if (devices.isEmpty) {
		new AlertDialog.Builder(context)
		  .setTitle("No BLE Devices Found")
		  .setMessage("Try again?")
		  .setPositiveButton("Rescan", new DialogInterface.OnClickListener {
			override def onClick(dialog: DialogInterface, which: Int): Unit = {
			  startBleScan() // Restart the scan
			}
		  })
		  .setNegativeButton("Cancel", null)
		  .show()
		return
	  }

	  val deviceSnapshot = devices.toList // Freeze the device list
	  val deviceListCharSequence = deviceSnapshot.map { device =>
		s"${Option(device.getName).getOrElse("Unknown")} - ${device.getAddress}"
	  }.map(_.asInstanceOf[CharSequence]).toArray

	  new AlertDialog.Builder(context)
		.setTitle("Select BLE Device")
		.setItems(deviceListCharSequence, new DialogInterface.OnClickListener {
		  override def onClick(dialog: DialogInterface, which: Int): Unit = {
			if (which >= 0 && which < deviceSnapshot.size) {
			  val selectedDevice = deviceSnapshot(which)
			  saveSelectedDevice(selectedDevice)
			} else {
			  Log.e("BleScanner", s"Invalid selection index: $which (List size: ${deviceSnapshot.size})")
			}
		  }
		})
		.setPositiveButton("Rescan", new DialogInterface.OnClickListener {
		  override def onClick(dialog: DialogInterface, which: Int): Unit = {
			startBleScan() // Restart scan
		  }
		})
		.setNegativeButton("Cancel", null)
		.show()
	}


	private def updateDeviceListInDialog(dialog: AlertDialog, deviceList: Seq[BluetoothDevice]): Unit = {
	  // Convert the device list to an array of CharSequences for display in the dialog
	  val deviceListCharSequence = deviceList.map { device =>
		s"${Option(device.getName).getOrElse("Unknown")} - ${device.getAddress}"
	  }.map(_.asInstanceOf[CharSequence]).toArray

	  // Update the items in the existing dialog's ListView
	  dialog.getListView.setAdapter(new ArrayAdapter[CharSequence](context, android.R.layout.simple_list_item_1, deviceListCharSequence))
	}


  private def saveSelectedDevice(device: BluetoothDevice): Unit = {
    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    val deviceName = Option(device.getName).getOrElse("Unknown")
    sharedPrefs.edit()
      .putString("ble.mac", device.getAddress)
      .putString("ble.name", deviceName)
      .apply()

    // Set the summary with the selected device's name and address
    setSummary(s"$deviceName - ${device.getAddress}")
  }
}
