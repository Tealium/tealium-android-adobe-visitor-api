package com.tealium.adobe.java

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AdobeVisitorModuleInstrumentedTests {

    val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun static_TestInitialization() {
        assertNull(AdobeVisitorModule.getInstance())
        val instance = AdobeVisitorModule.setUp(context, "adobeOrgId")
        assertNotNull(AdobeVisitorModule.getInstance())
    }
}