package org.na7q.app

import _root_.android.location.Location
import _root_.net.ab0oo.aprs.parser._
import _root_.android.util.Log

import scala.math.abs

object AprsPacket {

	// Define KENWOOD_COMMENT_DATA as an immutable Map
	val KENWOOD_COMMENT_DATA: Map[String, Map[String, String]] = Map(
	  ">" -> Map("vendor" -> "Kenwood", "model" -> "TH-D7A", "class" -> "ht"),
	  "=" -> Map("vendor" -> "Kenwood", "model" -> "TM-D710", "class" -> "rig"),
	  "^" -> Map("vendor" -> "Kenwood", "model" -> "TH-D74", "class" -> "ht"),
	  "&" -> Map("vendor" -> "Kenwood", "model" -> "TH-D75", "class" -> "ht"),
	  "]" -> Map("vendor" -> "Kenwood", "model" -> "TM-D700", "class" -> "rig")
	)

	// Define COMMENT_DATA as an immutable Map
	val COMMENT_DATA: Map[String, Map[String, String]] = Map(
	  "_ " -> Map("vendor" -> "Yaesu", "model" -> "VX-8", "class" -> "ht"),
	  "_\"" -> Map("vendor" -> "Yaesu", "model" -> "FTM-350", "class" -> "rig"),
	  "_#" -> Map("vendor" -> "Yaesu", "model" -> "VX-8G", "class" -> "ht"),
	  "_$" -> Map("vendor" -> "Yaesu", "model" -> "FT1D", "class" -> "ht"),
	  "_(" -> Map("vendor" -> "Yaesu", "model" -> "FT2D", "class" -> "ht"),
	  "_0" -> Map("vendor" -> "Yaesu", "model" -> "FT3D", "class" -> "ht"),
	  "_3" -> Map("vendor" -> "Yaesu", "model" -> "FT5D", "class" -> "ht"),
	  "_1" -> Map("vendor" -> "Yaesu", "model" -> "FTM-300D", "class" -> "rig"),
	  "_2" -> Map("vendor" -> "Yaesu", "model" -> "FTM-200D", "class" -> "rig"),
	  "_4" -> Map("vendor" -> "Yaesu", "model" -> "FTM-500D", "class" -> "rig"),
	  "_)" -> Map("vendor" -> "Yaesu", "model" -> "FTM-100D", "class" -> "rig"),
	  "_%" -> Map("vendor" -> "Yaesu", "model" -> "FTM-400DR", "class" -> "rig"),
	  "(5" -> Map("vendor" -> "Anytone", "model" -> "D578UV", "class" -> "ht"),
	  "(8" -> Map("vendor" -> "Anytone", "model" -> "D878UV", "class" -> "ht"),
	  "|3" -> Map("vendor" -> "Byonics", "model" -> "TinyTrak3", "class" -> "tracker"),
	  "|4" -> Map("vendor" -> "Byonics", "model" -> "TinyTrak4", "class" -> "tracker"),
	  "^v" -> Map("vendor" -> "HinzTec", "model" -> "anyfrog"),
	  "*v" -> Map("vendor" -> "KissOZ", "model" -> "Tracker", "class" -> "tracker"),
	  ":2" -> Map("vendor" -> "SQ8L", "model" -> "VP-Tracker", "class" -> "tracker"),
	  " X" -> Map("vendor" -> "SainSonic", "model" -> "AP510", "class" -> "tracker"),
	  "[1" -> Map("vendor" -> "NA7Q", "model" -> "APRSdroid", "class" -> "software")
	)

	val QRG_RE = ".*?(\\d{2,3}[.,]\\d{3,4}).*?".r
	  val characters = Array(
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D",
		"E", "F", "G", "H", "I", "J", "K", "L", "P", "Q", "R", "S", "T", "U", "V", 
		"W", "X", "Y", "Z"
	  )

	  def statusToBits(status: String): (Int, Int, Int) = status match {
		case "Off Duty" => (1, 1, 1)
		case "En Route" => (1, 1, 0)
		case "In Service" => (1, 0, 1)
		case "Returning" => (1, 0, 0)
		case "Committed" => (0, 1, 1)
		case "Special" => (0, 1, 0)
		case "Priority" => (0, 0, 1)
		case "EMERGENCY!" => (0, 0, 0)
		case _ => (1, 1, 1) // Default if status is not found
	  }

	  def degreesToDDM(dd: Double): (Int, Double) = {
		val degrees = Math.floor(dd).toInt
		val minutes = (dd - degrees) * 60
		(degrees, minutes)
	  }

	  def miceLong(dd: Double): (Int, Int, Int) = {
		val (degrees, minutes) = degreesToDDM(Math.abs(dd))
		val minutesInt = Math.floor(minutes).toInt
		val minutesHundreths = Math.floor(100 * (minutes - minutesInt)).toInt
		(degrees, minutesInt, minutesHundreths)
	  }


	  def encodeDest(dd: Double, longOffset: Int, west: Int, messageA: Int, messageB: Int, messageC: Int, ambiguity: Int): String = {
	    val north = if (dd < 0) 0 else 1
	  
	    val (degrees, minutes, minutesHundreths) = miceLong(dd)
	  
	    val degrees10 = Math.floor(degrees / 10.0).toInt
	    val degrees1 = degrees - (degrees10 * 10)
	  
	    val minutes10 = Math.floor(minutes / 10.0).toInt
	    val minutes1 = minutes - (minutes10 * 10)
	  
	    val minutesHundreths10 = Math.floor(minutesHundreths / 10.0).toInt
	    val minutesHundreths1 = minutesHundreths - (minutesHundreths10 * 10)
	  
	    val sb = new StringBuilder
	  	
	    if (messageA == 1) sb.append(characters(degrees10 + 22)) else sb.append(characters(degrees10))
	    if (messageB == 1) sb.append(characters(degrees1 + 22)) else sb.append(characters(degrees1))
	    if (messageC == 1) sb.append(characters(minutes10 + 22)) else sb.append(characters(minutes10))
	  	  
	    if (north == 1) sb.append(characters(minutes1 + 22)) else sb.append(characters(minutes1))
	    if (longOffset == 1) sb.append(characters(minutesHundreths10 + 22)) else sb.append(characters(minutesHundreths10))
	    if (west == 1) sb.append(characters(minutesHundreths1 + 22)) else sb.append(characters(minutesHundreths1))
	  
	    val encoded = sb.toString()
	  
	    // Replace indices 4 and 5 with 'Z' or 'L', depending on 'west'
	    val validAmbiguity = ambiguity.max(0).min(4)
	    val encodedArray = encoded.toCharArray // Convert the encoded string to a char array
	  
	    // A map that specifies the modification rules for each index based on ambiguity
	    val modifyRules = Map(
	  	2 -> (messageC, 'Z', 'L'),
	  	3 -> (north, 'Z', 'L'),
	  	4 -> (longOffset, 'Z', 'L'),
	  	5 -> (west, 'Z', 'L')
	    )
	  
	    // Loop over the indices based on validAmbiguity
	    for (i <- (6 - validAmbiguity) until 6) {
	  	modifyRules.get(i) match {
	  	  case Some((condition, trueChar, falseChar)) =>
	  		val charToUse = if (condition == 1) trueChar else falseChar
	  		encodedArray(i) = charToUse
	  	  case None => // No modification if the index is not in modifyRules
	  	}
	    }
	  
	    // Return the modified string
	    val finalEncoded = new String(encodedArray)
	  
	    finalEncoded
	  }

	  def encodeInfo(dd: Double, speed: Double, heading: Double, symbol: String): (String, Int, Int) = {
	  
		val (degrees, minutes, minutesHundreths) = miceLong(dd)

		val west = if (dd < 0) 1 else 0

		val sb = new StringBuilder
		sb.append("`")

		val speedHT = Math.floor(speed / 10.0).toInt
		val speedUnits = speed - (speedHT * 10)

		val headingHundreds = Math.floor(heading / 100.0).toInt
		val headingTensUnits = heading - (headingHundreds * 100)

		var longOffset = 0

		if (degrees <= 9) {
		  sb.append((degrees + 118).toChar)
		  longOffset = 1
		} else if (degrees >= 10 && degrees <= 99) {
		  sb.append((degrees + 28).toChar)
		  longOffset = 0
		} else if (degrees >= 100 && degrees <= 109) {
		  sb.append((degrees + 8).toChar)
		  longOffset = 1
		} else if (degrees >= 110) {
		  sb.append((degrees - 72).toChar)
		  longOffset = 1
		}

		if (minutes <= 9) sb.append((minutes + 88).toChar) else sb.append((minutes + 28).toChar)
		sb.append((minutesHundreths + 28).toChar)

		if (speed <= 199) sb.append((speedHT + 108).toChar) else sb.append((speedHT + 28).toChar)
		sb.append((Math.floor(speedUnits * 10).toInt + headingHundreds + 32).toChar)
		sb.append((headingTensUnits + 28).toChar)

		sb.append(symbol(1))
		sb.append(symbol(0))
		sb.append("`")

		(sb.toString(), west, longOffset)
	  }

	  def altitude(alt: Double): String = {
		val altM = Math.round(alt * 0.3048).toInt
		val relAlt = altM + 10000

		val val1 = Math.floor(relAlt / 8281.0).toInt
		val rem = relAlt % 8281
		val val2 = Math.floor(rem / 91.0).toInt
		val val3 = rem % 91

		// Ensure that the characters are treated as strings and concatenate properly
	    charFromInt(val1).toString + charFromInt(val2).toString + charFromInt(val3).toString + "}"
		}

	private def charFromInt(value: Int): Char = (value + 33).toChar

	def formatCourseSpeedMice(location: Location): (Int, Int) = {
	  // Default values
	  val status_spd = if (location.hasSpeed && location.getSpeed > 2) {
		// Convert speed from m/s to knots, and return as an integer
		mps2kt(location.getSpeed).toInt
	  } else {
		0 // If no valid speed or below threshold, set speed to 0
	  }

	  val course = if (location.hasBearing) {
		// Get bearing as an integer (course)
		location.getBearing.asInstanceOf[Int]
	  } else {
		0 // If no bearing, set course to 0
	  }

	  (status_spd, course)
	}

	def formatAltitudeMice(location: Location): Option[Int] = {
	  if (location.hasAltitude) {
		// Convert altitude to feet, round to nearest integer, and wrap in Some
		Some(math.round(m2ft(location.getAltitude)).toInt)
	  } else {
		None // If no altitude, return None
	  }
	}

	def passcode(callssid : String) : Int = {
		// remove ssid, uppercase, add \0 for odd-length calls
		val call = callssid.split("-")(0).toUpperCase() + "\u0000"
		var hash = 0x73e2
		for (i <- 0 to call.length-2 by 2) {
			hash ^= call(i) << 8
			hash ^= call(i+1)
		}
		hash & 0x7fff
	}
	def passcodeAllowed(callssid : String, pass : String, optional : Boolean) = {
		pass match {
		case "" => optional
		case "-1" => optional
		case _ => (passcode(callssid).toString() == pass)
		}
	}
		

	def formatCallSsid(callsign : String, ssid : String) : String = {
		if (ssid != null && ssid != "")
			return callsign + "-" + ssid
		else
			return callsign
	}

	def m2ft(meter : Double) : Int = (meter*3.2808399).asInstanceOf[Int]

	def mps2kt(mps : Double) : Int = (mps*1.94384449).asInstanceOf[Int]

	def formatAltitude(location : Location) : String = {
		if (location.hasAltitude)
			"/A=%06d".formatLocal(null, m2ft(location.getAltitude))
		else
			""
	}
	
	def formatAltitudeCompressed(location : Location) : String = {
		if (location.hasAltitude) {
			var altitude = m2ft(location.getAltitude)
			var compressedAltitude = ((math.log(altitude) / math.log(1.002)) + 0.5).asInstanceOf[Int]
			var c = (compressedAltitude / 91).asInstanceOf[Byte] + 33
			var s = (compressedAltitude % 91).asInstanceOf[Byte] + 33
			// Negative altitudes cannot be expressed in base-91 and results in corrupt packets
			if(c < 33) c = 33
			if(s < 33) s = 33
			"%c%c".format(c.asInstanceOf[Char], s.asInstanceOf[Char])
		} else
			""
	}

	def formatCourseSpeed(location : Location) : String = {
		// only report speeds above 2m/s (7.2km/h)
		if (location.hasSpeed && location.hasBearing)
		   // && location.getSpeed > 2)
			"%03d/%03d".formatLocal(null, location.getBearing.asInstanceOf[Int],
				mps2kt(location.getSpeed))
		else
			""
	}

	def formatCourseSpeedCompressed(location : Location) : String = {
		// only report speeds above 2m/s (7.2km/h)
		if (location.hasSpeed && location.hasBearing) {
			// && location.getSpeed > 2)
			var compressedBearing = (location.getBearing.asInstanceOf[Int] / 4).asInstanceOf[Int]
			var compressedSpeed = ((math.log(mps2kt(location.getSpeed)) / math.log(1.08)) - 1).asInstanceOf[Int]
			var c = compressedBearing.asInstanceOf[Byte] + 33;
			var s = compressedSpeed.asInstanceOf[Byte] + 33;
			// Negative speeds a courses cannot be expressed in base-91 and results in corrupt packets
			if(c < 33) c = 33
			if(s < 33) s = 33
			"%c%c".format(c.asInstanceOf[Char], s.asInstanceOf[Char])
		} else {
			""
		}
	}

	def formatFreq(csespd : String, freq : Float) : String = {
		if (freq == 0) "" else {
			val prefix = if (csespd.length() > 0) "/" else ""
			prefix + "%07.3fMHz".formatLocal(null, freq)
		}
	}
	
	def formatFreqMice(freq : Float) : String = {
		if (freq == 0) "" else {
			"%07.3fMHz".formatLocal(null, freq)
		}
	}	

	def formatLogin(callsign : String, ssid : String, passcode : String, version : String) : String = {
		"user %s pass %s vers %s".format(formatCallSsid(callsign, ssid), passcode, version)
	}

	def formatRangeFilter(loc : Location, range : Int) : String = {
		if (loc != null)
			"r/%1.3f/%1.3f/%d".formatLocal(null, loc.getLatitude, loc.getLongitude, range)
		else
			""
	}

	val  DirectionsLatitude = "NS";
	val  DirectionsLongitude = "EW";
	def formatDMS(coordinate : Float, nesw : String) = {
		val dms = Location.convert(abs(coordinate), Location.FORMAT_SECONDS).split(":")
		val nesw_idx = (coordinate < 0).compare(false)
		"%2s° %2s' %s\" %s".format(dms(0), dms(1), dms(2), nesw(nesw_idx))
	}

	def formatCoordinates(latitude : Float, longitude : Float) = {
		(AprsPacket.formatDMS(latitude, DirectionsLatitude),
		 AprsPacket.formatDMS(longitude, DirectionsLongitude))
	}

	def parseQrg(comment : String) : String = {
		comment match {
		case QRG_RE(qrg) => qrg
		case _ => null
		}
	}

	// Function to check if the last 2 characters of the comment match anything in COMMENT_DATA
	def micetocall(comment: String): Option[String] = {
	  val lastTwoChars = comment.takeRight(2) // Get the last 2 characters of the comment
	  COMMENT_DATA.get(lastTwoChars).flatMap(_.get("model"))
	}

	// Function to check if the last character of the comment matches anything in KENWOOD_COMMENT_DATA
	def kenwoodtocall(comment: String): Option[String] = {
	  val lastChar = comment.takeRight(1) // Get the last character of the comment
	  KENWOOD_COMMENT_DATA.get(lastChar).flatMap(_.get("model"))
	}

	// Function to check old Kenwood calls
	def oldkenwoodtocall(packet: String): Option[String] = {
	  val colonIndex = packet.indexOf(':')
	  
	  if (colonIndex == -1 || colonIndex + 10 >= packet.length) {
		None
	  } else {
		if (packet(colonIndex + 1) == '\'') {
		  val keyChar = packet(colonIndex + 10).toString
		  KENWOOD_COMMENT_DATA.get(keyChar).flatMap(_.get("model"))
		} else {
		  None
		}
	  }
	}

	def parseComment(comment: String): String = {
	  var modifiedComment = comment

	  // Step 1: Check if it ends with micetocall characters (last 2 characters)
	  if (modifiedComment.length >= 2 && AprsPacket.micetocall(modifiedComment.takeRight(2)).isDefined) {
		modifiedComment = modifiedComment.dropRight(2) // Strip last 2 characters and return immediately
	  }

	  // Step 2: Check if it ends with kenwoodtocall (last 1 character)
	  if (modifiedComment.nonEmpty && AprsPacket.kenwoodtocall(modifiedComment.takeRight(1)).isDefined) {
		modifiedComment = modifiedComment.dropRight(1) // Strip last 1 character
	  }

	  // Step 3: Remove `}` and everything before it if it appears within the first 4 characters
	  val bracketIndex = modifiedComment.indexOf("}")
	  if (bracketIndex >= 0 && bracketIndex <= 3) {
		modifiedComment = modifiedComment.substring(bracketIndex + 1).trim
	  }

	  // Step 4: Remove specific prefixes ("PHGxxxx", "RNGxxxx", "DFSxxxx")
	  val prefixPatterns = Seq(
		"PHG\\d{4,5}/?", // Matches PHG followed by exactly 4 digits, with an optional trailing slash
		"RNG\\d{4}/?", // Matches RNG followed by exactly 4 digits, with an optional trailing slash
		"DFS\\d{4}/?"  // Matches DFS followed by exactly 4 digits, with an optional trailing slash
	  )

	  for (pattern <- prefixPatterns) {
		val before = modifiedComment
		modifiedComment = modifiedComment.replaceFirst(pattern.r.regex, "").trim
	  }

	  // Step 5: Remove altitude format "/A=XXXXX" where X can be positive or negative digits
	  val altitudePattern = "/?A=(-\\d{5}|\\d{6})".r
	  if (altitudePattern.findFirstIn(modifiedComment).isDefined) {
		modifiedComment = altitudePattern.replaceAllIn(modifiedComment, "").trim
	  }

	  // Step 6: Remove "XXX/YYY" or "XXX/YYY/A=ZZZZZ" format
	  val courseSpeedPattern = "^\\d{3}/\\d{3}(/A=(-?\\d{5}|\\d{6}))?/?".r
	  if (courseSpeedPattern.findFirstIn(modifiedComment).isDefined) {
		modifiedComment = courseSpeedPattern.replaceAllIn(modifiedComment, "").trim
	  }

	  // Step 7: Remove weather-related patterns
	  val weatherPatterns = Seq(
		"\\.\\.\\./\\.\\.\\.",  // Matches ".../..." (Direction/Speed missing)
		"\\.\\.\\./\\d{3}",    // Matches ".../XXX" (Speed missing)
		"\\d{3}/\\.\\.\\.",    // Matches "XXX/..." (Direction missing)
		"c\\d{3}",  // Course (cXXX)
		"c[ .]{3}",  // Course missing (c...)
		"s\\d{3}",  // Speed (sXXX)
		"s[ .]{3}",  // Speed missing (s...)
		"g\\d{3}",  // Wind Gust (gXXX)
		"g[ .]{3}",  // Wind Gust missing (g...)
		"t\\d{3}",  // Temperature (tXXX)
		"t[ .]{3}",  // Temperature missing (t...)
		"r\\d{3}",  // Rainfall in last hour (rXXX)
		"r[ .]{3}",  // Rainfall missing (r...)
		"p\\d{3}",  // Rainfall in last 24 hours (pXXX)
		"p[ .]{3}",  // Rainfall missing (p...)
		"P\\d{3}",  // Rainfall since midnight (PXXX)
		"P[ .]{3}",  // Rainfall missing (P...)
		"h\\d{2,3}", // Humidity (hXX or hXXX)
		"h[ .]{2,3}",  // Humidity missing (h.. or h...)
		"b\\d{5}",  // Barometric Pressure (bXXXXX)
		"b[ .]{3,5}",		
		"L\\d{3}",  // Luminosity (LXXX)
		"L[ .]{3}"   // Luminosity missing (L...)
	  )

	  for (pattern <- weatherPatterns) {
		val before = modifiedComment
		modifiedComment = modifiedComment.replaceAll(pattern.r.regex, "").trim
	  }
	  
	  // Step 8: Remove Base91 telemetry if present
	  val base91TelemetryRegex = """\|[^|]{2,12}\|""".r
	  if (base91TelemetryRegex.findFirstIn(modifiedComment).isDefined) {
	    modifiedComment = base91TelemetryRegex.replaceAllIn(modifiedComment, "").trim
	  }

	  // Step 9: Remove APRS `!data!` segments (e.g., "!wF$!")
	  val exclamationData = """![!-~]+!""".r
	  if (exclamationData.findFirstIn(modifiedComment).isDefined) {
	    modifiedComment = exclamationData.replaceAllIn(modifiedComment, "").trim
	  }

	  // Return the modified comment after all transformations
	  modifiedComment
	}

	def parseHostPort(hostport : String, defaultport : Int) : (String, Int) = {
		val splits = hostport.trim().split(":")
		try {
			// assume string:int
			return (splits(0), splits(1).toInt)
		} catch {
			// fallback to default port if none/bad one given
			case _ : Throwable => return (splits(0), defaultport)
		}
	}

	// position ambiguity re-defined as 67% (Android's Location)
	// of the worst-case error from the ambiguity field
	//
	// Best possible APRS precision at the equator is ~18m, we assume
	// proper rounding (so max. 9m between actual and reported position)
	// and take 67% of that.
	val APRS_AMBIGUITY_METERS = Array(6, 37185, 6200, 620, 62)

	def position2location(ts : Long, p : Position, cse : CourseAndSpeedExtension = null) = {
		val l = new Location("APRS")
		l.setLatitude(p.getLatitude())
		l.setLongitude(p.getLongitude())
		l.setTime(ts)
		l.setAccuracy(APRS_AMBIGUITY_METERS(p.getPositionAmbiguity()))
		if (cse != null) {
			// course == bearing?
			l.setBearing(cse.getCourse)
			// APRS uses knots, Location expects m/s
			l.setSpeed(cse.getSpeed / 1.94384449f)
		}
		// todo: bearing, speed
		l
	}
}
