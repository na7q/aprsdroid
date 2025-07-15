package org.aprsdroid.app;

import android.content.Context; // Import Context
import android.util.Log; // Import Log for logging
import org.mapsforge.v3.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.v3.core.Tile;

public class OsmTileDownloader extends TileDownloader {
    private static final String HOST_NAME_ONLINE = "tile.openstreetmap.org";
    private static final String HOST_NAME_OFFLINE = null; // New hostname for offline maps
    private static final byte ZOOM_MAX = 18;
    private final StringBuilder stringBuilder = new StringBuilder();
    private static final String TAG = "OsmTileDownloader"; // Tag for logging
    private final PrefsWrapper prefsWrapper; // Instance of PrefsWrapper

    // Constructor that accepts a PrefsWrapper instance
    public OsmTileDownloader(PrefsWrapper prefsWrapper) {
        this.prefsWrapper = prefsWrapper;
    }

    // Factory method to create an instance
    public static OsmTileDownloader create(Context context) {
        PrefsWrapper prefsWrapper = new PrefsWrapper(context);
        return new OsmTileDownloader(prefsWrapper);
    }

    @Override
    public String getHostName() {
        // If offline mode is enabled, get the path to the offline map data
        if (prefsWrapper.isOfflineMap()) {
			String tilePath = prefsWrapper.getTilePath();  // This will return the tile path from preferences
            Log.d(TAG, "Getting offline host name: " + tilePath); // Log the offline tile path
            return tilePath; // Return the file path for offline maps
        } else {
            Log.d(TAG, "Getting online host name: " + HOST_NAME_ONLINE); // Log online hostname
            return HOST_NAME_ONLINE; // Online host
        }
	}

    @Override
    public String getProtocol() {
        String protocol = prefsWrapper.isOfflineMap() ? null : "https"; // Use HTTP for offline maps
        Log.d(TAG, "Getting protocol: " + protocol); // Log protocol
        return protocol;
    }

    @Override
    public int getPort() {
        int port = prefsWrapper.isOfflineMap() ? -1 : 443; // Use port 8080 for offline maps
        Log.d(TAG, "Getting port: " + port); // Log port
        return port;
    }

    @Override
    public String getTilePath(Tile tile) {
        this.stringBuilder.setLength(0);
		this.stringBuilder.append('/');
        this.stringBuilder.append(tile.zoomLevel);
        this.stringBuilder.append('/');
        this.stringBuilder.append(tile.tileX);
        this.stringBuilder.append('/');
        this.stringBuilder.append(tile.tileY);
        this.stringBuilder.append(".png");
        
        String tilePath = this.stringBuilder.toString();
        Log.d(TAG, "Generated tile path: " + tilePath); // Log the generated tile path
        return tilePath;
    }

    @Override
    public byte getZoomLevelMax() {
        Log.d(TAG, "Getting maximum zoom level: " + ZOOM_MAX); // Log max zoom level
        return ZOOM_MAX;
    }
}
