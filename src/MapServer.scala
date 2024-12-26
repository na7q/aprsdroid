package org.aprsdroid.app

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import java.io.{ByteArrayInputStream, File}
import android.content.Context



class MapServer(service: AprsService, prefs: PrefsWrapper) {

  val MBTILES_FILE = "/sdcard/.OSM/map.mbtiles" // Path to the MBTiles file

  class MapServerHttpd(port: Int) extends NanoHTTPD(port) {



    // Handle the HTTP request to serve the tile
    override def serve(session: IHTTPSession): NanoHTTPD.Response = {
      val uri = session.getUri
      Log.d("MapServer", s"Requested URI: $uri")

      // Match the request pattern for tile requests
      if (uri.matches("/(\\d+)/(\\d+)/(\\d+)\\.png")) {
        val regex = "/(\\d+)/(\\d+)/(\\d+)\\.png".r
        uri match {
          case regex(z, x, y) =>
            serveTile(z.toInt, x.toInt, y.toInt)
          case _ =>
            NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "404 Not Found")
        }
      } else {
        NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "404 Not Found")
      }
    }
    // Serve the tile from MBTiles using Android's SQLite support
    def serveTile(z: Int, x: Int, y: Int): NanoHTTPD.Response = {
      var db: SQLiteDatabase = null
      var cursor: Cursor = null

      try {
        // Invert the y-coordinate for the correct tile orientation
        val invertedY = (Math.pow(2, z).toInt - 1) - y

        // Open the MBTiles database using Android's SQLite API
        db = openDatabaseConnection()

        // Query the tile from the database
        cursor = db.rawQuery(
          "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?",
          Array(z.toString, x.toString, invertedY.toString)
        )

        if (cursor.moveToFirst()) {
          // Get the tile data from the result set
          val tileData: Array[Byte] = cursor.getBlob(cursor.getColumnIndex("tile_data"))

          // Return the tile as a PNG image
          val response = NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK, "image/png", new ByteArrayInputStream(tileData), tileData.length
          )
          response.setChunkedTransfer(false)
          return response
        } else {
          // If tile not found
          return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "Tile not found in MBTiles")
        }
      } catch {
        case e: Exception =>
          Log.e("MapServer", s"Error serving tile: ${e.getMessage}", e)
          return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/plain", "500 Internal Server Error")
      } finally {
        // Ensure that the cursor and database connection are always closed
        if (cursor != null) cursor.close()
        if (db != null && db.isOpen) db.close()
      }
    }

    // Helper function to open SQLite database connection using Android's SQLite
    def openDatabaseConnection(): SQLiteDatabase = {
      val dbFile = new File(MBTILES_FILE)
      if (!dbFile.exists()) {
        throw new IllegalArgumentException("MBTiles file not found")
      }
      SQLiteDatabase.openOrCreateDatabase(dbFile, null)
    }
  }

  private var server: Option[MapServerHttpd] = None

  // Check if the MBTiles file exists
  def checkMbTilesFile(): Unit = {
    val file = new File(MBTILES_FILE)
    if (!file.exists()) {
      Log.e("MapServer", s"MBTiles file does not exist at: $MBTILES_FILE")
    }
  }

  // Start the server
  def start(): Unit = {
    // Check if the file exists before starting the server
    checkMbTilesFile()

    val port = 8080
    try {
      val newServer = new MapServerHttpd(port)
      newServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
      server = Some(newServer)
      Log.d("MapServer", s"Server started on port $port")
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
        Log.w("MapServer", "Server is not running.")
    }
  }
}
