package com.tealium.adobe.api.network

import android.app.Service
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import android.os.Build
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
import java.util.concurrent.ExecutorService

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.KITKAT])
class HttpClientTestsKitKat {

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
        every { mockConnectivityManager.activeNetworkInfo } returns mockNetworkInfo
    }

    @Test
    fun isConnected_True_WhenActiveNetwork_ConnectedWifiOrMobile() {
        every { mockNetworkInfo.isConnected } returns true
        every { mockNetworkInfo.type } returns ConnectivityManager.TYPE_WIFI

        val httpClient = HttpClient(mockContext)
        assertTrue(httpClient.isConnected())
    }

    @Test
    fun isConnected_False_WhenActiveNetwork_NotConnected() {
        every { mockNetworkInfo.type } returns ConnectivityManager.TYPE_WIFI
        every { mockNetworkInfo.isConnected } returns false

        val httpClient = HttpClient(mockContext)
        assertFalse(httpClient.isConnected())
    }

    @Test
    fun isConnected_False_WhenActiveNetwork_NotWifiOrMobile() {
        every { mockNetworkInfo.isConnected } returns true
        every { mockNetworkInfo.type } returns ConnectivityManager.TYPE_BLUETOOTH

        val httpClient = HttpClient(mockContext)
        assertFalse(httpClient.isConnected())
    }
}