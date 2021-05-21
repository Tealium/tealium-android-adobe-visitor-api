package com.tealium.adobe.kotlin

import android.app.Application
import com.tealium.adobe.api.AdobeAuthState
import com.tealium.core.Collectors
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class TealiumConfigAdobeVisitorTests {

    @MockK
    lateinit var mockApplication: Application

    @MockK
    lateinit var mockFile: File

    lateinit var config: TealiumConfig

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { mockApplication.filesDir } returns mockFile
        config = TealiumConfig(mockApplication, "test", "test", Environment.DEV)
    }

    @Test
    fun config_Getters_ReturnsValue() {
        config.options[ADOBE_VISITOR_ORG_ID] = "orgId"
        assertEquals("orgId", config.adobeVisitorOrgId)

        config.options[ADOBE_VISITOR_EXISTING_ECID] = "ecid"
        assertEquals("ecid", config.adobeVisitorExistingEcid)

        config.options[ADOBE_VISITOR_RETRIES] = 10
        assertEquals(10, config.adobeVisitorRetries)

        config.options[ADOBE_VISITOR_DATA_PROVIDER_ID] = "01"
        assertEquals("01", config.adobeVisitorDataProviderId)

        config.options[ADOBE_VISITOR_AUTH_STATE] = AdobeAuthState.AUTH_STATE_AUTHENTICATED
        assertEquals(AdobeAuthState.AUTH_STATE_AUTHENTICATED, config.adobeVisitorAuthState)

        config.options[ADOBE_VISITOR_CUSTOM_VISITOR_ID] = "custom"
        assertEquals("custom", config.adobeVisitorCustomVisitorId)
    }

    @Test
    fun config_Setters_SetsValue() {
        config.adobeVisitorOrgId = "orgId"
        assertEquals("orgId", config.adobeVisitorOrgId)

        config.adobeVisitorExistingEcid = "ecid"
        assertEquals("ecid", config.adobeVisitorExistingEcid)

        config.adobeVisitorRetries = 10
        assertEquals(10, config.adobeVisitorRetries)

        config.adobeVisitorDataProviderId = "01"
        assertEquals("01", config.adobeVisitorDataProviderId)

        config.adobeVisitorAuthState = AdobeAuthState.AUTH_STATE_AUTHENTICATED
        assertEquals(AdobeAuthState.AUTH_STATE_AUTHENTICATED, config.adobeVisitorAuthState)

        config.adobeVisitorCustomVisitorId = "custom"
        assertEquals("custom", config.adobeVisitorCustomVisitorId)
    }

    @Test
    fun config_SetNulls_RemovesValues() {
        config.adobeVisitorOrgId = "orgId"
        assertEquals("orgId", config.adobeVisitorOrgId)
        config.adobeVisitorOrgId = null
        assertNull(config.adobeVisitorOrgId)
        assertFalse(config.options.containsKey(ADOBE_VISITOR_ORG_ID))

        config.adobeVisitorExistingEcid = "ecid"
        assertEquals("ecid", config.adobeVisitorExistingEcid)
        config.adobeVisitorExistingEcid = null
        assertNull(config.adobeVisitorExistingEcid)
        assertFalse(config.options.containsKey(ADOBE_VISITOR_EXISTING_ECID))

        config.adobeVisitorRetries = 10
        assertEquals(10, config.adobeVisitorRetries)
        config.adobeVisitorRetries = null
        assertNull(config.adobeVisitorRetries)
        assertFalse(config.options.containsKey(ADOBE_VISITOR_RETRIES))

        config.adobeVisitorDataProviderId = "10"
        assertEquals("10", config.adobeVisitorDataProviderId)
        config.adobeVisitorDataProviderId = null
        assertNull(config.adobeVisitorDataProviderId)
        assertFalse(config.options.containsKey(ADOBE_VISITOR_DATA_PROVIDER_ID))

        config.adobeVisitorAuthState = AdobeAuthState.AUTH_STATE_AUTHENTICATED
        assertEquals(AdobeAuthState.AUTH_STATE_AUTHENTICATED, config.adobeVisitorAuthState)
        config.adobeVisitorAuthState = null
        assertNull(config.adobeVisitorAuthState)
        assertFalse(config.options.containsKey(ADOBE_VISITOR_AUTH_STATE))

        config.adobeVisitorCustomVisitorId = "custom"
        assertEquals("custom", config.adobeVisitorCustomVisitorId)
        config.adobeVisitorCustomVisitorId = null
        assertNull(config.adobeVisitorCustomVisitorId)
        assertFalse(config.options.containsKey(ADOBE_VISITOR_CUSTOM_VISITOR_ID))
    }

    @Test
    fun collectors_PointsToModuleFactory() {
        assertSame(AdobeVisitorModule.Companion, Collectors.AdobeVisitor)
    }
}