package com.specknet.pdiotapp

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.specknet.pdiotapp.database.RecordDao
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HistoryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HistoryDailyFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var horizontalBarChart: HorizontalBarChart
    private lateinit var dateView: TextView
    private lateinit var todayDate: String

    private lateinit var recordDao: RecordDao
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history_daily, container, false)

        // Get the database instance
        recordDao = MainActivity.database.RecordDao()

        todayDate = dateToString(Date())
        queryDailyData(todayDate)

        dateView = view.findViewById(R.id.date)
        dateView.text = todayDate
        dateView.setOnClickListener {
            showDatePickerDialog()
        }

        horizontalBarChart = view.findViewById(R.id.hBarChartDaily)

        // 配置水平柱状图
        horizontalBarChart.description.isEnabled = false
        horizontalBarChart.setTouchEnabled(true)
        horizontalBarChart.setDrawValueAboveBar(true)

        // 自定义 X 轴
        val xAxis = horizontalBarChart.xAxis

        // 自定义 X 轴标签
        val customLabels = arrayOf(
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
        ).reversed()
        xAxis.valueFormatter = IndexAxisValueFormatter(customLabels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        // 设置 X 轴的标签间隔为1，强制显示所有标签
        xAxis.isGranularityEnabled = true
        xAxis.granularity = 1f
        xAxis.labelCount = customLabels.size;

        // 自定义 Y 轴
        val leftAxis = horizontalBarChart.axisLeft
        val rightAxis = horizontalBarChart.axisRight
        leftAxis.axisMinimum = 0f
        rightAxis.axisMinimum = 0f

        // 创建示例数据
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, 10f))
        entries.add(BarEntry(1f, 1f))
        entries.add(BarEntry(2f, 2f))
        entries.add(BarEntry(3f, 3f))
        entries.add(BarEntry(4f, 4f))
        entries.add(BarEntry(5f, 8f))
        entries.add(BarEntry(6f, 4f))
        entries.add(BarEntry(7f, 5f))
        entries.add(BarEntry(8f, 6f))
        entries.add(BarEntry(9f, 4f))
        entries.add(BarEntry(10f, 0f))
        entries.add(BarEntry(11f, 20f))


        // 允许 X 轴和 Y 轴自动缩放以适应数据
//        xAxis.axisMinimum = entries.minByOrNull { it.x }?.x ?: 0f
//        xAxis.axisMaximum = entries.maxByOrNull { it.x }?.x ?: 1f
//        leftAxis.axisMinimum = entries.minByOrNull { it.y }?.y ?: 0f
//        leftAxis.axisMaximum = entries.maxByOrNull { it.y }?.y ?: 1f
//        rightAxis.axisMinimum = entries.minByOrNull { it.y }?.y ?: 0f
//        rightAxis.axisMaximum = entries.maxByOrNull { it.y }?.y ?: 1f

        // 创建数据集
        val dataSet = BarDataSet(entries, "示例数据")
        dataSet.color = Color.BLUE

        // 创建 BarData 对象并设置数据集
        val data = BarData(dataSet)

        // 设置数据到图表
        horizontalBarChart.data = data

        return view
    }

    private fun queryDailyData(selectedDate: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val entities = recordDao.getEntitiesByDate(selectedDate)
            println(entities)
            // Handle the list of entities as needed
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.DatePickerTheme,
            DatePickerDialog.OnDateSetListener { _, selectedYear, selectedMonth, selectedDay ->
                // 在此处处理选定的日期
                val selectedDate = "$selectedYear-${selectedMonth + 1}-$selectedDay"
                dateView.text = selectedDate
            },
            year,
            month,
            day
        )

        // 设置日期选择对话框的最小日期和最大日期（可选）
        // datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        // datePickerDialog.datePicker.maxDate = System.currentTimeMillis() + 1000

        datePickerDialog.show()
    }

    fun dateToString(date: Date): String {
        // 定义日期格式
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // 使用日期格式将 Date 对象转换为字符串
        return dateFormat.format(date)
    }

    companion object {
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