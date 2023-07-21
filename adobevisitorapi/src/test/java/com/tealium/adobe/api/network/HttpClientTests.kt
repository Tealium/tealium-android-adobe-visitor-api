package com.tealium.adobe.api.network

import android.app.Service
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import android.os.Build
import com.tealium.adobe.api.ErrorCode
import com.tealium.adobe.api.ResponseListener
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.LOLLIPOP, Build.VERSION_CODES.P])
class HttpClientTests {

    @MockK
    lateinit var mockContext: Context

    @MockK
    lateinit var mockConnectivityManager: ConnectivityManager

    @MockK
    lateinit var mockNetwork: Network

    @MockK
    lateinit var mockNetworkInfo: NetworkInfo

    @RelaxedMockK
    lateinit var mockExecutor: ExecutorService

    @RelaxedMockK
    lateinit var mockResponseListener: ResponseListener<String>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { mockContext.applicationContext } returns mockContext
        every { mockContext.getSystemService(Service.CONNECTIVITY_SERVICE) } returns mockConnectivityManager

        // default: connected
        every { mockConnectivityManager.allNetworks } returns arrayOf(mockNetwork)
        every { mockConnectivityManager.getNetworkInfo(mockNetwork) } returns mockNetworkInfo
        every { mockNetworkInfo.isConnected } returns true
    }

    @Test
    fun isConnected_True_WhenOneNetwork_AndConnected() {
        val httpClient = HttpClient(mockContext, mockExecutor)
        assertTrue(httpClient.isConnected())
    }

    @Test
    fun isConnected_True_WhenMultipleNetworks_AndOneConnected() {
        every { mockConnectivityManager.allNetworks } returns arrayOf(mockNetwork, mockNetwork, mockNetwork)
        every { mockConnectivityManager.getNetworkInfo(mockNetwork) } returns mockNetworkInfo
        every { mockNetworkInfo.isConnected } returnsMany listOf(false, true, false)

        val httpClient = HttpClient(mockContext, mockExecutor)
        assertTrue(httpClient.isConnected())
    }

    @Test
    fun isConnected_False_WhenMultipleNetworks_AndNoneConnected() {
        every { mockConnectivityManager.allNetworks } returns arrayOf(mockNetwork, mockNetwork, mockNetwork)
        every { mockConnectivityManager.getNetworkInfo(mockNetwork) } returns mockNetworkInfo
        every { mockNetworkInfo.isConnected } returnsMany listOf(false, false, false)

        val httpClient = HttpClient(mockContext, mockExecutor)
        assertFalse(httpClient.isConnected())
    }

    @Test
    fun get_ReturnsFailure_WhenNotConnected() {
        every { mockNetworkInfo.isConnected } returns false
        val httpClient = HttpClient(mockContext, mockExecutor)

        httpClient.get(mockk(), mockResponseListener)

        verify {
            mockResponseListener.failure(ErrorCode.ERROR_NOT_CONNECTED)
            mockExecutor wasNot Called
        }
    }

    @Test
    fun get_SubmitsJob_WhenConnected() {
        val httpClient = HttpClient(mockContext, mockExecutor)
        httpClient.get(URL("https://"), mockResponseListener)

        verify {
            mockExecutor.execute(any<Runnable>())
        }
    }
}