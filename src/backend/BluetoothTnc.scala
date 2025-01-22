package org.aprsdroid.app

import _root_.android.bluetooth._
import _root_.android.app.Service
import _root_.android.content.Intent
import _root_.android.location.Location
import _root_.android.util.Log
import _root_.java.io.{InputStream, OutputStream}
import _root_.java.net.{InetAddress, Socket}
import _root_.java.util.UUID

import _root_.net.ab0oo.aprs.parser._

class BluetoothTnc(service : AprsService, prefs : PrefsWrapper) extends AprsBackend(prefs) {
	val TAG = "APRSdroid.Bluetooth"
	val SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

	val bt_client = prefs.getBoolean("bt.client", true)
	val tncmac = prefs.getString("bt.mac", null)
	val tncchannel = prefs.getStringInt("bt.channel", -1)
	var conn : BtSocketThread = null
	val RECONNECT = prefs.getStringInt("bt.reconnect", 30)

	def start() = {
		if (conn == null)
			createConnection()
		false
	}

	def createConnection() {
		Log.d(TAG, "BluetoothTnc.createConnection: " + tncmac)
		val adapter = BluetoothAdapter.getDefaultAdapter()
		if (adapter == null) {
			service.postAbort(service.getString(R.string.bt_error_unsupported))
			return
		}
		if (!adapter.isEnabled()) {
			service.postAbort(service.getString(R.string.bt_error_disabled))
			return
		}
		if (bt_client && tncmac == null) {
			service.postAbort(service.getString(R.string.bt_error_no_tnc))
			return
		}

		val tnc = if (bt_client) adapter.getRemoteDevice(tncmac) else null
		conn = new BtSocketThread(adapter, tnc)
		conn.start()
	}

	def update(packet : APRSPacket) : String = {
		Log.d(TAG, "BluetoothTnc.update: " + packet)
		conn.update(packet)
	}

	def stop() {
		if (conn == null)
			return
		conn.synchronized {
			conn.running = false
		}
		conn.shutdown()
		conn.interrupt()
		conn.join(50)
	}

	class BtSocketThread(ba : BluetoothAdapter, tnc : BluetoothDevice)
			extends Thread("APRSdroid Bluetooth connection") {
		val TAG = "BtSocketThread"
		var running = true
		var socket : BluetoothSocket = null
		var proto : TncProto = null

		def returnFreq() {
			try {
				val backendName = service.prefs.getBackendName()
				// Check if the conditions for frequency control are met
				if (service.prefs != null && backendName != null &&
					service.prefs.getBoolean("freq_control", false) && backendName.contains("Bluetooth SPP")) {					
					proto.writeReturn() // Send return command
					}
			} catch {
				case e: Exception =>
					Log.e(TAG, "Error while sending return frequency command.", e)
			}
		}			

		def log(s : String) {
			service.postAddPost(StorageDatabase.Post.TYPE_INFO, R.string.post_info, s)
		}
		def log(id : Integer, args : Object*) {
			service.postAddPost(StorageDatabase.Post.TYPE_INFO, R.string.post_info, service.getString(id, args : _*))
		}

		def init_socket() {
			Log.d(TAG, "init_socket()")
			if (socket != null) {
				shutdown()
			}
				if (tnc == null) {
					// we are a host
					log(R.string.bt_awaiting)
					socket = ba.listenUsingRfcommWithServiceRecord("SPP", SPP).accept(-1)
					val dev = socket.getRemoteDevice()
					val name = if (dev.getName() != null) dev.getName() else dev.getAddress()
					log(R.string.bt_client_connected, name)
				} else
				if (tncchannel == -1) {
					log(R.string.bt_connecting_to_spp, tncmac)
					socket = tnc.createRfcommSocketToServiceRecord(SPP)
					socket.connect()
				} else {
					log(R.string.bt_connecting_to_channel, tncmac, new Integer(tncchannel))
					val m = tnc.getClass().getMethod("createRfcommSocket", classOf[Int])
					socket = m.invoke(tnc, tncchannel.asInstanceOf[AnyRef]).asInstanceOf[BluetoothSocket]
					socket.connect()
				}
				log(R.string.bt_connected)

			proto = AprsBackend.instanciateProto(service, socket.getInputStream(), socket.getOutputStream())
			Log.d(TAG, "init_socket() done")
		}

		override def run() {
			running = true
			var need_reconnect = false
			Log.d(TAG, "BtSocketThread.run()")
			try {
				init_socket()
				service.postPosterStarted()
			} catch {
				case e : IllegalArgumentException => service.postAbort(e.getMessage()); running = false
				case e : Exception => {
					e.printStackTrace();
					val name = if (tnc != null && tnc.getName() != null) tnc.getName() else tncmac
					service.postAbort(service.getString(R.string.bt_error_connect, name))
					running = false;
                                }
			}
			while (running) {
				try {
					if (need_reconnect) {
						log(service.getString(R.string.bt_reconnecting, RECONNECT.asInstanceOf[AnyRef]))						
						try {
							Thread.sleep(RECONNECT*1000)
						} catch { case _ : InterruptedException => }
						init_socket()
						need_reconnect = false
						service.postLinkOn(R.string.p_link_bt)
					}
					Log.d(TAG, "waiting for data...")
					while (running) {
						val line = proto.readPacket()
						Log.d(TAG, "recv: " + line)
						service.postSubmit(line)
					}
				} catch {
					case e : Exception => 
						Log.d(TAG, "exception, reconnecting...")
						if (running && !need_reconnect)
							service.postLinkOff(R.string.p_link_bt)
						need_reconnect = true
						try {
							if (running) // only bother the user if not yet quitting
								service.postAddPost(StorageDatabase.Post.TYPE_INFO,
									R.string.post_error, e.toString())
							e.printStackTrace()
						} catch { case _ : Exception => Log.d(TAG, "Yo dawg! I got an exception while getting an exception!")
						}
				}
			}
			Log.d(TAG, "BtSocketThread.terminate()")
		}

		def update(packet : APRSPacket) : String = {
			try {
				proto.writePacket(packet)
				"Bluetooth OK"
			} catch { case e : Exception => e.printStackTrace(); conn.socket.close(); "Bluetooth disconnected" }
		}

		def catchLog(tag : String, fun : ()=>Unit) {
			Log.d(TAG, "catchLog(" + tag + ")")
			try {
				fun()
			} catch {
			case e : Exception => e.printStackTrace(); Log.d(TAG, tag + " execption: " + e)
			}
		}

		def shutdown() {
			Log.d(TAG, "shutdown()")
			if (proto != null) {
				returnFreq()
				proto.stop()
			}
			this.synchronized {
				catchLog("socket.close", socket.close)
			}
		}
	}


}
