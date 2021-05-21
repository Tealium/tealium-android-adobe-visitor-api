package com.tealium.adobe.api.network

import com.tealium.adobe.api.ResponseListener
import java.net.URL

interface NetworkClient {
    /**
     * Determines whether there is connectivity available.
     * @return true if connectivity available, else false
     */
    fun isConnected(): Boolean

    /**
     * Makes an asynchronous http GET request to the given [url]. String response data will be
     * returned via the [listener]
     */
    fun get(url: URL, listener: ResponseListener<String>)
}