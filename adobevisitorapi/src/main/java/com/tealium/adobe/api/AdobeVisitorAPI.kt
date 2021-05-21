package com.tealium.adobe.api

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tealium.adobe.api.network.HttpClient
import com.tealium.adobe.api.network.NetworkClient
import org.json.JSONException
import org.json.JSONObject
import java.net.URL
import java.net.URLDecoder

/**
 * Implementation of the [AdobeExperienceCloudIdService] to allow requesting and linking of
 * Experience Cloud Ids
 */
class AdobeVisitorAPI @JvmOverloads constructor(
    private val context: Context,
    private val orgId: String,
    private val networkClient: NetworkClient = HttpClient(context)
) : AdobeExperienceCloudIdService {

    private val demdexUrl = Uri.Builder()
        .scheme(API_DEFAULT_VISITOR_SCHEME)
        .authority(API_DEFAULT_VISITOR_AUTHORITY)
        .path(API_DEFAULT_VISITOR_PATH)
        .build()
    private val cidSeparator =
        URLDecoder.decode(API_DATA_PROVIDER_ID_SEPARATOR, Charsets.UTF_8.name())

    override fun requestNewAdobeEcid(adobeResponseListener: ResponseListener<AdobeVisitor>?) {
        val uri = demdexUrl.buildUpon()
            .appendQueryParameter(QP_ORG_ID, orgId)
            .appendQueryParameter(QP_VERSION, API_VERSION.toString())
            .build()

        sendRequest(URL(uri.toString()), adobeResponseListener)
    }

    override fun refreshExistingAdobeEcid(
        experienceCloudId: String,
        adobeResponseListener: ResponseListener<AdobeVisitor>?
    ) {
        val uri = demdexUrl.buildUpon()
            .appendQueryParameter(QP_ORG_ID, orgId)
            .appendQueryParameter(QP_VERSION, API_VERSION.toString())
            .appendQueryParameter(QP_EXPERIENCE_CLOUD_ID, experienceCloudId)
            .build()

        sendRequest(URL(uri.toString()), adobeResponseListener)
    }

    override fun linkEcidToKnownIdentifier(
        knownId: String,
        experienceCloudId: String,
        adobeDataProviderId: String,
        authState: Int?,
        adobeResponseListener: ResponseListener<AdobeVisitor>?
    ) {
        val uri = demdexUrl.buildUpon()
            .appendQueryParameter(QP_ORG_ID, orgId)
            .appendQueryParameter(QP_VERSION, API_VERSION.toString())
            .appendQueryParameter(QP_EXPERIENCE_CLOUD_ID, experienceCloudId)
            .appendQueryParameter(
                QP_DATA_PROVIDER_ID,
                generateCid(adobeDataProviderId, knownId, authState)
            )
            .build()

        sendRequest(URL(uri.toString()), adobeResponseListener)
    }

    override fun requestNewEcidAndLink(
        knownId: String,
        adobeDataProviderId: String,
        authState: Int?,
        adobeResponseListener: ResponseListener<AdobeVisitor>?
    ) {
        requestNewAdobeEcid(object :
            ResponseListener<AdobeVisitor> {
            override fun success(data: AdobeVisitor) {
                linkEcidToKnownIdentifier(
                    knownId,
                    data.experienceCloudId,
                    adobeDataProviderId,
                    authState,
                    adobeResponseListener
                )
            }

            override fun failure(errorCode: Int, ex: Exception?) {
                adobeResponseListener?.failure(errorCode, ex)
            }
        })
    }

    private fun generateCid(
        dataProviderId: String,
        customVisitorId: String,
        authenticationState: Int? = null
    ): String {
        return arrayOf(
            dataProviderId,
            customVisitorId,
            authenticationState ?: AdobeAuthState.AUTH_STATE_UNKNOWN
        ).joinToString(separator = cidSeparator)
    }

    private fun sendRequest(url: URL, adobeResponseListener: ResponseListener<AdobeVisitor>?) {
        networkClient.get(url, object :
            ResponseListener<String> {
            override fun success(data: String) {
                Log.d(BuildConfig.TAG, "Retrieved data: $data")
                try {
                    val json = JSONObject(data)
                    AdobeVisitor.fromJson(json)?.let {
                        adobeResponseListener?.success(it)
                    } ?: adobeResponseListener?.failure(
                        ErrorCode.ERROR_INVALID_VISITOR_JSON,
                        null
                    )
                } catch (jsonEx: JSONException) {
                    adobeResponseListener?.failure(ErrorCode.ERROR_INVALID_VISITOR_JSON, jsonEx)
                }
            }

            override fun failure(errorCode: Int, ex: Exception?) {
                Log.w(BuildConfig.TAG, "Request failed with error code: $errorCode")
                ex?.let {
                    Log.w(BuildConfig.TAG, "${ex.message}")
                }
                adobeResponseListener?.failure(errorCode, ex)
            }
        })
    }
}