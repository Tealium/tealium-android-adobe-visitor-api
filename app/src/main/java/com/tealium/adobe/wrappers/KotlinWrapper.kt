package com.tealium.adobe.wrappers

import android.app.Application
import com.tealium.adobe.BuildConfig
import com.tealium.adobe.api.AdobeVisitor
import com.tealium.adobe.api.ResponseListener
import com.tealium.adobe.kotlin.AdobeVisitor
import com.tealium.adobe.kotlin.adobeVisitorApi
import com.tealium.adobe.kotlin.adobeVisitorOrgId
import com.tealium.collectdispatcher.Collect
import com.tealium.core.*
import com.tealium.dispatcher.TealiumEvent
import java.util.*

class KotlinWrapper(app: Application) : TealiumWrapper {

    private val tealium: Tealium

    override val initialVisitor: AdobeVisitor?
        get() = tealium.adobeVisitorApi?.visitor

    init {
        val config = TealiumConfig(
            app,
            BuildConfig.TEALIUM_ACCOUNT,
            BuildConfig.TEALIUM_PROFILE,
            Environment.valueOf(
                BuildConfig.TEALIUM_ENVIRONMENT.toUpperCase(
                    Locale.ROOT
                )
            ),
            collectors = mutableSetOf(Collectors.AdobeVisitor),
            dispatchers = mutableSetOf(Dispatchers.Collect)
        )
        config.adobeVisitorOrgId = BuildConfig.ADOBE_ORG_ID
        tealium = Tealium.create(BuildConfig.TEALIUM_INSTANCE, config)
    }

    override fun track(eventName: String, data: Map<String, Any>?) {
        tealium.track(TealiumEvent(eventName, data))
    }

    override fun linkExistingEcidToKnownIdentifier(
        knownId: String,
        dataProviderId: String,
        authState: Int?,
        responseListener: ResponseListener<AdobeVisitor>
    ) {
        tealium.adobeVisitorApi?.linkEcidToKnownIdentifier(knownId, dataProviderId, authState, responseListener)
    }

    override fun clearVisitor() {
        tealium.adobeVisitorApi?.resetVisitor()
    }
}