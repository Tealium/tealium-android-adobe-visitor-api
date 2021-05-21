package com.tealium.adobe.api

import android.content.SharedPreferences
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat

@RunWith(RobolectricTestRunner::class)
class AdobeVisitorTests {

    @MockK
    lateinit var mockSharedPreferences: SharedPreferences

    @MockK
    lateinit var mockEditor: SharedPreferences.Editor

    private val testDateString = "2025-01-01T00:00:00Z"

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun fromJson_ValidJson_ReturnsValidObject() {
        val valid = JSONObject(
            """
            {
                "d_mid": "ecid",
                "dcs_region": 6,
                "id_sync_ttl": 100,
                "d_blob": "blob"
            }
        """.trimIndent()
        )

        val visitor = AdobeVisitor.fromJson(valid)!!
        assertEquals("ecid", visitor.experienceCloudId)
        assertEquals(6, visitor.region)
        assertEquals("blob", visitor.blob)
        assertEquals(100, visitor.idSyncTTL)
    }

    @Test
    fun fromJson_MissingEcid_ReturnsNull() {
        val valid = JSONObject(
            //missing ecid
            """
            {
                "dcs_region": 6,
                "id_sync_ttl": 100,
                "d_blob": "blob"
            }
        """.trimIndent()
        )

        assertNull(AdobeVisitor.fromJson(valid))
    }

    @Test
    fun fromJson_MissingRegion_ReturnsNull() {
        val valid = JSONObject(
            //missing region
            """
            {
                "d_mid": "ecid",
                "id_sync_ttl": 100,
                "d_blob": "blob"
            }
        """.trimIndent()
        )

        assertNull(AdobeVisitor.fromJson(valid))
    }

    @Test
    fun fromJson_MissingTTL_ReturnsNull() {
        val valid = JSONObject(
            //missing TTL
            """
            {
                "d_mid": "ecid",
                "dcs_region": 6,
                "d_blob": "blob"
            }
        """.trimIndent()
        )

        assertNull(AdobeVisitor.fromJson(valid))
    }

    @Test
    fun fromSharedPreferences_ValidEntry_ReturnsValidObject() {
        every { mockSharedPreferences.getString(QP_EXPERIENCE_CLOUD_ID, any()) } returns "ecid"
        every { mockSharedPreferences.getInt(QP_REGION, any()) } returns 6
        every { mockSharedPreferences.getInt(QP_ID_SYNC_TTL, any()) } returns 100
        every { mockSharedPreferences.getString(QP_ENCRYPTED_META_DATA, any()) } returns "blob"
        every { mockSharedPreferences.getString("next_refresh", any()) } returns testDateString // yyyy-MM-dd'T'HH:mm:ss'Z

        val visitor = AdobeVisitor.fromSharedPreferences(mockSharedPreferences)!!
        assertEquals("ecid", visitor.experienceCloudId)
        assertEquals(6, visitor.region)
        assertEquals("blob", visitor.blob)
        assertEquals(100, visitor.idSyncTTL)
        assertEquals(testDateString, SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(visitor.nextRefresh))
    }

    @Test
    fun fromSharedPreferences_MissingEcid_ReturnsNull() {
        every { mockSharedPreferences.getString(QP_EXPERIENCE_CLOUD_ID, any()) } returns null
        every { mockSharedPreferences.getInt(QP_REGION, any()) } returns 6
        every { mockSharedPreferences.getInt(QP_ID_SYNC_TTL, any()) } returns 100
        every { mockSharedPreferences.getString(QP_ENCRYPTED_META_DATA, any()) } returns "blob"
        every { mockSharedPreferences.getString("next_refresh", any()) } returns testDateString // yyyy-MM-dd'T'HH:mm:ss'Z

       assertNull(AdobeVisitor.fromSharedPreferences(mockSharedPreferences))
    }

    @Test
    fun toSharedPreferences() {
        val adobeVisitor = AdobeVisitor(
            "ecid",
            100,
            6,
            "blob",
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(testDateString)!!
        )
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs

        AdobeVisitor.toSharedPreferences(mockSharedPreferences, adobeVisitor)
        verify {
            mockSharedPreferences.edit()
            mockEditor.putString(QP_EXPERIENCE_CLOUD_ID, "ecid")
            mockEditor.putInt(QP_ID_SYNC_TTL, 100)
            mockEditor.putInt(QP_REGION, 6)
            mockEditor.putString(QP_ENCRYPTED_META_DATA, "blob")
            mockEditor.putString("next_refresh", testDateString)
            mockEditor.apply()
        }
    }
}