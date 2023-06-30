package com.tealium.adobe.api


interface GetUrlParamsHandler {
    fun onRetrieveParams(params: Map<String, String>?)
}