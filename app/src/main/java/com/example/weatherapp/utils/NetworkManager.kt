package com.example.weatherapp.utils

import android.content.ContentValues.TAG
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

/**
 * Class to monitor real-time network status changes.
 */
class NetworkManager(context: Context) {

    // Access system's connectivity service
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Interface to communicate network events
     */
    interface NetworkListener {
        fun onNetworkAvailable()
        fun onNetworkLost()
    }

    // Reference to the callback so it can be unregistered later to prevent memory leak
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * Function to monitor the systems network status
     */
    fun observeNetwork(listener: NetworkListener) {
        // Define what kind of network we are looking for
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // Create the callback object that triggers when network event occurs
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Triggered when network with internet capability is connected
                listener.onNetworkAvailable()
            }

            override fun onLost(network: Network) {
                // Triggered when network is disconnected
                listener.onNetworkLost()
            }
        }

        // Register callback with system
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
    }

    /**
     * Function to stop monitoring network changes
     */
    fun stopObserving() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
    }

    /**
     * Helper to check current status quickly
     */
    fun isOnline(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    /**
     * Executes a POST request (Used for Google API)
     *
     * @param urlString the endpoint url
     * @param jsonBody  JSON formatted string to send
     * @return  Server response
     */
    fun performPostRequest(urlString: String, jsonBody: String): String {
        val url = URL(urlString)
        // Open a connection the URL
        val conn = url.openConnection() as HttpURLConnection
        return try {
            // Set the HTTP method
            conn.requestMethod = "POST"

            // Define the content type so the server knows it's json
            conn.setRequestProperty("Content-Type", "application/json")

            // Enable doOutput because a post request needs to send data
            conn.doOutput = true

            // Open the output stream and write the JSON bytes into the request body
            conn.outputStream.use { it.write(jsonBody.toByteArray()) }

            // Get the HTTP responseCode
            val responseCode = conn.responseCode

            // If the response is in the successful range, return server response
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw Exception("HTTP Error")
            }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Executes a GET request (Used for Google API)
     *
     * @param urlString API url
     * @return Raw response from request
     */
    fun performGetRequest(urlString: String): String {
        // Convert string into URL object
        val url = URL(urlString)

        // Open connection to server
        val conn = url.openConnection() as HttpURLConnection
        return try {
            // Define the request method
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")

            // Get the HTTP response code
            val responseCode = conn.responseCode

            // Check if response code is in the successful range
            if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw Exception("HTTP Error")
            }
        } finally {
            conn.disconnect()
        }
    }
}