package com.specknet.pdiotapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.specknet.pdiotapp.database.RecordDao
import com.specknet.pdiotapp.database.RecordDatabase
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HistoryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HistoryFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var horizontalBarChart: HorizontalBarChart


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        val toggleButton = view.findViewById<ToggleButton>(R.id.toggleButton)
        loadFragment(HistoryDailyFragment())

        toggleButton.setOnCheckedChangeListener { buttonView, isChecked ->
            // 处理开关状态变化事件
            if (isChecked) {
                // 开关已打开
                loadFragment(HistoryWeeklyFragment())
                true
            } else {
                loadFragment(HistoryDailyFragment())
                true
            }
        }

        colorClassArray = generateColorList(12)

        customLabels = arrayOf(
            "Sitting",
            "standing",
            "lying down on left side",
            "lying down on right side",
            "lying down on stomach",
            "lying down on back",
            "normal walking",
            "ascending stairs",
            "descending stairs",
            "shuffle walking",
            "running/jogging",
            "miscellaneous movements",
        )

        return view
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.history_container, fragment)
        transaction.addToBackStack(null) // 可选，用于将事务添加到返回栈
        transaction.commit()
    }

    fun generateColorList(num: Int): List<Int> {
        val colorList = mutableListOf<Int>()

        // 生成12种颜色
        for (i in 0 until num) {
            val hue = (i * 30) % 360  // 通过改变hue值生成不同的颜色
            val color = Color.HSVToColor(floatArrayOf(hue.toFloat(), 1f, 1f))
            colorList.add(color)
        }

        return colorList
    }

    companion object {
        lateinit var colorClassArray: List<Int>
            private set
        lateinit var customLabels: Array<String>
            private set
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HistoryFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic fun newInstance(param1: String, param2: String) =
                HistoryFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}