package com.tealium.adobe.api

interface ResponseListener<T> {
    fun success(data: T)
    fun failure(@ErrorCode errorCode: Int, ex: Exception? = null)
}