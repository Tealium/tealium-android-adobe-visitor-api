package com.tealium.adobe.api


interface GetURLParametersHandler {
    fun onRetrieveParameters(params: Map<String, String>?)
}