package com.tealium.adobe.java

import android.content.SharedPreferences
import com.tealium.adobe.api.*
import com.tealium.internal.data.Dispatch
import com.tealium.library.DataSources
import com.tealium.library.Tealium
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL
import java.util.*

@RunWith(RobolectricTestRunner::class)
class AdobeVisitorModuleTests {

    @MockK
    lateinit var mockSharedPreferences: SharedPreferences

    @MockK
    lateinit var mockEditor: SharedPreferences.Editor

    @RelaxedMockK
    lateinit var mockAdobeService: AdobeExperienceCloudIdService

    @MockK
    lateinit var mockVisitor: AdobeVisitor

    @RelaxedMockK
    lateinit var mockAdobeListener: ResponseListener<AdobeVisitor>

    @RelaxedMockK
    lateinit var mockTealium: Tealium

    @RelaxedMockK
    lateinit var mockDataSources: DataSources

    @RelaxedMockK
    lateinit var mockPersistentData: SharedPreferences

    private val adobeOrgId = "orgId"

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.clear() } returns mockEditor
        every { mockEditor.apply() } just Runs

        // Default known AdobeVisitor.
        every { mockVisitor.experienceCloudId } returns "ecid"
        every { mockVisitor.idSyncTTL } returns 100
        every { mockVisitor.region } returns 1
        every { mockVisitor.blob } returns "blob"
        every { mockVisitor.nextRefresh } returns Date(Long.MAX_VALUE)

        mockkStatic(Tealium::class)
        every { Tealium.getInstance(any()) } returns mockTealium
        every { mockTealium.dataSources } returns mockDataSources
        every { mockDataSources.persistentDataSources } returns mockPersistentData
        every { mockPersistentData.edit() } returns mockEditor

        mockkObject(AdobeVisitor)
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns mockVisitor
        every { AdobeVisitor.toSharedPreferences(mockSharedPreferences, any()) } just Runs
    }

    @Test
    fun config_OrgIdGetsSet_FromConfig() {
        val adobeVisitorModule =
            AdobeVisitorModule(
                adobeOrgId,
                mockAdobeService,
                mockSharedPreferences,
                0,
                null,
                null,
                null,
                null,
                null
            )

        assertEquals("orgId", adobeVisitorModule.adobeOrgId)
    }

    @Test
    fun visitor_SetToNull_WhenNoSavedVisitor() {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null

        val adobeVisitorModule =
            AdobeVisitorModule(
                adobeOrgId,
                mockAdobeService,
                mockSharedPreferences,
                0,
                null,
                null,
                null,
                null,
                null
            )

        assertNull(adobeVisitorModule.visitor)
    }

    @Test
    fun visitor_SetToSavedVisitor() {
        val adobeVisitorModule =
            AdobeVisitorModule(
                adobeOrgId,
                mockAdobeService,
                mockSharedPreferences,
                0,
                null,
                null,
                null,
                null,
                null
            )

        assertSame(mockVisitor, adobeVisitorModule.visitor)
    }

    @Test
    fun visitor_Id_SetFromConfig() {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null

        val adobeVisitorModule =
            AdobeVisitorModule(
                adobeOrgId,
                mockAdobeService,
                mockSharedPreferences,
                0,
                "my_ecid",
                null,
                null,
                null,
                null
            )

        assertNotNull(adobeVisitorModule.visitor)
        assertEquals("my_ecid", adobeVisitorModule.visitor?.experienceCloudId)
    }

    @Test
    fun visitor_RequestedAndLinked_WhenKnownIdAndDataProviderAvailable_AndNoSavedVisitor() {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null

        val adobeVisitorModule =
            AdobeVisitorModule(
                adobeOrgId,
                mockAdobeService,
                mockSharedPreferences,
                5,
                null,
                "dp",
                null,
                "custom",
                null
            )

        assertNull(adobeVisitorModule.visitor)

        verify {
            mockAdobeService.requestNewEcidAndLink("custom", "dp", any(), any())
        }
    }

    @Test
    fun visitor_Linked_WhenKnownIdAndDataProviderAvailable_AndSavedVisitor() {
        val adobeVisitorModule =
            AdobeVisitorModule(
                adobeOrgId,
                mockAdobeService,
                mockSharedPreferences,
                5,
                null,
                "dp",
                null,
                "custom",
                null
            )

        assertNotNull(adobeVisitorModule.visitor)

        verify {
            mockAdobeService.linkEcidToKnownIdentifier("custom", "ecid", "dp", any(), any())
        }
    }

    @Test
    fun refresh_RefreshesUser_WhenRequired() {
        every { mockVisitor.nextRefresh } returns Date(Long.MIN_VALUE)
        AdobeVisitorModule(
            adobeOrgId,
            mockAdobeService,
            mockSharedPreferences,
            0,
            null,
            null,
            null,
            null,
            null
        )

        verify {
            mockAdobeService.refreshExistingAdobeEcid("ecid", any())
        }
    }

    @Test
    fun collect_ReturnsEmpty_WhenNoRetriesSet() {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null

        val adobeVisitorModule =
            AdobeVisitorModule(
                adobeOrgId,
                mockAdobeService,
                mockSharedPreferences,
                0,
                null,
                null,
                null,
                null,
                null
            )

        val dispatch = Dispatch()
        adobeVisitorModule.onPopulateDispatch(dispatch)
        assertFalse(dispatch.containsKey("adobe_ecid"))

        verify {
            mockAdobeService wasNot Called
        }
    }

    @Test
    fun collect_ReturnsInitialEcid_WhenProvided() {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null

        val adobeVisitorModule =
            AdobeVisitorModule(
                adobeOrgId,
                mockAdobeService,
                mockSharedPreferences,
                5,
                "my_ecid",
                null,
                null,
                null,
                null
            )

        val dispatch = Dispatch()
        adobeVisitorModule.onPopulateDispatch(dispatch)
        assertTrue(dispatch.containsKey("adobe_ecid"))
        assertEquals("my_ecid", dispatch.getString("adobe_ecid"))

        verify(exactly = 0) {
            // should not fetch new id
            mockAdobeService.requestNewAdobeEcid(any())
        }

        verify(exactly = 1) {
            // should not fetch new id
            mockAdobeService.refreshExistingAdobeEcid("my_ecid", any())
        }
    }

    @Test
    fun collect_RetriesMultipleTimes_WhenConfigured() {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null

        val adobeVisitorModule =
            AdobeVisitorModule(
                adobeOrgId,
                mockAdobeService,
                mockSharedPreferences,
                5,
                null,
                null,
                null,
                null,
                null
            )

        val dispatch = Dispatch()
        adobeVisitorModule.onPopulateDispatch(dispatch)
        assertFalse(dispatch.containsKey("adobe_ecid"))

        verify(exactly = 5) {
            mockAdobeService.requestNewAdobeEcid(any())
        }
    }

    @Test
    fun collect_RetriesMultipleTimes_AndAllDispatchesGetId() {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null

        val capturedListener = slot<ResponseListener<AdobeVisitor>>()
        every { mockAdobeService.requestNewAdobeEcid(capture(capturedListener)) } coAnswers {
            Thread.sleep(100)
            capturedListener.captured.failure(ErrorCode.ERROR_FAILED_REQUEST, null)
        } andThen {
            capturedListener.captured.success(mockVisitor)
        }

        val adobeVisitorModule =
            AdobeVisitorModule(
                adobeOrgId,
                mockAdobeService,
                mockSharedPreferences,
                5,
                null,
                null,
                null,
                null,
                null
            )

        val dispatch1 = Dispatch()
        val dispatch2 = Dispatch()
        adobeVisitorModule.onPopulateDispatch(dispatch1)
        adobeVisitorModule.onPopulateDispatch(dispatch2)
        assertEquals("ecid", dispatch1.getString("adobe_ecid"))
        assertEquals("ecid", dispatch2.getString("adobe_ecid"))

        verify(exactly = 2) {
            mockAdobeService.requestNewAdobeEcid(any())
        }
    }

    @Test
    fun linkExisting_CallsVisitorApi_AndUpdatesVisitor() {
        val adobeVisitorModule =
            AdobeVisitorModule(
                adobeOrgId,
                mockAdobeService,
                mockSharedPreferences,
                0,
                null,
                null,
                null,
                null,
                null
            )

        val newVisitor: AdobeVisitor = mockk()
        every { newVisitor.experienceCloudId } returns "new_ecid"
        val capturedListener = slot<ResponseListener<AdobeVisitor>>()
        every {
            mockAdobeService.linkEcidToKnownIdentifier(
                "knownId",
                "ecid",
                "1",
                AdobeAuthState.AUTH_STATE_UNKNOWN,
                capture(capturedListener)
            )
        } answers {
            capturedListener.captured.success(newVisitor)
        }

        adobeVisitorModule.linkEcidToKnownIdentifier(
            "knownId",
            "1",
            AdobeAuthState.AUTH_STATE_UNKNOWN,
            mockAdobeListener
        )

        assertSame(newVisitor, adobeVisitorModule.visitor)
        verify(timeout = 100) {
            mockAdobeService.linkEcidToKnownIdentifier(
                "knownId",
                "ecid",
                "1",
                AdobeAuthState.AUTH_STATE_UNKNOWN,
                capturedListener.captured
            )
            mockAdobeListener.success(newVisitor)
        }
    }

    @Test
    fun linkExisting_CallsVisitorApi_AndFails() {
        val adobeVisitorModule =
            AdobeVisitorModule(
                adobeOrgId,
                mockAdobeService,
                mockSharedPreferences,
                0,
                null,
                null,
                null,
                null,
                null
            )

        val capturedListener = slot<ResponseListener<AdobeVisitor>>()
        every {
            mockAdobeService.linkEcidToKnownIdentifier(
                "knownId",
                "ecid",
                "1",
                AdobeAuthState.AUTH_STATE_UNKNOWN,
                capture(capturedListener)
            )
        } answers {
            mockAdobeListener.failure(ErrorCode.ERROR_FAILED_REQUEST, null)
        }

        adobeVisitorModule.linkEcidToKnownIdentifier(
            "knownId",
            "1",
            AdobeAuthState.AUTH_STATE_UNKNOWN,
            mockAdobeListener
        )

        assertSame(mockVisitor, adobeVisitorModule.visitor)
        verify(timeout = 100) {
            mockAdobeService.linkEcidToKnownIdentifier(
                "knownId",
                "ecid",
                "1",
                AdobeAuthState.AUTH_STATE_UNKNOWN,
                capturedListener.captured
            )
            mockAdobeListener.failure(ErrorCode.ERROR_FAILED_REQUEST, null)
        }
    }

    @Test
    fun resetVisitor_ClearsStorage() {
        val adobeVisitorModule =
            AdobeVisitorModule(
                adobeOrgId,
                mockAdobeService,
                mockSharedPreferences,
                0,
                null,
                null,
                null,
                null,
                null
            )

        adobeVisitorModule.resetVisitor()

        assertNull(adobeVisitorModule.visitor)
        verify(timeout = 100) {
            mockSharedPreferences.edit()
            mockEditor.clear()
            mockEditor.apply()
        }
    }

    @Test
    fun appendVisitorQueryParams() {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns mockVisitor

        val adobeVisitorModule = AdobeVisitorModule(
            adobeOrgId,
            mockAdobeService,
            mockSharedPreferences,
            0,
            null,
            null,
            null,
            null,
            null
        )

        adobeVisitorModule.provideParameters { map ->
            if (map != null) {
                map["adobe_mc"]?.let {
                    assertTrue(it[0].contains("MCMID=ecid|MCORGID=orgId|TS="))
                }
            }
        }
    }


    @Test
    fun generateUrlWithVisitorQueryParams(): Unit = runBlocking {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns mockVisitor

        val adobeVisitorModule = AdobeVisitorModule(
            adobeOrgId,
            mockAdobeService,
            mockSharedPreferences,
            0,
            null,
            null,
            null,
            null,
            null
        )
        val mockHandler = mockk<UrlDecoratorHandler>(relaxed = true)
        adobeVisitorModule.decorateUrl(URL("https://tealium.com/"), mockHandler)

        verify(timeout = 100) {
            mockHandler.onDecorateUrl(match {
                val urlString = it.toString()
                urlString.contains(QP_ADOBE_MC, ignoreCase = false)
                        && urlString
                    .contains("/?adobe_mc=MCMID%3Decid%7CMCORGID%3DorgId%7CTS%3D", ignoreCase = false)

            })
        }
    }

    @Test
    fun getQueryParameters(): Unit = runBlocking {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns mockVisitor

        val adobeVisitorModule = AdobeVisitorModule(
            adobeOrgId,
            mockAdobeService,
            mockSharedPreferences,
            0,
            null,
            null,
            null,
            null,
            null
        )
        val mockHandler = mockk<GetUrlParametersHandler>(relaxed = true)
        adobeVisitorModule.getUrlParameters(mockHandler)

        verify(timeout = 100) {
            mockHandler.onRetrieveParameters(match {
                val params = it.entries.iterator().next()
                params.key == QP_ADOBE_MC && params.value.contains("MCMID=ecid|MCORGID=orgId|TS=")
            })
        }
    }

}