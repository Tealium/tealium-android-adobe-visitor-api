@file:JvmName("AdobeVisitorModule")

package com.tealium.adobe.kotlin

import android.content.SharedPreferences
import android.net.Uri
import com.tealium.adobe.api.*
import com.tealium.adobe.api.network.HttpClient
import com.tealium.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import java.net.URL
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AdobeVisitorModule(
    private val context: TealiumContext,
    private val executor: Executor = context.config.adobeVisitorExecutor ?: Dispatchers.IO.asExecutor(),
    private val visitorApi: AdobeExperienceCloudIdService = AdobeVisitorAPI(
        context.config.application,
        context.config.adobeVisitorOrgId ?: "",
        executor,
        HttpClient(
            context.config.application.applicationContext,
            executor
        )
    ),
    private val sharedPreferences: SharedPreferences = context.config.application.getSharedPreferences(
        getSharedPreferencesName(
            context.config
        ), 0
    ),
    maxRetries: Int = context.config.adobeVisitorRetries ?: 5,
    existingEcid: String? = context.config.adobeVisitorExistingEcid,
    dataProviderId: String? = context.config.adobeVisitorDataProviderId,
    @AdobeAuthState authState: Int? = context.config.adobeVisitorAuthState,
    customVisitorId: String? = context.config.adobeVisitorCustomVisitorId
) : Collector, QueryParameterProvider {

    override var enabled: Boolean = true
    override val name: String = MODULE_NAME

    private val maxRetries: Int = maxRetries.coerceAtLeast(0)
    private var autoFetchVisitorDeferred: Deferred<AdobeVisitor?>? = null
    private val backgroundScope = CoroutineScope(Dispatchers.IO)

    private var _visitor: AdobeVisitor? = AdobeVisitor.fromSharedPreferences(sharedPreferences)
        set(value) {
            field = value
            value?.let {
                AdobeVisitor.toSharedPreferences(sharedPreferences, it)
            } ?: sharedPreferences.edit().clear().apply()
        }

    val adobeOrgId = context.config.adobeVisitorOrgId

    /**
     * Gets the current AdobeVisitor
     */
    val visitor: AdobeVisitor?
        get() = _visitor

    init {
        if (!existingEcid.isNullOrEmpty()) {
            // Initial visitor id provided
            _visitor = AdobeVisitor(existingEcid, -1, 0, "")
        }

        val ecid = _visitor?.experienceCloudId
        if (ecid != null) {
            if (dataProviderId != null &&
                customVisitorId != null
            ) {
                visitorApi.linkEcidToKnownIdentifier(
                    customVisitorId,
                    ecid,
                    dataProviderId,
                    authState,
                    AdobeVisitorListener(null)
                )
            } else {
                syncVisitor()
            }
        } else {
            runBlocking {
                fetchInitialVisitorId(customVisitorId, dataProviderId, authState)
            }
        }
    }

    private fun syncVisitor() {
        _visitor?.let { v ->
            if (v.nextRefresh < Date()) {
                refreshExistingAdobeEcid(v.experienceCloudId, AdobeVisitorListener(null))
            }
        }
    }

    private fun checkRequiredConfig(): Boolean {
        return adobeOrgId?.isNotBlank() ?: false.also {
            Logger.qa(
                BuildConfig.TAG,
                "Missing required config: adobeOrgId"
            )
        }
    }

    fun resetVisitor() {
        _visitor = null
        autoFetchVisitorDeferred = null
    }

    fun linkEcidToKnownIdentifier(
        knownId: String,
        adobeDataProviderId: String,
        authState: Int?,
        adobeResponseListener: ResponseListener<AdobeVisitor>?
    ) {
        if (!checkRequiredConfig()) return

        _visitor?.let { v ->
            visitorApi.linkEcidToKnownIdentifier(
                knownId,
                v.experienceCloudId,
                adobeDataProviderId,
                authState,
                AdobeVisitorListener(adobeResponseListener)
            )
        }
    }

    fun decorateUrl(url: URL, handler: UrlDecoratorHandler) {
        backgroundScope.launch {
            val params = provideParameters()

            val uriBuilder = Uri.parse(url.toURI().toString()).buildUpon()

            if (params.isEmpty()) {
                handler.onDecorateUrl(url)
            }

            params.forEach { entry ->
                entry.value.forEach { value ->
                    uriBuilder.appendQueryParameter(entry.key, value)
                }
            }
            handler.onDecorateUrl(URL(uriBuilder.build().toString()))
        }
    }

    private fun refreshExistingAdobeEcid(
        experienceCloudId: String,
        adobeResponseListener: ResponseListener<AdobeVisitor>?
    ) {
        if (!checkRequiredConfig()) return

        visitorApi.refreshExistingAdobeEcid(
            experienceCloudId,
            AdobeVisitorListener(adobeResponseListener)
        )
    }

    override suspend fun provideParameters(): Map<String, List<String>> {
        if (visitor == null && maxRetries > 0) {
            val deferred = fetchInitialVisitorId()
            if (!deferred.isCompleted) {
                Logger.dev(BuildConfig.TAG, "Awaiting ecid")
                deferred.await()
            }
        }

        return visitor?.let { v ->
            mapOf(
                QP_ADOBE_MC to listOf(
                    "$QP_MCID=${v.experienceCloudId}$QP_SEPARATOR" +
                            "$QP_MCORGID=$adobeOrgId$QP_SEPARATOR" +
                            "$QP_TS=${System.currentTimeMillis() / 1000}"
                )
            )
        } ?: emptyMap()
    }

    override suspend fun collect(): Map<String, Any> {
        if (visitor == null && maxRetries > 0) {
            val deferred = fetchInitialVisitorId()
            if (!deferred.isCompleted) {
                Logger.dev(BuildConfig.TAG, "Awaiting ecid")
                deferred.await()
            }
        }

        return visitor?.let {
            mapOf("adobe_ecid" to it.experienceCloudId)
        } ?: emptyMap()
    }

    private suspend fun fetchInitialVisitorId(
        customVisitorId: String? = null,
        dataProviderId: String? = null,
        authState: Int? = null
    ): Deferred<AdobeVisitor?> =
        autoFetchVisitorDeferred
            ?: coroutineScope {
                async {
                    for (i in 1..maxRetries) {
                        Logger.dev(BuildConfig.TAG, "Attempt $i")
                        try {
                            visitorApi.suspendRequestNewAdobeVisitor(
                                1000,
                                customVisitorId,
                                dataProviderId,
                                authState
                            )?.let {
                                // Return early if valid visitor.
                                _visitor = it
                                return@async it
                            }
                        } catch (ce: CancellationException) {
                            Logger.dev(
                                BuildConfig.TAG,
                                "Request for ECID failed/was cancelled ($i)"
                            )
                            Logger.dev(BuildConfig.TAG, "Exception: (${ce.message})")
                        } catch (e: java.lang.Exception) {
                            Logger.dev(BuildConfig.TAG, "Failed to retrieve ECID ($i)")
                            Logger.dev(BuildConfig.TAG, "Exception: (${e.message})")
                        }
                    }
                    null
                }.also { autoFetchVisitorDeferred = it }
            }


    companion object : CollectorFactory {
        const val MODULE_NAME = "AdobeVisitorService"

        private fun getSharedPreferencesName(config: TealiumConfig): String {
            return "tealium.adobevisitor." + Integer.toHexString((config.accountName + config.profileName + config.environment.environment).hashCode())
        }

        override fun create(context: TealiumContext): Collector {
            return AdobeVisitorModule(context)
        }
    }

    private inner class AdobeVisitorListener(private val otherListener: ResponseListener<AdobeVisitor>?) :
        ResponseListener<AdobeVisitor> {
        override fun success(data: AdobeVisitor) {
            this@AdobeVisitorModule._visitor = data
            otherListener?.success(data)
        }

        override fun failure(errorCode: Int, ex: Exception?) {
            otherListener?.failure(errorCode, ex)
        }
    }

    private suspend fun AdobeExperienceCloudIdService.suspendRequestNewAdobeVisitor(
        timeout: Long,
        customVisitorId: String? = null,
        dataProviderId: String? = null,
        authState: Int? = null
    ): AdobeVisitor? =
        suspendCancellableCoroutine { cont ->
            val listener = AdobeVisitorListener(object : ResponseListener<AdobeVisitor> {
                override fun success(data: AdobeVisitor) {
                    // resume coroutine as we have valid AdobeVisitor
                    cont.resume(data)
                }

                override fun failure(errorCode: Int, ex: Exception?) {
                    // failure, so check coroutine is not already completed, and throw exception
                    if (!cont.isCompleted) {
                        cont.resumeWithException(
                            ex ?: Exception("Failed to retrieve visitor with errorCode: $errorCode")
                        )
                    }
                }
            })

            // use given custom id and data provider if available
            if (customVisitorId != null &&
                dataProviderId != null
            ) {
                visitorApi.requestNewEcidAndLink(
                    customVisitorId,
                    dataProviderId,
                    authState,
                    listener
                )
            } else {
                visitorApi.requestNewAdobeEcid(listener)
            }

            // withTimeout not working well in this instance.
            // delay for given timeout, and manually cancel to ensure continuation gets completed.
            runBlocking { delay(timeout) }
            if (!cont.isCompleted) cont.cancel(CancellationException("Timed Out."))
        }
}