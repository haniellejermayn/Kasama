package com.mobicom.s18.kasama.utils

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.mobicom.s18.kasama.R

object LoadingUtils {

    fun Activity.showLoading(message: String = "Loading...") {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        var loadingOverlay = rootView.findViewById<FrameLayout>(R.id.loading_overlay)

        if (loadingOverlay == null) {
            // Inflate and add loading overlay if it doesn't exist
            loadingOverlay = layoutInflater.inflate(
                R.layout.loading_overlay,
                rootView,
                false
            ) as FrameLayout
            rootView.addView(loadingOverlay)
        }

        loadingOverlay.findViewById<TextView>(R.id.loading_text)?.text = message
        loadingOverlay.visibility = View.VISIBLE
    }

    fun Activity.hideLoading() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        val loadingOverlay = rootView.findViewById<FrameLayout>(R.id.loading_overlay)
        loadingOverlay?.visibility = View.GONE
    }
}