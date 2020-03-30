package com.originalstocks.spotifydemo.activity

import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.originalstocks.spotifydemo.R
import com.originalstocks.spotifydemo.broadcastReceiver.SpotifyBroadcastReceiver
import com.originalstocks.spotifydemo.utils.getRedirectUri
import com.originalstocks.spotifydemo.utils.showToast
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.ContentApi
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.error.SpotifyDisconnectedException
import com.spotify.protocol.types.Image
import com.spotify.protocol.types.PlayerState
import com.spotify.protocol.types.Track
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    private var mSpotifyAppRemote: SpotifyAppRemote? = null
    private var connectionParams: ConnectionParams? = null
    private var spotifyBroadcastReceiver = SpotifyBroadcastReceiver()
    val spotifyMyPlaylistURI = "spotify:playlist:0E6OYw9qGFgaKQPJqZVhHy"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playMeButton.text = "Connecting..."
        playMeButton.isEnabled = false


    }

    override fun onStart() {
        super.onStart()

        // Set the connection parameters
        if (SpotifyAppRemote.isSpotifyInstalled(this)) {
            // installed
            val intentFilter = IntentFilter()
            intentFilter.addAction("com.spotify.music.active")
            registerReceiver(spotifyBroadcastReceiver, intentFilter)

            connectionParams =
                ConnectionParams.Builder(getString(R.string.client_id))             // TODO: Please update your Client ID in strings.xml
                    .setRedirectUri(getRedirectUri(this).toString())
                    .showAuthView(true)
                    .build()

            SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
                override fun onFailure(throwable: Throwable?) {
                    Log.e(TAG, throwable?.message, throwable)

                }

                override fun onConnected(spotifyAppRemote: SpotifyAppRemote?) {
                    // Now you can start interacting with App Remote
                    playMeButton.isEnabled = true
                    Log.d(TAG, "Connected! Yay!")
                    mSpotifyAppRemote = spotifyAppRemote

                    playMeButton.text = "Connected ! Play Now 😎"
                    Log.d(
                        TAG,
                        "getRecommendedContentItems = " + spotifyAppRemote?.contentApi?.getRecommendedContentItems(
                            ContentApi.ContentType.DEFAULT
                        )
                    )
                }
            })

        } else {
            // not installed
            playMeButton.text = "Please install Spotify"
            showToast(this, "Please Install the Spotify to use this Feature.")
        }

        // getRecommendedContentItems = mSpotifyAppRemote?.contentApi?.getRecommendedContentItems(ContentApi.ContentType.DEFAULT) as CallResult<ListItems>


        playMeButton.setOnClickListener {
            mSpotifyAppRemote?.playerApi?.play(spotifyMyPlaylistURI) // spotify:playlist:37i9dQZF1DWY4xHQp97fN6

            Log.d(TAG, "imageURI = ${mSpotifyAppRemote?.imagesApi}")

            // Subscribe to PlayerState
            mSpotifyAppRemote!!.playerApi.subscribeToPlayerState()
                .setEventCallback { playerState: PlayerState ->
                    val track: Track? = playerState.track
                    if (track != null) {
                        Log.d(TAG, track.name.toString() + " by " + track.artist.name)
                    }
                }
            mSpotifyAppRemote!!.playerApi.subscribeToPlayerState()
                .setEventCallback { playerState ->
                    updateTrackCoverArt(playerState)
                    if (playerState.isPaused) {
                        Log.i(TAG, "player_state = " + playerState.track.name)

                    }
                }
                .setErrorCallback { throwable ->
                    Log.e(TAG, throwable.message.toString())
                }
            Log.i(TAG, "isSpotifyInstalled = " + "")

        }


    }


    private fun updateTrackCoverArt(playerState: PlayerState) {
        // Get image from track
        assertAppRemoteConnected()
            .imagesApi
            .getImage(playerState.track.imageUri, Image.Dimension.LARGE)
            .setResultCallback { bitmap ->
                Log.d(
                    TAG,
                    "ImageURI = " + playerState.track.imageUri + " dimens = " + bitmap.width + " " + bitmap.height
                )
                coverArtImageView.setImageBitmap(bitmap)
                trackNameTextView.text = playerState.track.name
            }
    }

    private fun setResponse(text: String) {
        runOnUiThread {
            /*val responseView = findViewById<TextView>(R.id.response_text_view)
            responseView.text = text*/
            Log.d(TAG, "setResponse = $text")

        }
    }

    override fun onStop() {
        unregisterReceiver(spotifyBroadcastReceiver)
        super.onStop()
        SpotifyAppRemote.disconnect(mSpotifyAppRemote)
    }

    private fun assertAppRemoteConnected(): SpotifyAppRemote {
        mSpotifyAppRemote?.let {
            if (it.isConnected) {
                return it
            }
        }
        Log.e(TAG, getString(R.string.err_spotify_disconnected))
        throw SpotifyDisconnectedException()
    }


}
