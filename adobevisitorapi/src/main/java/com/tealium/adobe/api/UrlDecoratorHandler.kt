package com.tealium.adobe.api

import java.net.URL

interface UrlDecoratorHandler {
    fun onDecorateUrl(url: URL)
}