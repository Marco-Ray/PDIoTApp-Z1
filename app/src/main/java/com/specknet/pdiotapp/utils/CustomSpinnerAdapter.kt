package com.specknet.pdiotapp.utils

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.specknet.pdiotapp.R

class CustomSpinnerAdapter(
    context: Context,
    items: List<String>
) : ArrayAdapter<String>(context, R.layout.custom_spinner_item, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)

        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f) // 设置文本大小

        // 设置布局参数以控制每个项的宽度
        val layoutParams = view.layoutParams
        layoutParams.width = 300 // 替换为你想要的宽度，例如：200dp
        view.layoutParams = layoutParams

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)

        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f) // 设置下拉列表项的文本大小

        return view
    }
}
