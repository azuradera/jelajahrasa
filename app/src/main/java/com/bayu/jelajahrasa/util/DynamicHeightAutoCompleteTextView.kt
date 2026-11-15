package com.bayu.jelajahrasa.util

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class DynamicHeightAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.autoCompleteTextViewStyle
) : MaterialAutoCompleteTextView(context, attrs, defStyleAttr) {

    var maxVisibleItems: Int = 4

    override fun showDropDown() {
        val count = adapter?.count ?: 0
        if (count > 0) {
            val itemHeightPx = (44 * resources.displayMetrics.density).toInt()
            val calculatedHeight = count * itemHeightPx
            val maxHeight = (44 * maxVisibleItems * resources.displayMetrics.density).toInt()
            dropDownHeight = minOf(calculatedHeight, maxHeight)
        }
        super.showDropDown()
    }
}
