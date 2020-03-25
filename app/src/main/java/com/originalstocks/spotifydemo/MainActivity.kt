package com.originalstocks.spotifydemo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.PlayerState
import com.spotify.protocol.types.Track
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private var mSpotifyAppRemote: SpotifyAppRemote? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playMeButton.text = "Connecting..."
        playMeButton.isEnabled = false

    }

    override fun onStart() {
        super.onStart()

        // Set the connection parameters
        // TODO: Please update your Client ID in strings.xml
        val connectionParams = ConnectionParams.Builder(getString(R.string.client_id))
                .setRedirectUri("https://www.google.com/")
                .showAuthView(true)
                .build()

        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onFailure(throwable: Throwable?) {
                Log.e("MainActivity", throwable?.message, throwable)

            }

            override fun onConnected(spotifyAppRemote: SpotifyAppRemote?) {
                playMeButton.isEnabled = true
                Log.d("MainActivity", "Connected! Yay!")
                mSpotifyAppRemote = spotifyAppRemote
                // Now you can start interacting with App Remote
                playMeButton.text = "Connected ! Play Now 😎"
            }
        })

        playMeButton.setOnClickListener {

            mSpotifyAppRemote?.playerApi?.play("spotify:playlist:37i9dQZF1DWY4xHQp97fN6") // spotify:playlist:37i9dQZF1DWY4xHQp97fN6

            // Subscribe to PlayerState
            mSpotifyAppRemote!!.playerApi
                    .subscribeToPlayerState()
                    .setEventCallback { playerState: PlayerState ->
                        val track: Track? = playerState.track
                        if (track != null) {
                            Log.d("MainActivity", track.name.toString() + " by " + track.artist.name)
                        }
                    }

        }


    }

    override fun onStop() {
        super.onStop()
        SpotifyAppRemote.disconnect(mSpotifyAppRemote)
    }

}
