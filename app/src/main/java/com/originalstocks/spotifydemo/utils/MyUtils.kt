package com.originalstocks.spotifydemo.utils

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.originalstocks.spotifydemo.R

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

fun getRedirectUri(context: Context): Uri {
    return Uri.Builder()
        .scheme(context.getString(R.string.com_spotify_sdk_redirect_scheme))
        .authority(context.getString(R.string.com_spotify_sdk_redirect_host))
        .appendPath("tera-clock-callback")
        .build()
}