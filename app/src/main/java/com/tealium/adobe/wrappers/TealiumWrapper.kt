package com.tealium.adobe.wrappers

import com.tealium.adobe.api.AdobeVisitor
import com.tealium.adobe.api.ResponseListener

interface TealiumWrapper {
    val initialVisitor: AdobeVisitor?
    fun track(eventName: String, data: Map<String, Any>?)
    fun linkExistingEcidToKnownIdentifier(knownId: String, dataProviderId: String, authState: Int?, responseListener: ResponseListener<AdobeVisitor>)
    fun clearVisitor()
}