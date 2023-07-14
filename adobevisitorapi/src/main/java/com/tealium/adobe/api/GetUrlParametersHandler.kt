package com.tealium.adobe.api


interface GetURLParametersHandler {
    fun onRetrieveParams(params: Map<String, String>?)
}