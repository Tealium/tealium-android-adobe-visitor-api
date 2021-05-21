package com.tealium.adobe

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.tealium.adobe.wrappers.JavaWrapper
import com.tealium.adobe.wrappers.KotlinWrapper
import com.tealium.adobe.wrappers.TealiumWrapper
import java.util.*

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var apiSpinner: Spinner
    private lateinit var apiLayout: LinearLayout

    private lateinit var trackEventButton: Button
    private lateinit var clearVisitorButton: Button
    private lateinit var refreshVisitorButton: Button

    private lateinit var fragment: AdobeVisitorFragment
    private val wrappers = mutableMapOf<String, TealiumWrapper>()
    private var itemSelected: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        apiSpinner = findViewById(R.id.spinner_api_options)
        ArrayAdapter.createFromResource(
            this,
            R.array.api_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_item)
            apiSpinner.adapter = adapter
        }
        apiSpinner.onItemSelectedListener = this

        apiLayout = findViewById(R.id.layout_adobe_fragment)

        trackEventButton = findViewById(R.id.button_track_event)
        clearVisitorButton = findViewById(R.id.button_clear_visitor)
        enableButtons(false)

        trackEventButton.setOnClickListener {
            trackEvent()
        }
        clearVisitorButton.setOnClickListener {
            clearVisitor()
        }

        refreshVisitorButton = findViewById(R.id.button_refresh)
        refreshVisitorButton.setOnClickListener {
            refreshVisitor()
        }

        setUpWrappers()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        parent?.getItemAtPosition(position)?.let { item ->
            (item as? String)?.toLowerCase(Locale.ROOT)?.let { name ->
                itemSelected = name
                wrappers[name]?.let { wrapper ->
                    fragment = AdobeVisitorFragment(
                        wrapper)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.layout_adobe_fragment, fragment)
                        .commit()
                }
            }
        }
    }

    private fun setUpWrappers() {
        wrappers["java"] = JavaWrapper(application)
        wrappers["kotlin"] = KotlinWrapper(application)

        enableButtons(true)
    }

    private fun enableButtons(enable: Boolean) {
        trackEventButton.isEnabled = enable
        clearVisitorButton.isEnabled = enable
    }

    private fun trackEvent() {
        itemSelected?.let {
            wrappers[it]?.track("event", null)
        }
    }

    private fun clearVisitor() {
        itemSelected?.let {
            wrappers[it]?.clearVisitor()
            fragment.updateVisitor(null)
        }
    }

    private fun refreshVisitor() {
        itemSelected?.let {
            fragment.updateVisitor(wrappers[it]?.initialVisitor)
        }
    }
}