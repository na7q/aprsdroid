package org.aprsdroid.app

import _root_.android.content.Context
import _root_.android.util.Log

import java.io.File
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.matching.Regex

// Loads tocalls.yaml and matches a tocall string to a device name.
// The file is parsed once and cached in memory; it reloads automatically
// if the file on disk has changed (e.g. after an update).
object DeviceIdentifier {
  val TAG = "APRSdroid.DeviceIdentifier"

  type DeviceInfo = Map[String, String]

  // Each entry is a compiled pattern paired with its parsed device info.
  private var patterns: Seq[(Regex, DeviceInfo)] = Seq.empty
  private var loadedFileTime: Long = -1

  def tocallsFile(context: Context): File =
    new File(context.getFilesDir, "tocalls.yaml")

  def getDeviceInfo(context: Context, tocall: String): Option[DeviceInfo] = {
    if (tocall == null || tocall.isEmpty) return None
    reloadIfStale(context)
    patterns.find { case (regex, _) => regex.pattern.matcher(tocall).matches() }
            .map  { case (_, info)   => info }
  }

  // Backward-compatible helper.
  def getDevice(context: Context, tocall: String): Option[String] =
    getDeviceInfo(context, tocall).flatMap(_.get("model"))

  // Force a reload on the next call to getDevice.
  def invalidate(): Unit = { loadedFileTime = -1 }

  // ---- private helpers ----

  private def reloadIfStale(context: Context): Unit = {
    val file = tocallsFile(context)
    if (!file.exists()) return
    if (file.lastModified() == loadedFileTime) return   // already up to date
    reload(file)
  }

  private def reload(file: File): Unit = {
    Log.i(TAG, "Loading tocalls from " + file.getAbsolutePath)
    val buf = ListBuffer[(Regex, DeviceInfo)]()

    // Parse the YAML conservatively without a full YAML dependency.
    // We only care about the tocalls list entries.
    var inTocalls = false
    var currentKey: Option[String] = None
    var currentInfo = Map[String, String]()

    val lines =
      try   { Source.fromFile(file, "UTF-8").getLines().toSeq }
      catch { case e: Exception => Log.e(TAG, "Failed to read " + file, e); return }

    def flushCurrent(): Unit = {
      if (currentKey.isDefined && currentInfo.get("model").exists(_.nonEmpty))
        buf += ((patternToRegex(currentKey.get), currentInfo))
      currentKey = None
      currentInfo = Map.empty
    }

    for (rawLine <- lines) {
      val line = rawLine.replace("\t", "    ")
      val trimmed = line.trim

      if (trimmed == "tocalls:") {
        inTocalls = true
        flushCurrent()
      } else if (inTocalls && !trimmed.isEmpty && !trimmed.startsWith("#") && !line.startsWith(" ")) {
        flushCurrent()
        inTocalls = false
      } else if (inTocalls) {
        if (trimmed.startsWith("- tocall:")) {
          flushCurrent()
          val key = trimmed.substring("- tocall:".length).trim
          if (key.nonEmpty) currentKey = Some(key)
        } else if (currentKey.isDefined) {
          Seq("vendor", "model", "class", "os").foreach(field => {
            val prefix = field + ":"
            if (trimmed.startsWith(prefix)) {
              val value = trimmed.substring(prefix.length).trim.stripPrefix("\"").stripSuffix("\"")
              if (value.nonEmpty) currentInfo += (field -> value)
            }
          })
        }
      }
    }

    flushCurrent()
    patterns = buf.toSeq
    loadedFileTime = file.lastModified()
    Log.i(TAG, "Loaded %d device patterns".format(patterns.size))
  }

  // Converts a tocall glob pattern (using ? and *) to a Regex.
  // All other regex metacharacters are escaped first.
  private def patternToRegex(pattern: String): Regex = {
    val sb = new StringBuilder
    for (ch <- pattern) ch match {
      case '?' => sb.append('.')
      case '*' => sb.append(".*")
      case c   => sb.append(Regex.quote(c.toString))
    }
    ("(?i)" + sb.toString).r   // case-insensitive: tocalls can be mixed case
  }
}
