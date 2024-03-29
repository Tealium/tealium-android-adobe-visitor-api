package com.tealium.adobe.kotlin

import android.content.SharedPreferences
import android.net.Uri
import com.tealium.adobe.api.*
import com.tealium.core.Logger
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import com.tealium.core.messaging.MessengerService
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    lateinit var mockTealiumContext: TealiumContext

    @MockK
    lateinit var mockTealiumContextNullVisitor: TealiumContext

    @MockK
    lateinit var mockConfig: TealiumConfig

    @MockK
    lateinit var mockConfigNullVisitor: TealiumConfig

    @MockK
    lateinit var mockVisitor: AdobeVisitor

    @MockK
    var mockNullVisitor: AdobeVisitor? = null

    @RelaxedMockK
    lateinit var mockAdobeListener: ResponseListener<AdobeVisitor>

    @RelaxedMockK
    lateinit var mockMessengerService: MessengerService

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { mockTealiumContext.config } returns mockConfig
        every { mockTealiumContextNullVisitor.config } returns mockConfigNullVisitor
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.clear() } returns mockEditor
        every { mockEditor.apply() } just Runs

        // Default orgId set correctly.
        every { mockConfig.adobeVisitorOrgId } returns "orgId"
        every { mockConfig.adobeVisitorRetries } returns null
        every { mockConfig.adobeVisitorExistingEcid } returns null
        every { mockConfig.adobeVisitorDataProviderId } returns null
        every { mockConfig.adobeVisitorAuthState } returns null
        every { mockConfig.adobeVisitorCustomVisitorId } returns null
        every { mockConfig.adobeVisitorExecutor } returns null

        // Default orgId set correctly.
        every { mockConfigNullVisitor.adobeVisitorOrgId } returns "orgId"
        every { mockConfigNullVisitor.adobeVisitorRetries } returns 0
        every { mockConfigNullVisitor.adobeVisitorExistingEcid } returns null
        every { mockConfigNullVisitor.adobeVisitorDataProviderId } returns null
        every { mockConfigNullVisitor.adobeVisitorAuthState } returns null
        every { mockConfigNullVisitor.adobeVisitorCustomVisitorId } returns null
        every { mockConfigNullVisitor.adobeVisitorExecutor } returns null

        // Default known AdobeVisitor.
        every { mockVisitor.experienceCloudId } returns "ecid"
        every { mockVisitor.idSyncTTL } returns 100
        every { mockVisitor.region } returns 1
        every { mockVisitor.blob } returns "blob"
        every { mockVisitor.nextRefresh } returns Date(Long.MAX_VALUE)

        every { mockNullVisitor?.experienceCloudId } returns "ecid"
        every { mockNullVisitor?.idSyncTTL } returns 100
        every { mockNullVisitor?.region } returns 1
        every { mockNullVisitor?.blob } returns "blob"
        every { mockNullVisitor?.nextRefresh } returns Date(Long.MAX_VALUE)

        every { mockTealiumContext.events } returns mockMessengerService

        mockkObject(Logger)
        every {
            Logger.dev(
                any(),
                any()
            )
        } just Runs
        every {
            Logger.qa(
                any(),
                any()
            )
        } just Runs
        mockkObject(AdobeVisitor)
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns mockVisitor
        every {
            AdobeVisitor.toSharedPreferences(
                mockSharedPreferences,
                any()
            )
        } just Runs
    }

    @Test
    fun config_OrgIdGetsSet_FromConfig() {
        val adobeVisitorModule =
            AdobeVisitorModule(
                mockTealiumContext,
                visitorApi = mockAdobeService,
                sharedPreferences = mockSharedPreferences
            )

        assertEquals("orgId", adobeVisitorModule.adobeOrgId)
    }

    @Test
    fun config_MethodsDontFunction_WhenMissingOrgId() {
        every { mockConfig.adobeVisitorOrgId } returns null
        val adobeVisitorModule =
            AdobeVisitorModule(
                mockTealiumContext,
                visitorApi = mockAdobeService,
                sharedPreferences = mockSharedPreferences
            )

        assertNull(adobeVisitorModule.adobeOrgId)
        adobeVisitorModule.linkEcidToKnownIdentifier(
            "knownId",
            "1",
            AdobeAuthState.AUTH_STATE_AUTHENTICATED,
            null
        )

        verify {
            mockAdobeService wasNot Called
        }
    }

    @Test
    fun visitor_SetToNull_WhenNoSavedVisitor() {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null

        val adobeVisitorModule =
            AdobeVisitorModule(
                mockTealiumContext,
                visitorApi = mockAdobeService,
                sharedPreferences = mockSharedPreferences
            )

        assertNull(adobeVisitorModule.visitor)
    }

    @Test
    fun visitor_SetToSavedVisitor() {
        val adobeVisitorModule =
            AdobeVisitorModule(
                mockTealiumContext,
                visitorApi = mockAdobeService,
                sharedPreferences = mockSharedPreferences
            )

        assertSame(mockVisitor, adobeVisitorModule.visitor)
    }

    @Test
    fun visitor_Id_SetFromConfig() {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null
        every { mockConfig.adobeVisitorExistingEcid } returns "my_ecid"

        val adobeVisitorModule =
            AdobeVisitorModule(
                mockTealiumContext,
                visitorApi = mockAdobeService,
                sharedPreferences = mockSharedPreferences
            )

        assertNotNull(adobeVisitorModule.visitor)
        assertEquals("my_ecid", adobeVisitorModule.visitor?.experienceCloudId)
    }

    @Test
    fun visitor_ExistingIsLinked_WhenDataProviderAndKnownIdProvided() {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns mockVisitor
        every { mockConfig.adobeVisitorDataProviderId } returns "dp"
        every { mockConfig.adobeVisitorCustomVisitorId } returns "custom"

        val adobeVisitorModule =
            AdobeVisitorModule(
                mockTealiumContext,
                visitorApi = mockAdobeService,
                sharedPreferences = mockSharedPreferences
            )

        assertNotNull(adobeVisitorModule.visitor)
        verify {
            mockAdobeService.linkEcidToKnownIdentifier("custom", "ecid", "dp", any(), any())
        }
    }

    @Test
    fun visitor_IsRequestedAndLinked_WhenDataProviderAndKnownIdProvided() {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null
        every { mockConfig.adobeVisitorDataProviderId } returns "dp"
        every { mockConfig.adobeVisitorCustomVisitorId } returns "custom"

        val adobeVisitorModule =
            AdobeVisitorModule(
                mockTealiumContext,
                visitorApi = mockAdobeService,
                sharedPreferences = mockSharedPreferences
            )

        verify {
            mockAdobeService.requestNewEcidAndLink("custom", "dp", any(), any())
        }
    }

    @Test
    fun refresh_RefreshesUser_WhenRequired() {
        every { mockVisitor.nextRefresh } returns Date(Long.MIN_VALUE)
        AdobeVisitorModule(
            mockTealiumContext,
            visitorApi = mockAdobeService,
            sharedPreferences = mockSharedPreferences
        )

        verify {
            mockAdobeService.refreshExistingAdobeEcid("ecid", any())
        }
    }

    @Test
    fun collect_ReturnsEmpty_WhenNoVisitorInformation() =
        runBlocking {
            every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null

            val adobeVisitorModule =
                AdobeVisitorModule(
                    mockTealiumContext,
                    visitorApi = mockAdobeService,
                    sharedPreferences = mockSharedPreferences
                )

            assertEquals(0, adobeVisitorModule.collect().size)
        }

    @Test
    fun collect_ReturnsEcid_WhenValidVisitorInformation() =
        runBlocking {
            every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns mockVisitor

            val adobeVisitorModule =
                AdobeVisitorModule(
                    mockTealiumContext,
                    visitorApi = mockAdobeService,
                    sharedPreferences = mockSharedPreferences
                )

            val data = adobeVisitorModule.collect()
            assertEquals(1, data.size)
            assertEquals("ecid", data["adobe_ecid"])
        }

    @Test
    fun collect_ReturnsEmpty_WhenNoRetriesSet() =
        runBlocking {
            every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null
            every { mockConfig.adobeVisitorRetries } returns 0

            val adobeVisitorModule =
                AdobeVisitorModule(
                    mockTealiumContext,
                    visitorApi = mockAdobeService,
                    sharedPreferences = mockSharedPreferences
                )

            assertEquals(0, adobeVisitorModule.collect().size)

            verify {
                mockAdobeService wasNot Called
            }
        }

    @Test
    fun collect_ReturnsInitialEcid_WhenProvided() =
        runBlocking {
            every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null
            every { mockConfig.adobeVisitorExistingEcid } returns "my_ecid"
            every { mockConfig.adobeVisitorRetries } returns 5

            val adobeVisitorModule =
                AdobeVisitorModule(
                    mockTealiumContext,
                    visitorApi = mockAdobeService,
                    sharedPreferences = mockSharedPreferences
                )

            assertEquals(1, adobeVisitorModule.collect().size)
            assertEquals("my_ecid", adobeVisitorModule.collect()["adobe_ecid"])

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
    fun collect_RetriesMultipleTimes_WhenConfigured() =
        runBlocking {
            every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null
            every { mockConfig.adobeVisitorRetries } returns 5

            val adobeVisitorModule =
                AdobeVisitorModule(
                    mockTealiumContext,
                    visitorApi = mockAdobeService,
                    sharedPreferences = mockSharedPreferences
                )

            assertEquals(0, adobeVisitorModule.collect().size)

            verify(exactly = 5) {
                mockAdobeService.requestNewAdobeEcid(any())
            }
        }

    @Test
    fun collect_RetriesMultipleTimes_AndAllDispatchesGetId() =
        runBlocking {
            every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null
            every { mockConfig.adobeVisitorRetries } returns 5

            val capturedListener = slot<ResponseListener<AdobeVisitor>>()
            every { mockAdobeService.requestNewAdobeEcid(capture(capturedListener)) } coAnswers {
                delay(100)
                capturedListener.captured.failure(ErrorCode.ERROR_FAILED_REQUEST, null)
            } andThen {
                capturedListener.captured.success(mockVisitor)
            }

            val adobeVisitorModule =
                AdobeVisitorModule(
                    mockTealiumContext,
                    visitorApi = mockAdobeService,
                    sharedPreferences = mockSharedPreferences
                )

            val data1 = adobeVisitorModule.collect()
            val data2 = adobeVisitorModule.collect()
            assertEquals("ecid", data1["adobe_ecid"])
            assertEquals("ecid", data2["adobe_ecid"])

            verify(exactly = 2) {
                mockAdobeService.requestNewAdobeEcid(any())
            }
        }

    @Test
    fun linkExisting_CallsVisitorApi_AndUpdatesVisitor() {
        val adobeVisitorModule =
            AdobeVisitorModule(
                mockTealiumContext,
                visitorApi = mockAdobeService,
                sharedPreferences = mockSharedPreferences
            )

        val newVisitor: AdobeVisitor = mockk()
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
        verify {
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
                mockTealiumContext,
                visitorApi = mockAdobeService,
                sharedPreferences = mockSharedPreferences
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
        verify {
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
                mockTealiumContext,
                visitorApi = mockAdobeService,
                sharedPreferences = mockSharedPreferences
            )

        adobeVisitorModule.resetVisitor()

        assertNull(adobeVisitorModule.visitor)
        verify {
            mockSharedPreferences.edit()
            mockEditor.clear()
            mockEditor.apply()
        }
    }

    @Test
    fun appendVisitorQueryParams(): Unit = runBlocking {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns mockVisitor

        val adobeVisitorModule = AdobeVisitorModule(
            mockTealiumContext,
            visitorApi = mockAdobeService,
            sharedPreferences = mockSharedPreferences
        )

        val encodedParams = adobeVisitorModule.provideParameters()

        assertTrue(encodedParams.containsKey(QP_ADOBE_MC))
        encodedParams[QP_ADOBE_MC]?.let {
            assertTrue(
                it[0].contains("MCMID=ecid|MCORGID=orgId|TS=")
            )
        }
    }

    @Test
    fun generateUrlWithVisitorQueryParams(): Unit = runBlocking {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns mockVisitor

        val adobeVisitorModule = AdobeVisitorModule(
            mockTealiumContext,
            visitorApi = mockAdobeService,
            sharedPreferences = mockSharedPreferences
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
    fun generateUrlWithVisitorQueryParamsWithNullAdobeVisitor(): Unit = runBlocking {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null

        val adobeVisitorModule = AdobeVisitorModule(
            mockTealiumContext,
            visitorApi = mockAdobeService,
            sharedPreferences = mockSharedPreferences
        )
        val mockHandler = mockk<UrlDecoratorHandler>(relaxed = true)
        adobeVisitorModule.decorateUrl(URL("https://tealium.com/"), mockHandler)

        verify(timeout = 100) {
            mockHandler.onDecorateUrl(match {
                val urlString = it.toString()
                urlString.equals("https://tealium.com/", false)
            })
        }
    }

    @Test
    fun getQueryParameters(): Unit = runBlocking {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns mockVisitor

        val adobeVisitorModule = AdobeVisitorModule(
            mockTealiumContext,
            visitorApi = mockAdobeService,
            sharedPreferences = mockSharedPreferences
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

    @Test
    fun getQueryParametersWithNullAdobeVisitor(): Unit = runBlocking {
        every { AdobeVisitor.fromSharedPreferences(mockSharedPreferences) } returns null

        val adobeVisitorModule = AdobeVisitorModule(
            mockTealiumContextNullVisitor,
            visitorApi = mockAdobeService,
            sharedPreferences = mockSharedPreferences
        )
        val mockHandler = mockk<GetUrlParametersHandler>(relaxed = true)
        adobeVisitorModule.getUrlParameters(mockHandler)

        verify(timeout = 100) {
            mockHandler.onRetrieveParameters(matchNullable { it.isNullOrEmpty() })
        }
    }
}