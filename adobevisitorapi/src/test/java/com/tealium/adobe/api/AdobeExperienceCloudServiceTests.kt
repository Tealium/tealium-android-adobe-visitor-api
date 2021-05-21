package com.tealium.adobe.api

import android.content.Context
import com.tealium.adobe.api.network.NetworkClient
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdobeExperienceCloudServiceTests {

    @MockK
    lateinit var mockContext: Context

    @RelaxedMockK
    lateinit var mockNetworkClient: NetworkClient

    private lateinit var visitorApi: AdobeVisitorAPI

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        visitorApi = AdobeVisitorAPI(
            mockContext,
            "orgId", mockNetworkClient
        )
    }

    @Test
    fun ecid_RequestsNewId() {
        visitorApi.requestNewAdobeEcid(null)

        verify {
            mockNetworkClient.get(match {
                it.toString() == "https://dpm.demdex.net/id?d_orgid=orgId&d_ver=2"
            }, any())
        }
    }

    @Test
    fun ecid_RefreshId() {
        visitorApi.refreshExistingAdobeEcid("ecid", null)

        verify {
            mockNetworkClient.get(match {
                it.toString() == "https://dpm.demdex.net/id?d_orgid=orgId&d_ver=2&d_mid=ecid"
            }, any())
        }
    }

    @Test
    fun ecid_LinksToExistingECID() {
        visitorApi.linkEcidToKnownIdentifier("knownId", "ecid", "1", null, null)

        verify {
            mockNetworkClient.get(match {
                it.toString() == "https://dpm.demdex.net/id?d_orgid=orgId&d_ver=2&d_mid=ecid&d_cid=1%01knownId%010"
            }, any())
        }
    }

    @Test
    fun ecid_LinksToExistingECID_AuthenticationStateCorrect() {
        visitorApi.linkEcidToKnownIdentifier(
            "knownId",
            "ecid",
            "1",
            AdobeAuthState.AUTH_STATE_AUTHENTICATED,
            null
        )

        verify {
            mockNetworkClient.get(match {
                it.toString() == "https://dpm.demdex.net/id?d_orgid=orgId&d_ver=2&d_mid=ecid&d_cid=1%01knownId%011"
            }, any())
        }
    }

    @Test
    fun ecid_RequestsNewAndLinks() {
        val visitor = JSONObject(
            """
            {
                "d_mid": "ecid",
                "dcs_region": 6,
                "id_sync_ttl": 100,
                "d_blob": "blob"
            }
        """.trimIndent()
        ).toString()
        val listener = slot<ResponseListener<String>>()
        every { mockNetworkClient.get(any(), capture(listener)) } answers {
            listener.captured.success(visitor)
        }
        visitorApi.requestNewEcidAndLink("knownId", "1", null, null)

        verify {
            mockNetworkClient.get(match {
                it.toString() == "https://dpm.demdex.net/id?d_orgid=orgId&d_ver=2"
            }, any())
            mockNetworkClient.get(match {
                it.toString() == "https://dpm.demdex.net/id?d_orgid=orgId&d_ver=2&d_mid=ecid&d_cid=1%01knownId%010"
            }, any())
        }
    }
}