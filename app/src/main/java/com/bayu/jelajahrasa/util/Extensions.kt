package com.bayu.jelajahrasa.util

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.bayu.jelajahrasa.R
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView

fun View.showAppSnackbar(message: String) {
    val snackbar = Snackbar.make(this, message, Snackbar.LENGTH_SHORT)
    val sbView = snackbar.view
    ViewCompat.setBackgroundTintList(sbView, null)
    sbView.background = ContextCompat.getDrawable(context, R.drawable.bg_card)

    val textView = sbView.findViewById<android.widget.TextView>(com.google.android.material.R.id.snackbar_text)
    textView?.setTextColor(ContextCompat.getColor(context, R.color.theme_text))
    textView?.textSize = 14f
    textView?.setPadding(24, 16, 24, 16)
    textView?.maxLines = 3

    val params = sbView.layoutParams as? android.view.ViewGroup.MarginLayoutParams
    params?.setMargins(
        params.leftMargin + 24,
        params.topMargin,
        params.rightMargin + 24,
        params.bottomMargin + 220
    )
    sbView.layoutParams = params

    snackbar.show()
}

fun MaterialAutoCompleteTextView.setupDynamicDropdownHeight(maxVisibleItems: Int = 4) {
    setOnClickListener {
        showCustomDropDown(maxVisibleItems)
    }
    setOnFocusChangeListener { _, hasFocus ->
        if (hasFocus) {
            showCustomDropDown(maxVisibleItems)
        }
    }
}

private fun MaterialAutoCompleteTextView.showCustomDropDown(maxVisibleItems: Int) {
    val count = adapter?.count ?: 0
    if (count > 0) {
        val itemHeightPx = (44 * resources.displayMetrics.density).toInt()
        val calculatedHeight = count * itemHeightPx
        val maxHeight = (44 * maxVisibleItems * resources.displayMetrics.density).toInt()
        dropDownHeight = minOf(calculatedHeight, maxHeight)
    }
    showDropDown()
}
