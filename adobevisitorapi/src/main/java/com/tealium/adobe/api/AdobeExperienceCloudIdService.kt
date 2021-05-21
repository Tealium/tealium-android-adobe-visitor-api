package com.tealium.adobe.api

interface AdobeExperienceCloudIdService {

    /**
     * Requests a new Experience Cloud Id to identify this Visitor.
     * New [AdobeVisitor] will provided via the [adobeResponseListener] when available.
     */
    fun requestNewAdobeEcid(adobeResponseListener: ResponseListener<AdobeVisitor>?)

    /**
     * Requests an update to an existing Experience Cloud Id identifying this Visitor.
     * Updated [AdobeVisitor] will provided via the [adobeResponseListener] when available.
     */
    fun refreshExistingAdobeEcid(
        experienceCloudId: String,
        adobeResponseListener: ResponseListener<AdobeVisitor>?
    )

    /**
     * Links a known user id (e.g. an email address) to an existing Experience Cloud Id identifying
     * this Visitor.
     * Updated [AdobeVisitor] will provided via the [adobeResponseListener] when available.
     */
    fun linkEcidToKnownIdentifier(
        knownId: String,
        experienceCloudId: String,
        adobeDataProviderId: String,
        @AdobeAuthState authState: Int?,
        adobeResponseListener: ResponseListener<AdobeVisitor>?
    )

    /**
     * Requests a new Experience cloud and subsequently links a known user id (e.g. an email address)
     * to the new ECID.
     * Updated [AdobeVisitor] will provided via the [adobeResponseListener] when available.
     */
    fun requestNewEcidAndLink(
        knownId: String,
        adobeDataProviderId: String,
        @AdobeAuthState authState: Int?,
        adobeResponseListener: ResponseListener<AdobeVisitor>?
    )
}