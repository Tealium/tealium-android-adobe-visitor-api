package com.tealium.adobe.api


interface GetUrlParametersHandler {
    fun onRetrieveParameters(params: Map<String, String>?)
}