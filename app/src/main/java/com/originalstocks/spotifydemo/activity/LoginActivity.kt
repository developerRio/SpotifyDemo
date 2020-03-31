package com.originalstocks.spotifydemo.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.originalstocks.spotifydemo.R
import com.originalstocks.spotifydemo.utils.getRedirectUri
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


class LoginActivity : AppCompatActivity() {
    val TAG = "LoginActivity"

    // spotify auth
    private val mOkHttpClient: OkHttpClient? = OkHttpClient()
    private var mAccessToken: String? = null
    private var mAccessCode: String? = null
    private var mCallUserData: Call? = null
    private var mCallUserPlaylist: Call? = null
    val AUTH_TOKEN_REQUEST_CODE = 0x10
    val AUTH_CODE_REQUEST_CODE = 0x11

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Log.i(
            TAG,
            "BuildConfig_VERSION_NAME" + com.spotify.sdk.android.auth.BuildConfig.VERSION_NAME
        )
        Log.i(TAG, "getRedirectUri = " + getRedirectUri(this))

    }

    override fun onDestroy() {
        cancelCall()
        super.onDestroy()
    }

    private fun fetchingUserProfileData(mAccessToken: String) {
        if (mAccessToken.isEmpty()) {
            val snackbar: Snackbar = Snackbar.make(
                findViewById(R.id.login_activity),
                R.string.warning_need_token,
                Snackbar.LENGTH_INDEFINITE
            )
            snackbar.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
            snackbar.show()
            return
        }
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me")
            .addHeader("Authorization", "Bearer $mAccessToken")
            .build()
        cancelCall()
        mCallUserData = mOkHttpClient!!.newCall(request)
        mCallUserData?.enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException) {
                setResponse("Failed to fetch data: $e")
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call?, response: Response) {
                try {
                    val jsonObject = JSONObject(response.body()!!.string())
                    Log.i(TAG, "onSuccessResponse = $jsonObject")
                    setResponse(jsonObject.toString(3))
                } catch (e: JSONException) {
                    setResponse("Failed to parse data: $e")
                }
            }
        })
    }

    private fun fetchingUserPlaylistsData(mAccessToken: String) {
        if (mAccessToken.isEmpty()) {
            val snackbar: Snackbar = Snackbar.make(
                findViewById(R.id.login_activity),
                R.string.warning_need_token,
                Snackbar.LENGTH_INDEFINITE
            )
            snackbar.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
            snackbar.show()
            return
        }
        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me/playlists")
            .addHeader("Authorization", "Bearer $mAccessToken")
            .build()
        if (mCallUserPlaylist != null) {
            mCallUserPlaylist?.cancel()
        }
        mCallUserPlaylist = mOkHttpClient!!.newCall(request)
        mCallUserPlaylist?.enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException) {
                e.printStackTrace()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call?, response: Response) {
                try {
                    val jsonObject = JSONObject(response.body()!!.string())
                    Log.i(TAG, "onSuccessPlaylistsResponse = $jsonObject")
                    parsePlaylistJSONData(jsonObject)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        })
    }

    private fun parsePlaylistJSONData(rootObject: JSONObject) {
        val hrefString = rootObject.getString("href").toString()
        var extLinkString = ""
        var playListName = ""
        var playlistURI = ""
        val itemsArray: JSONArray = rootObject.getJSONArray("items")
        for (i in 0 until itemsArray.length()) {
            val itemsObj: JSONObject =
                itemsArray.getJSONObject(i) // we can change it to 0 for default
            val extLinkObj: JSONObject = itemsObj.getJSONObject("external_urls")
            extLinkString = extLinkObj.getString("spotify")
            playListName = itemsObj.getString("name")
            playlistURI = itemsObj.getString("uri")
        }

        Log.i(TAG, "playList_parsedData = $hrefString\n$extLinkString\n$playListName\n$playlistURI")

    }

    fun onRequestCodeClicked(view: View?) {
        val request =
            getAuthenticationRequest(AuthorizationResponse.Type.CODE)
        AuthorizationClient.openLoginActivity(this, AUTH_CODE_REQUEST_CODE, request)
    }

    fun onRequestTokenClicked(view: View?) {
        val request =
            getAuthenticationRequest(AuthorizationResponse.Type.TOKEN)
        AuthorizationClient.openLoginActivity(this, AUTH_TOKEN_REQUEST_CODE, request)
    }

    private fun getAuthenticationRequest(type: AuthorizationResponse.Type): AuthorizationRequest {
        return AuthorizationRequest.Builder(
            getString(R.string.client_id),
            type,
            getRedirectUri(this).toString()
        )
            .setShowDialog(false)
            .setScopes(
                arrayOf(
                    "user-read-email",
                    "user-read-recently-played",
                    "user-read-private",
                    "user-library-read",
                    "playlist-read-collaborative",
                    "playlist-read-private",
                    "playlist-modify-private",
                    "playlist-modify-public"
                )
            ).build()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        val response = AuthorizationClient.getResponse(resultCode, data)

        Log.i(TAG, "onActivityResult_response = ${response.accessToken}")

        if (response.error != null && response.error.isEmpty()) {
            setResponse(response.error)
        }
        if (requestCode == AUTH_TOKEN_REQUEST_CODE) {
            mAccessToken = response.accessToken
            Log.e(TAG, "AccessToken = " + response.accessToken.toString())
            updateTokenView()
            fetchingUserProfileData(response.accessToken.toString())

            /** if user is authenticated sending it to home !*/
            if (response.accessToken.toString().isNotEmpty()) {
                fetchingUserPlaylistsData(response.accessToken.toString())
                //startActivity(Intent(this, MainActivity::class.java))
            }
        } else if (requestCode == AUTH_CODE_REQUEST_CODE) {
            mAccessCode = response.code
            updateCodeView()
        }
    }

    private fun setResponse(text: String) {
        runOnUiThread {
            val responseView = findViewById<TextView>(R.id.response_text_view)
            responseView.text = text
        }
    }

    private fun updateTokenView() {
        val tokenView = findViewById<TextView>(R.id.token_text_view)
        tokenView.text = getString(R.string.token, mAccessToken)
        Log.e(TAG, "onSuccessResponse_mAccessToken = $mAccessToken")
    }

    private fun updateCodeView() {
        val codeView = findViewById<TextView>(R.id.code_text_view)
        codeView.text = getString(R.string.code, mAccessCode)
        Log.e(TAG, "onSuccessResponse_mAccessCode = $mAccessCode")
    }

    private fun cancelCall() {
        if (mCallUserData != null) {
            mCallUserData?.cancel()
        }

    }


}
