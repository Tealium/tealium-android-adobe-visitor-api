package com.tealium.adobe.kotlin

import com.tealium.adobe.api.AdobeAuthState
import com.tealium.core.*

val Collectors.AdobeVisitor: CollectorFactory
    get() = AdobeVisitorModule

val Tealium.adobeVisitorApi: AdobeVisitorModule?
    get() = modules.getModule(AdobeVisitorModule::class.java)

const val ADOBE_VISITOR_ORG_ID = "adobe_visitor_org_id"
const val ADOBE_VISITOR_EXISTING_ECID = "adobe_visitor_existing_ecid"
const val ADOBE_VISITOR_RETRIES = "adobe_visitor_retries"
const val ADOBE_VISITOR_AUTH_STATE = "adobe_visitor_auth_state"
const val ADOBE_VISITOR_DATA_PROVIDER_ID = "adobe_visitor_data_provider_id"
const val ADOBE_VISITOR_CUSTOM_VISITOR_ID = "adobe_visitor_custom_visitor_id"

/**
 *  Sets the Adobe OrgId for fetching/updating visitor Ids.
 */
var TealiumConfig.adobeVisitorOrgId: String?
    get() = options[ADOBE_VISITOR_ORG_ID] as? String
    set(value) {
        value?.let {
            options[ADOBE_VISITOR_ORG_ID] = it
        } ?: options.remove(ADOBE_VISITOR_ORG_ID)
    }

/**
 *  Sets an initial known ECID for this visitor - it is only used to fetch/refresh the visitor, and
 *  subsequent launches should load the visitor data from cache.
 */
var TealiumConfig.adobeVisitorExistingEcid: String?
    get() = options[ADOBE_VISITOR_EXISTING_ECID] as? String
    set(value) {
        value?.let {
            options[ADOBE_VISITOR_EXISTING_ECID] = it
        } ?: options.remove(ADOBE_VISITOR_EXISTING_ECID)
    }

/**
 *  Sets the maximum number of attempts to fetch a new visitor when none is available.
 *  Values of 0 or less will not auto-fetch a new visitor.
 *  Values greater than 0 will try and fetch a maximum of [adobeVisitorRetries] times before
 *  allowing any tracking calls to continue without an ECID - you can specify an initial id using
 *  [adobeVisitorExistingEcid] if it's known.
 */
var TealiumConfig.adobeVisitorRetries: Int?
    get() = options[ADOBE_VISITOR_RETRIES] as? Int
    set(value) {
        value?.let {
            options[ADOBE_VISITOR_RETRIES] = it
        } ?: options.remove(ADOBE_VISITOR_RETRIES)
    }

/**
 * Sets the AuthState during the initial fetching of a new ECID or refresh of an existing ECID.
 */
var TealiumConfig.adobeVisitorAuthState: Int?
    @AdobeAuthState get() = options[ADOBE_VISITOR_AUTH_STATE] as? Int
    set(@AdobeAuthState value) {
        value?.let {
            options[ADOBE_VISITOR_AUTH_STATE] = it
        } ?: options.remove(ADOBE_VISITOR_AUTH_STATE)
    }

/**
 * Sets the DataProvider Id during the initial fetching of a new ECID or refresh of an existing ECID.
 */
var TealiumConfig.adobeVisitorDataProviderId: String?
    get() = options[ADOBE_VISITOR_DATA_PROVIDER_ID] as? String
    set(value) {
        value?.let {
            options[ADOBE_VISITOR_DATA_PROVIDER_ID] = it
        } ?: options.remove(ADOBE_VISITOR_DATA_PROVIDER_ID)
    }

/**
 * Sets a Known Visitor Id during the initial fetching of a new ECID or refresh of an existing ECID.
 */
var TealiumConfig.adobeVisitorCustomVisitorId: String?
    get() = options[ADOBE_VISITOR_CUSTOM_VISITOR_ID] as? String
    set(value) {
        value?.let {
            options[ADOBE_VISITOR_CUSTOM_VISITOR_ID] = it
        } ?: options.remove(ADOBE_VISITOR_CUSTOM_VISITOR_ID)
    }
