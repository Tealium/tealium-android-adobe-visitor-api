package com.tealium.adobe

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.tealium.adobe.api.AdobeAuthState
import com.tealium.adobe.api.AdobeVisitor
import com.tealium.adobe.api.ResponseListener
import com.tealium.adobe.wrappers.TealiumWrapper
import org.json.JSONObject

class AdobeVisitorFragment(
    private val wrapper: TealiumWrapper,
    private var visitor: AdobeVisitor? = wrapper.initialVisitor
) : Fragment() {

    private lateinit var linkKnownIdEditText: EditText
    private lateinit var linkExistingVisitorIdButton: Button

    private lateinit var visitorInfoTextView: TextView

    private val mainHandler = Handler(Looper.getMainLooper())

    private val responseListener = object : ResponseListener<AdobeVisitor> {
        override fun success(data: AdobeVisitor) {
            mainHandler.post {
                updateVisitor(data)
            }
        }

        override fun failure(errorCode: Int, ex: Exception?) {
            mainHandler.post {
                Toast.makeText(context, "Error updating visitor ($errorCode)", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.adobe_visitor_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        visitorInfoTextView = view.findViewById(R.id.text_adobe_visitor)

        linkKnownIdEditText = view.findViewById(R.id.edit_known_id)
        linkKnownIdEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                linkExistingVisitorIdButton.isEnabled = s != null && s.length > 0
            }
        })

        linkExistingVisitorIdButton = view.findViewById(R.id.button_link_existing_ecid)
        linkExistingVisitorIdButton.isEnabled = false
        linkExistingVisitorIdButton.setOnClickListener {
            val text: String = linkKnownIdEditText.text.toString()
            linkExisting(text)
        }

        updateVisitor(visitor)
    }

    fun updateVisitor(visitor: AdobeVisitor?) {
        this.visitor = visitor
        visitor?.let {
            visitorInfoTextView.text = it.toJson().toString(4)
        } ?: run {
            visitorInfoTextView.text = getString(R.string.no_visitor)
        }
    }

    private fun linkExisting(knownId: String? = null) {
        visitor?.let {
            wrapper.linkExistingEcidToKnownIdentifier(
                knownId ?: "someone@tealium.com",
                "0",
                AdobeAuthState.AUTH_STATE_UNKNOWN,
                responseListener
            )
        }
    }
}

fun AdobeVisitor.toJson(): JSONObject {
    val json = JSONObject()
    json.put("ecid", experienceCloudId)
    json.put("region", region)
    json.put("blob", blob)
    json.put("ttl", idSyncTTL)
    json.put("nextRefresh", nextRefresh)
    return json
}