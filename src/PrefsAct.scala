package org.aprsdroid.app

import _root_.android.content.{Context, Intent, SharedPreferences}
import _root_.android.net.Uri
import _root_.android.os.{Build, Bundle, Environment}
import _root_.android.preference.Preference.OnPreferenceClickListener
import _root_.android.preference.{Preference, PreferenceActivity, PreferenceManager}
import _root_.android.view.{Menu, MenuItem}
import _root_.android.widget.Toast
import java.text.SimpleDateFormat
import java.io.{File, PrintWriter}
import java.util.Date
import android.provider.Settings


import org.json.JSONObject

class PrefsAct extends PreferenceActivity {
	lazy val db = StorageDatabase.open(this)
	lazy val prefs = new PrefsWrapper(this)

	def exportPrefs() {
		val filename = "profile-%s.aprs".format(new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date()))
		val directory = UIHelper.getExportDirectory(this)
		val file = new File(directory, filename)
		try {
			directory.mkdirs()
			val prefs = PreferenceManager.getDefaultSharedPreferences(this)
			val allPrefs = prefs.getAll
			allPrefs.remove("map_zoom")
			val json = new JSONObject(allPrefs)
			val fo = new PrintWriter(file)
			fo.println(json.toString(2))
			fo.close()

			UIHelper.shareFile(this, file, filename)
		} catch {
			case e : Exception => Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show()
		}
	}

	def fileChooserPreference(pref_name : String, reqCode : Int, titleId : Int) {
		findPreference(pref_name).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			def onPreferenceClick(preference : Preference) = {
				val get_file = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*")
					.addCategory(Intent.CATEGORY_OPENABLE)
				startActivityForResult(Intent.createChooser(get_file, 
					getString(titleId)), reqCode)
				true
			}
		});
	}
	override def onCreate(savedInstanceState: Bundle) {
		super.onCreate(savedInstanceState)
		addPreferencesFromResource(R.xml.preferences)

        // Set the click listener for the "Manage All Files Access" preference
        val allFilesAccessPref = findPreference("all_files_access")
        if (allFilesAccessPref != null) {
            allFilesAccessPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                def onPreferenceClick(preference: Preference) = {
                    openAllFilesAccessSettings()  // Call the method to handle the access settings
                    true  // Return true to indicate the click was handled
                }
            })
        }
		fileChooserPreference("tilepath", 123456, R.string.p_mbtiles_file_picker_title)
		//fileChooserPreference("themefile", 123457, R.string.p_themefile_choose)
	}
	override def onResume() {
		super.onResume()
		// Get the 'tilepath' value from SharedPreferences
		val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
		val tilepath = sharedPreferences.getString("tilepath", null)

		// Update the summary of 'tilepath' if a valid path is set
		if (tilepath != null && tilepath.nonEmpty) {
			val tilepathPref = findPreference("tilepath")
			val filename = new File(tilepath).getName() // Extract the file name from the path
			tilepathPref.setSummary(s"$tilepath")
		} else {
			// If no tilepath is set, show the default summary
			val tilepathPref = findPreference("tilepath")
		}
			
		findPreference("p_connsetup").setSummary(prefs.getBackendName())
		findPreference("p_location").setSummary(prefs.getLocationSourceName())
		findPreference("p_symbol").setSummary(getString(R.string.p_symbol_summary) + ": " + prefs.getString("symbol", "/$"))
	}

	def resolveContentUri(uri : Uri) = {
		val Array(storage, path) = uri.getPath().replace("/document/", "").split(":", 2)
		android.util.Log.d("PrefsAct", "resolveContentUri s=" + storage + " p=" + path)
		
		val resolvedPath = if (storage == "primary")
			Environment.getExternalStorageDirectory() + "/" + path
		else
			"/storage/" + storage + "/" + path
		
		// Remove "/storage/raw//" if present on Fire Tablet devices
		val fixedPath = resolvedPath.replace("/storage/raw//", "")

		android.util.Log.d("PrefsAct", s"Fixed path: $fixedPath")
		fixedPath
		
	}

	def parseFilePickerResult(data : Intent, pref_name : String, error_id : Int) {
		val file = data.getData().getScheme() match {
		case "file" =>
			data.getData().getPath()
		case "content" =>
			// fix up Uri for KitKat+; http://stackoverflow.com/a/20559175/539443
			// http://stackoverflow.com/a/27271131/539443
			if ("com.android.externalstorage.documents".equals(data.getData().getAuthority())) {
				resolveContentUri(data.getData())
			} else {
				val fixup_uri = Uri.parse(data.getDataString().replace(
					"content://com.android.providers.downloads.documents/document",
					"content://downloads/public_downloads"))
				val cursor = getContentResolver().query(fixup_uri, null, null, null, null)
				cursor.moveToFirst()
				val idx = cursor.getColumnIndex("_data")
				val result = if (idx != -1) cursor.getString(idx) else null
				cursor.close()
				result
			}
		case _ =>
			null
		}
		if (file != null) {
			val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
			sharedPreferences.edit()
				.putString(pref_name, file)
				.commit()
			finish()
			startActivity(getIntent())
		} else {
			val errmsg = getString(error_id, data.getDataString())
			Toast.makeText(this, errmsg, Toast.LENGTH_SHORT).show()
			db.addPost(System.currentTimeMillis(), StorageDatabase.Post.TYPE_ERROR,
				getString(R.string.post_error), errmsg)
		}
	}

	override def onActivityResult(reqCode : Int, resultCode : Int, data : Intent) {
		android.util.Log.d("PrefsAct", "onActResult: request=" + reqCode + " result=" + resultCode + " " + data)
		if (resultCode == android.app.Activity.RESULT_OK) {
			reqCode match {
				case 123456 =>
					//parseFilePickerResult(data, "mapfile", R.string.mapfile_error)
					val takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
					getContentResolver.takePersistableUriPermission(data.getData(), takeFlags)
					val resolvedPath = data.getData().getScheme match {
						case "file" => data.getData().getPath
						case "content" => resolveContentUri(data.getData())
						case _ => null
					}

					if (resolvedPath != null) {
					PreferenceManager.getDefaultSharedPreferences(this)
							.edit().putString("tilepath", resolvedPath).commit()
						Toast.makeText(this, getString(R.string.selected_file, new File(resolvedPath).getName()), Toast.LENGTH_SHORT).show()
					} else {
						Toast.makeText(this, R.string.mapfile_error, Toast.LENGTH_SHORT).show()
					}
					finish()
					startActivity(getIntent())
				//case 123457 =>
					//parseFilePickerResult(data, "themefile", R.string.themefile_error)					
				case 123458 =>
					data.setClass(this, classOf[ProfileImportActivity])
					startActivity(data)
				case _ =>
					super.onActivityResult(reqCode, resultCode, data)
			}
		} else {
			super.onActivityResult(reqCode, resultCode, data)
		}
	}

	override def onCreateOptionsMenu(menu : Menu) : Boolean = {
		getMenuInflater().inflate(R.menu.options_prefs, menu)
		true
	}
	override def onOptionsItemSelected(mi : MenuItem) : Boolean = {
		mi.getItemId match {
		case R.id.profile_load =>
			val get_file = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*")
			// TODO: use MaterialFilePicker().withFilter() for *.aprs
			startActivityForResult(Intent.createChooser(get_file,
				getString(R.string.profile_import_activity)), 123458)
			true
		case R.id.profile_export =>
			exportPrefs()
			true
		case _ => super.onOptionsItemSelected(mi)
		}
	}

    def openAllFilesAccessSettings(): Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (API 33 and above): open All Files Access settings
            val intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.setData(Uri.parse("package:" + getPackageName()))
            startActivity(intent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 (API 30) and above but below Android 13, open App Info page directly
            val intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.setData(Uri.parse("package:" + getPackageName()))
            startActivity(intent)
        } else {
            // For older versions (Android 10 and below), open the App Info page
            val intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.setData(Uri.parse("package:" + getPackageName()))
            startActivity(intent)
        }
    }
}
