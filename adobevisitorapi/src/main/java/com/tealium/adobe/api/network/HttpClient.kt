package com.tealium.adobe.api.network

import android.app.Service
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import com.tealium.adobe.api.ErrorCode
import com.tealium.adobe.api.ResponseListener
import java.io.BufferedReader
import java.net.URL
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

class HttpClient @JvmOverloads constructor(
    context: Context,
    private val executor: Executor,
) : NetworkClient {

    private val connectivityManager =
        context.applicationContext.getSystemService(Service.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun isConnected(): Boolean {
        var connected = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectivityManager.allNetworks.forEach { network ->
                connectivityManager.getNetworkInfo(network)?.apply {
                    connected = connected or isConnected
                }
            }
        } else {
            connectivityManager.activeNetworkInfo?.let {
                connected = it.isConnected && (
                    it.type == ConnectivityManager.TYPE_WIFI ||
                            it.type == ConnectivityManager.TYPE_MOBILE)
            }
        }
        return connected
    }

    override fun get(url: URL, listener: ResponseListener<String>) {
        if (!isConnected()) {
            listener.failure(ErrorCode.ERROR_NOT_CONNECTED)
            return
        }
        executor.execute {
            with(url.openConnection() as HttpsURLConnection) {
                requestMethod = "GET"
                var reader: BufferedReader? = null
                try {
                    reader = inputStream.bufferedReader(Charsets.UTF_8)
                    val response = reader.readText()
                    listener.success(response)
                } catch (ex: Exception) {
                    listener.failure(ErrorCode.ERROR_FAILED_REQUEST, ex)
                } finally {
                    reader?.close()
                }
            }
        }
    }
}