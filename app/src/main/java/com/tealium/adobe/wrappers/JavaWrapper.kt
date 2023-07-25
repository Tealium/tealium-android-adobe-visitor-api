package com.tealium.adobe.wrappers

import android.app.Application
import com.tealium.adobe.BuildConfig
import com.tealium.adobe.api.AdobeVisitor
import com.tealium.adobe.api.GetUrlParametersHandler
import com.tealium.adobe.api.ResponseListener
import com.tealium.adobe.api.UrlDecoratorHandler
import com.tealium.adobe.java.AdobeVisitorModule
import com.tealium.library.Tealium
import java.net.URL

class JavaWrapper(app: Application) : TealiumWrapper {

    private val tealium: Tealium
    private val adobeVisitorModule: AdobeVisitorModule = AdobeVisitorModule.setUp(
        app.applicationContext,
        BuildConfig.ADOBE_ORG_ID,
        null
    )

    override val initialVisitor: AdobeVisitor?
        get() = adobeVisitorModule.visitor

    init {
        val config = Tealium.Config.create(
            app,
            BuildConfig.TEALIUM_ACCOUNT,
            BuildConfig.TEALIUM_PROFILE,
            BuildConfig.TEALIUM_ENVIRONMENT
        )
        config.eventListeners.add(adobeVisitorModule)
        tealium = Tealium.createInstance(BuildConfig.TEALIUM_INSTANCE, config)
    }

    override fun track(eventName: String, data: Map<String, Any>?) {
        tealium.trackEvent(eventName, data)
    }

    override fun linkExistingEcidToKnownIdentifier(
        knownId: String,
        dataProviderId: String,
        authState: Int?,
        responseListener: ResponseListener<AdobeVisitor>
    ) {
        adobeVisitorModule.linkEcidToKnownIdentifier(knownId, dataProviderId, authState, responseListener)
    }

    override fun getUrlParameters(handler: GetUrlParametersHandler) {
        adobeVisitorModule.getUrlParameters(handler)
    }

    override fun clearVisitor() {
        adobeVisitorModule.resetVisitor()
    }

    override fun decorateUrl(url: String, handler: UrlDecoratorHandler) {
        adobeVisitorModule.decorateUrl(URL(url), handler)
    }

}