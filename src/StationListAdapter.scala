package org.aprsdroid.app

import _root_.android.app.ListActivity
import _root_.android.content._
import _root_.android.database.Cursor
import _root_.android.os.{AsyncTask, Bundle, Handler}
import _root_.android.text.format.DateUtils
import _root_.android.util.Log
import _root_.android.view.View
import _root_.android.widget.{SimpleCursorAdapter, TextView}
import _root_.android.widget.FilterQueryProvider
import java.util.regex.{Matcher, Pattern}

object StationListAdapter {
	import StorageDatabase.Station._
	val LIST_FROM = Array(CALL, COMMENT, QRG)
	val LIST_TO = Array(R.id.station_call, R.id.listmessage, R.id.station_qrg)

	val SINGLE = 0
	val NEIGHBORS = 1
	val SSIDS = 2
}

class StationListAdapter(context : Context, prefs : PrefsWrapper,
	mycall : String, targetcall : String, mode : Int)
		extends SimpleCursorAdapter(context, R.layout.stationview, null, StationListAdapter.LIST_FROM, StationListAdapter.LIST_TO) {

	var my_lat = 0
	var my_lon = 0
	var reload_pending = 0
	lazy val storage = StorageDatabase.open(context)

	if (mode == StationListAdapter.NEIGHBORS)
		setFilterQueryProvider(getNeighborFilter())

	reload()

	lazy val locReceiver = new LocationReceiver2(load_cursor,
		replace_cursor, cancel_cursor)

	context.registerReceiver(locReceiver, new IntentFilter(AprsService.UPDATE))

	private val DARK = Array(0xff, 0x80, 0x80, 0x50)
	private val BRIGHT = Array(0xff, 0xff, 0xff, 0xe8)
	private val MAX = 30*60*1000
	def getAgeColor(ts : Long) : Int = {
		val delta = System.currentTimeMillis - ts
		// normalize the time difference to a value 0..30min [ms]
		val factor = if (delta < MAX) delta.toInt else MAX
		// linearly blend the individual RGB values using the factor
		val mix = DARK zip BRIGHT map (t => { t._2 - (t._2 - t._1)*factor/MAX } )
		// make a single int from the color array
		mix.reduceLeft(_*256 + _)
	}

	// return compass bearing for a given value
	private val LETTERS = Array("N", "NE", "E", "SE", "S", "SW", "W", "NW")
	def getBearing(b : Double) = LETTERS(((b.toInt + 22 + 720) % 360) / 45)

	override def bindView(view : View, context : Context, cursor : Cursor) {
		import StorageDatabase.Station._

		// TODO: multidimensional mapping
		val distage = view.findViewById(R.id.station_distage).asInstanceOf[TextView]
		val call = cursor.getString(COLUMN_CALL)
		val ts = cursor.getLong(COLUMN_TS)
		val age = DateUtils.getRelativeTimeSpanString(context, ts)
		val lat = cursor.getInt(COLUMN_LAT)
		val lon = cursor.getInt(COLUMN_LON)
		val qrg = cursor.getString(COLUMN_QRG)
		val symbol = cursor.getString(COLUMN_SYMBOL)
		val speedInKnots = cursor.getFloat(COLUMN_SPEED)
		val speed = speedInKnots * 1.15078               // Convert from knots to mph to get corrected speed
		val course = cursor.getFloat(COLUMN_COURSE)
		val dist = Array[Float](0, 0)
		val comment = cursor.getString(COLUMN_COMMENT) // Retrieve COMMENT data
		val wx = parseWeatherData(comment, symbol)

		if (call == mycall) {
			view.setBackgroundColor(0x4020ff20)
		} else if (call == targetcall) {
			view.setBackgroundColor(0x402020ff)
		} else
			view.setBackgroundColor(0)
		val color = getAgeColor(ts)
		distage.setTextColor(color)
		view.findViewById(R.id.station_call).asInstanceOf[TextView].setTextColor(color)
		view.findViewById(R.id.station_qrg).asInstanceOf[TextView].setTextColor(color)
		val qrg_visible = if (qrg != null && qrg != "") View.VISIBLE else View.GONE
		view.findViewById(R.id.station_qrg).asInstanceOf[View].setVisibility(qrg_visible)
		val MCD = 1000000.0
		android.location.Location.distanceBetween(my_lat/MCD, my_lon/MCD, lat/MCD, lon/MCD, dist)
		
		// Determine whether to use metric or imperial based on user preference
		val isMetric = prefs.isMetric() // Assuming isMetric() returns true for metric, false for imperial
		val distanceText: String = if (isMetric) {
			val distanceInKm = dist(0) / 1000.0
			"%1.1f km %s\n%s".format(distanceInKm, getBearing(dist(1)), age)
		} else {
			val distanceInMiles = dist(0) / 1000.0 * 0.621371
			"%1.1f mi %s\n%s".format(distanceInMiles, getBearing(dist(1)), age)
		}

		distage.setText(distanceText)
		
		view.findViewById(R.id.station_symbol).asInstanceOf[SymbolView].setSymbol(symbol)

		// Add speed and course to the UI (simplified version)
		val speedTextView = view.findViewById(R.id.station_speed).asInstanceOf[TextView]
		val courseTextView = view.findViewById(R.id.station_course).asInstanceOf[TextView]
		val listMessageTextView = view.findViewById(R.id.listmessage).asInstanceOf[TextView]		
		val wxTextView = view.findViewById(R.id.wx).asInstanceOf[TextView]		

		if (wx.isDefined && wx.get.trim.nonEmpty) {
		  // If wx contains a non-empty value, set the text and make it visible
		  wxTextView.setText(wx.get)
		  wxTextView.setVisibility(View.VISIBLE)
		} else {
		  // If wx is None or empty, hide the wx TextView
		  wxTextView.setVisibility(View.GONE)
		}

		if (comment != null && comment.trim.nonEmpty && wx.isEmpty) {
			listMessageTextView.setText(comment)
			listMessageTextView.setVisibility(View.VISIBLE) // Make it visible if comment is not empty
		} else {
			listMessageTextView.setVisibility(View.GONE)  // Hide it if comment is empty or null
		}
		
		// Convert speed based on the preference
		val speedText = if (isMetric) {
		  val speedInKmh = speed * 1.60934 // 1 mph = 1.60934 km/h
		  f"Speed: $speedInKmh%.1fkmh"
		} else {
		  f"Speed: $speed%.1fmph"
		}

		// Set visibility based on the speed value (only show if valid)
		speedTextView.setVisibility(if (speed > 0) View.VISIBLE else View.GONE)
		if (speed > 0) speedTextView.setText(speedText) // Assuming speed is in km/h

		// Set visibility based on the course value (only show if valid)
		courseTextView.setVisibility(if (course > 0) View.VISIBLE else View.GONE)
		if (course > 0) courseTextView.setText(f"Course: $course%.1f°") // Assuming course is in degrees
		
		super.bindView(view, context, cursor)
	}
	
	def parseWeatherData(comment: String, symbol: String): Option[String] = {
	  // Check if the symbol ends with '_'
	  if (symbol.endsWith("_")) {
		// Log if the symbol ends with '_'
		Log.d("WeatherParser", s"Symbol ends with '_', proceeding with parsing.")
		Log.d("WeatherParser", s"Raw comment: $comment")

		//val pattern = ".*(\\d{3}|\\.\\.\\.)/(\\d{3}|\\.\\.\\.)*(g\\d{3})?(t[-\\d]{3})?(r\\d{3})?(p\\d{3})?(P\\d{3})?(h\\d{2})?(b\\d{5})?(L\\d{3})?(l\\d{3})?(.*)".r
		//val pattern = ".*(\\d{3}|\\.\\.\\.)/(\\d{3}|\\.\\.\\.)?(g\\d{3}|g\\.\\.\\.)?(t[-\\d]{3}|t\\.\\.\\.)?(r\\d{3}|r\\.\\.\\.)?(p\\d{3}|p\\.\\.\\.)?(P\\d{3}|P\\.\\.\\.)?(h\\d{2}|h\\.\\.)?(b\\d{5}|b\\.\\.\\.)?(L\\d{3}|L\\.\\.\\.)?(l\\d{3}|l\\.\\.\\.)?\\s*(.*)".r
		val pattern = ".*?(\\d{3}|\\.\\.\\.)/(\\d{3}|\\.\\.\\.)?(g\\d{3}|g\\.\\.\\.)?(t[-\\d]{3}|t\\.\\.\\.)?(r\\d{3}|r\\.\\.\\.)?(p\\d{3}|p\\.\\.\\.)?(P\\d{3}|P\\.\\.\\.)?(h\\d{2}|h\\.\\.)?(b\\d{5}|b\\.\\.\\.)?(L\\d{3}|L\\.\\.\\.)?(l\\d{3}|l\\.\\.\\.)?\\s*(.*)?".r

		//val pattern = ".*?(\\d{3}|\\.\\.\\.)/(\\d{3}|\\.\\.\\.)?(g\\d{3}|g\\.\\.\\.)?(t[-\\d]{3}|t\\.\\.\\.)?(r\\d{3}|r\\.\\.\\.)?(p\\d{3}|p\\.\\.\\.)?(P\\d{3}|P\\.\\.\\.)?(h\\d{2}|h\\.\\.)?(b\\d{5}|b\\.\\.\\.)?(L\\d{3}|L\\.\\.\\.)?(l\\d{3}|l\\.\\.\\.)?(?:\\s+(.*))?".r

		// Define regex patterns for all weather components
		val patterns = List(
		  "DirectionSpeed" -> ".*(\\d{3}|\\.\\.\\.)/(\\d{3}|\\.\\.\\.).*".r,
		  "Gust" -> ".*g(\\d{3}).*".r,
		  "Temp" -> ".*t([-\\d]{3}).*".r,
		  "Rain1hr" -> ".*r(\\d{3}).*".r,
		  "Rain24hr" -> ".*p(\\d{3}).*".r,
		  "RainMidnight" -> ".*P(\\d{3}).*".r,
		  "Humidity" -> ".*h(\\d{2}).*".r,
		  "Pressure" -> ".*b(\\d{5}).*".r,
		  "LuminosityLow" -> ".*L(\\d{3}).*".r,
		  "LuminosityHigh" -> ".*l(\\d{3}).*".r
		)

		// Use foldLeft to accumulate the parsed data
		val weatherDataList = patterns.foldLeft(List[String]()) { (acc, patternPair) =>
		  val (name, pattern) = patternPair
		  comment match {
			case pattern(data @ _*) if name == "DirectionSpeed" =>
			  // Ensure exactly 2 items (direction and speed)
			  if (data.length == 2) {
				val direction = data(0)
				val speed = data(1)
				Log.d("WeatherParser", s"Matched $name data: direction = $direction, speed = $speed")
				acc :+ s"Direction: $direction Speed: $speed"
			  } else acc

			case pattern(data @ _*) =>
			  Log.d("WeatherParser", s"Matched $name data: ${data.mkString(", ")}")
			  acc :+ s"$name: ${data.mkString(", ")}"

			case _ => acc // No match for this pattern, leave the accumulator unchanged
		  }
		}

		// Extract the remaining comment (everything after weather data)
		val remainingComment = comment match {
		  case pattern(_, _, _, _, _, _, _, _, _, _, _, remaining) => remaining.trim
		  case _ => "" // If the pattern doesn't match, return an empty string
		}

		// Add the remaining comment to the weather data list if it's non-empty
		if (weatherDataList.nonEmpty || remainingComment.nonEmpty) {
		  val result = weatherDataList.mkString(" ") + (if (remainingComment.nonEmpty) s" Comment: $remainingComment" else "")
		  Log.d("WeatherParser", s"Final parsed weather data: $result")
		  Some(result)
		} else {
		  Log.d("WeatherParser", "No matches found for weather data.")
		  None
		}
	  } else {
		// If symbol does not end with '_', log and return None
		Log.d("WeatherParser", s"Symbol does not end with '_', returning empty result.")
		None
	  }
	}

	def getNeighborFilter() = new FilterQueryProvider() {
		def runQuery(constraint : CharSequence) = {
			if (constraint.length() > 0)
				storage.getNeighborsLike("%s%%".format(constraint),
					my_lat, my_lon, System.currentTimeMillis - prefs.getShowAge(), "300")
			else
				storage.getNeighbors(mycall, my_lat, my_lon,
					System.currentTimeMillis - prefs.getShowAge(), "300")
		}
	}

	def load_cursor(i : Intent) = {
		import StationListAdapter._
		val cursor = storage.getStaPosition(mycall)
		if (cursor.getCount() > 0) {
			cursor.moveToFirst()
			my_lat = cursor.getInt(StorageDatabase.Station.COLUMN_LAT)
			my_lon = cursor.getInt(StorageDatabase.Station.COLUMN_LON)
		}
		cursor.close()
		val c = mode match {
			case SINGLE	=> storage.getStaPosition(targetcall)
			case NEIGHBORS	=> storage.getNeighbors(mycall, my_lat, my_lon,
				System.currentTimeMillis - prefs.getShowAge(), "300")
			case SSIDS	=> storage.getAllSsids(targetcall)
		}
		c.getCount()
		c
	}

	def replace_cursor(c : Cursor) {
		if (!context.asInstanceOf[ListActivity].getListView().hasTextFilter())
			changeCursor(c)
		context.asInstanceOf[LoadingIndicator].onStopLoading()
	}
	def cancel_cursor(c : Cursor) {
		c.close()
	}

	def reload() {
		locReceiver.startTask(null)
	}

	def onDestroy() {
		context.unregisterReceiver(locReceiver)
		changeCursor(null)
	}
}
