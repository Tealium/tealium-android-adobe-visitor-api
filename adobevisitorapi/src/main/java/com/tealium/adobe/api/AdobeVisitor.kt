package com.tealium.adobe.api

import android.content.SharedPreferences
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Model class representing an Adobe Visitor.
 */
data class AdobeVisitor @JvmOverloads constructor(
    val experienceCloudId: String,
    val idSyncTTL: Int,
    val region: Int,
    val blob: String,
    val nextRefresh: Date = getNextRefresh(
        idSyncTTL
    )
) {

    companion object {
        private const val INVALID_INT = -1
        private const val KEY_NEXT_REFRESH = "next_refresh"
        private const val FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        private val ISO_8601_DATE_FORMAT = SimpleDateFormat(FORMAT_ISO_8601, Locale.ROOT)

        @JvmStatic
        private fun getNextRefresh(ttl: Int): Date {
            return Calendar.getInstance().let { cal ->
                cal.add(Calendar.SECOND, ttl)
                cal.time
            }
        }

        /**
         * Constructs an AdobeVisitor from a JSON representation - typically the response data from
         * the ECID service. As [nextRefresh] is a calculated property, there is no support for
         * supplying on the json object.
         *
         * Keys are expected to match the query parameters used to send to Adobe.
         */
        @JvmStatic
        fun fromJson(json: JSONObject): AdobeVisitor? {
            return try {
                val experienceCloudId = json.getString(QP_EXPERIENCE_CLOUD_ID)
                val idSyncTTL = json.getInt(QP_ID_SYNC_TTL)
                val region = json.getInt(QP_REGION)
                val blob = json.getString(QP_ENCRYPTED_META_DATA)

                AdobeVisitor(
                    experienceCloudId,
                    idSyncTTL,
                    region,
                    blob
                )
            } catch (ex: java.lang.Exception) {
                null
            }
        }

        /**
         * Constructs an [AdobeVisitor] instance from [sharedPreferences]. Keys will match the query
         * parameters used to send data to the ECID service.
         *
         * See [toSharedPreferences] as a counterpart to this method.
         */
        @JvmStatic
        fun fromSharedPreferences(sharedPreferences: SharedPreferences): AdobeVisitor? {
            return try {
                val experienceCloudId = sharedPreferences.getString(QP_EXPERIENCE_CLOUD_ID, null)
                val idSyncTTL = sharedPreferences.getInt(
                    QP_ID_SYNC_TTL,
                    INVALID_INT
                )
                val region = sharedPreferences.getInt(
                    QP_REGION,
                    INVALID_INT
                )
                val blob = sharedPreferences.getString(QP_ENCRYPTED_META_DATA, null)
                val nextRefresh = sharedPreferences.getString(KEY_NEXT_REFRESH, null)?.let {
                    ISO_8601_DATE_FORMAT.parse(it)
                }

                if (!experienceCloudId.isNullOrEmpty() && !blob.isNullOrEmpty() && idSyncTTL != INVALID_INT && region != INVALID_INT && nextRefresh != null) {
                    AdobeVisitor(
                        experienceCloudId,
                        idSyncTTL,
                        region,
                        blob,
                        nextRefresh
                    )
                } else null

            } catch (ex: java.lang.Exception) {
                null
            }
        }

        /**
         * Writes an [AdobeVisitor] instance to [sharedPreferences]. Keys will match the query
         * parameters used to send data to the ECID service.
         *
         * See [fromSharedPreferences] as a counterpart to this method.
         */
        @JvmStatic
        fun toSharedPreferences(sharedPreferences: SharedPreferences, visitor: AdobeVisitor) {
            sharedPreferences.edit()
                .putString(QP_EXPERIENCE_CLOUD_ID, visitor.experienceCloudId)
                .putInt(QP_REGION, visitor.region)
                .putInt(QP_ID_SYNC_TTL, visitor.idSyncTTL)
                .putString(KEY_NEXT_REFRESH, ISO_8601_DATE_FORMAT.format(visitor.nextRefresh))
                .putString(QP_ENCRYPTED_META_DATA, visitor.blob)
                .apply()
        }
    }
}