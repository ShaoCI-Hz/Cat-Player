package com.example.smbplayer.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.smbplayer.MainActivity

/**
 * Quick Settings Tile for Cat Player.
 * Shows play/pause state and toggles playback on tap.
 */
class PlayerTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        // Launch the app
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivityAndCollapse(intent)
    }

    private fun updateTile() {
        qsTile?.let { tile ->
            tile.label = "Cat Player"
            tile.contentDescription = "打开 Cat Player"
            tile.state = Tile.STATE_ACTIVE
            tile.updateTile()
        }
    }
}
