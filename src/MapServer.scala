package org.aprsdroid.app

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.{ByteArrayInputStream, File}
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import org.eclipse.jetty.server.{Server, Request, HttpConfiguration}
import org.eclipse.jetty.servlet.{ServletHandler, ServletHolder}
import android.content.Context

class MapServer(service: AprsService, prefs: PrefsWrapper) {

  val MBTILES_FILE = "/sdcard/.OSM/map.mbtiles" // Path to the MBTiles file

  class MapServerServlet extends HttpServlet {
    override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
	  val uri = req.getRequestURI
	  Log.d("MapServer", s"Incoming request: ${req.getMethod} $uri")

	  // Match the request pattern for tile requests
	  val regex = "/(\\d+)/(\\d+)/(\\d+)\\.png".r
	  uri match {
		case regex(z, x, y) =>
		  Log.d("MapServer", s"Matched tile request: z=$z, x=$x, y=$y")
		  serveTile(z.toInt, x.toInt, y.toInt, resp)
		case _ =>
		  Log.d("MapServer", s"No match for URI: $uri")
		  resp.setStatus(HttpServletResponse.SC_NOT_FOUND)
		  resp.getWriter.println("404 Not Found")
	  }
	}


    // Serve the tile from MBTiles using Android's SQLite support
    def serveTile(z: Int, x: Int, y: Int, resp: HttpServletResponse): Unit = {
      var db: SQLiteDatabase = null
      var cursor: Cursor = null

      Log.d("MapServer", s"Attempting to serve tile for z=$z, x=$x, y=$y")
      try {
        // Invert the y-coordinate for the correct tile orientation
        val invertedY = (Math.pow(2, z).toInt - 1) - y
        Log.d("MapServer", s"Inverted Y-coordinate: $invertedY")

        // Open the MBTiles database using Android's SQLite API
        db = openDatabaseConnection()
        Log.d("MapServer", "Database connection opened successfully")

        // Query the tile from the database
        val query = "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?"
        val args = Array(z.toString, x.toString, invertedY.toString)
        Log.d("MapServer", s"Executing query: $query with args ${args.mkString(", ")}")

        cursor = db.rawQuery(query, args)

        if (cursor.moveToFirst()) {
          Log.d("MapServer", "Tile data found in database")
          // Get the tile data from the result set
          val tileData: Array[Byte] = cursor.getBlob(cursor.getColumnIndex("tile_data"))

          // Return the tile as a PNG image
          resp.setContentType("image/png")
          resp.setStatus(HttpServletResponse.SC_OK)
          resp.getOutputStream.write(tileData)
          Log.d("MapServer", "Tile served successfully")
        } else {
          Log.d("MapServer", "Tile not found in database")
          // If tile not found
          resp.setStatus(HttpServletResponse.SC_NOT_FOUND)
          resp.getWriter.println("Tile not found in MBTiles")
        }
      } catch {
        case e: Exception =>
          Log.e("MapServer", s"Error serving tile: ${e.getMessage}", e)
          resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
          resp.getWriter.println("500 Internal Server Error")
      } finally {
        // Ensure that the cursor and database connection are always closed
        if (cursor != null) {
          cursor.close()
          Log.d("MapServer", "Database cursor closed")
        }
        if (db != null && db.isOpen) {
          db.close()
          Log.d("MapServer", "Database connection closed")
        }
      }
    }

    // Helper function to open SQLite database connection using Android's SQLite
    def openDatabaseConnection(): SQLiteDatabase = {
      val dbFile = new File(MBTILES_FILE)
      if (!dbFile.exists()) {
        Log.e("MapServer", s"MBTiles file not found at path: $MBTILES_FILE")
        throw new IllegalArgumentException("MBTiles file not found")
      }
      Log.d("MapServer", s"Opening MBTiles database at path: $MBTILES_FILE")
      SQLiteDatabase.openOrCreateDatabase(dbFile, null)
    }
  }

  private var server: Option[Server] = None

  // Check if the MBTiles file exists
  def checkMbTilesFile(): Unit = {
    val file = new File(MBTILES_FILE)
    if (!file.exists()) {
      Log.e("MapServer", s"MBTiles file does not exist at: $MBTILES_FILE")
    } else {
      Log.d("MapServer", s"MBTiles file found at: $MBTILES_FILE")
    }
  }

	// Start the server
	def start(): Unit = {
	  // Check if the file exists before starting the server
	  checkMbTilesFile()

	  val port = 8080
	  try {
		// Set up the Jetty server
		val server = new Server(port)

		// Create a servlet handler
		val handler = new ServletHandler()

		// Use a wildcard to capture all requests and process them in the servlet
		handler.addServletWithMapping(new ServletHolder(new MapServerServlet()), "/*")
		Log.d("MapServer", "Servlet handler set up for all requests")

		// Set the handler for the server
		server.setHandler(handler)

		// Start the server
		server.start()
		this.server = Some(server)
		Log.d("MapServer", s"Server started successfully on port $port")
	  } catch {
		case e: Exception =>
		  Log.e("MapServer", s"Error starting server: ${e.getMessage}", e)
	  }
	}


  // Stop the server (gracefully shut down)
  def stop(): Unit = {
    server match {
      case Some(s) =>
        s.stop()
        Log.d("MapServer", "Server stopped successfully")
        server = None
      case None =>
        Log.w("MapServer", "Attempted to stop server, but it was not running")
    }
  }
}
