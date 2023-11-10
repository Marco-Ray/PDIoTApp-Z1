package com.specknet.pdiotapp

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.Calendar
import java.text.SimpleDateFormat
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
class HistoryWeeklyFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var horizontalBarChart: HorizontalBarChart
    private lateinit var firstDateView: TextView
    private lateinit var secondDateView: TextView
    private var fromDate: Date? = null
    private var toDate: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history_weekly, container, false)
        horizontalBarChart = view.findViewById(R.id.hBarChartWeekly)
        // 配置水平堆叠的条形图
        configureHorizontalBarChart()

        firstDateView = view.findViewById(R.id.firstDate)
        firstDateView.setOnClickListener {
            showDatePickerDialog { selectedDate ->
                fromDate = selectedDate
                if ((toDate == null) || (fromDate!! > toDate!!)) {
                    toDate = addDaysToDate(fromDate!!, 1)
                    secondDateView.text = formatDateToString(toDate!!)
                }
                firstDateView.text = formatDateToString(fromDate!!)

            }
        }

        secondDateView = view.findViewById(R.id.secondDate)
        secondDateView.setOnClickListener {
            showDatePickerDialog { selectedDate ->
                toDate = selectedDate
                if ((fromDate == null) || (fromDate!! > toDate!!)) {
                    fromDate = addDaysToDate(toDate!!, -1)
                    firstDateView.text = formatDateToString(fromDate!!)
                }
                secondDateView.text = formatDateToString(toDate!!)
            }
        }

        // 创建示例数据
        val entries1 = ArrayList<BarEntry>()
        entries1.add(BarEntry(0f, floatArrayOf(5f, 15f, 20f)))
        entries1.add(BarEntry(1f, floatArrayOf(5f, 15f, 20f)))
        entries1.add(BarEntry(2f, floatArrayOf(5f, 20f, 1f)))
        entries1.add(BarEntry(3f, floatArrayOf(5f, 15f, 20f)))
        entries1.add(BarEntry(4f, floatArrayOf(5f, 3f, 10f)))
        entries1.add(BarEntry(5f, floatArrayOf(5f, 2f, 10f)))
        entries1.add(BarEntry(6f, floatArrayOf(5f, 15f, 20f)))


        // 创建数据集
        val dataSet1 = BarDataSet(entries1, "数据集 1")
        val colorClassArray = listOf<Int>(Color.BLUE, Color.CYAN, Color.RED)
        dataSet1.colors = colorClassArray

        // 创建 BarData 对象并设置数据集
        val data = BarData(dataSet1)

        // 设置数据到图表
        horizontalBarChart.data = data

        return view
    }

    private fun configureHorizontalBarChart() {
        // 自定义 X 轴标签
        val customLabels = arrayOf(
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday",
            "Sunday",
        ).reversed()

        // 配置水平堆叠的条形图
        horizontalBarChart.setDrawBarShadow(false)
        horizontalBarChart.setDrawValueAboveBar(true)
        horizontalBarChart.description.isEnabled = false

        val xAxis = horizontalBarChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        // 设置 X 轴的标签间隔为1，强制显示所有标签
        xAxis.isGranularityEnabled = true
        xAxis.granularity = 1f
        xAxis.axisMinimum = 0f
        xAxis.labelCount = customLabels.size

        xAxis.valueFormatter = IndexAxisValueFormatter(customLabels)

        val leftAxis = horizontalBarChart.axisLeft
        leftAxis.axisMinimum = 0f

        val rightAxis = horizontalBarChart.axisRight
        rightAxis.axisMinimum = 0f
    }

    fun formatDateToString(date: Date?): String {
        date?.let {
            val format = SimpleDateFormat("yyyy-MM-dd")
            return format.format(it)
        }
        return "null" // 或者返回空字符串，取决于你的需求
    }

    private fun addDaysToDate(date: Date, days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return calendar.time
    }

    private fun showDatePickerDialog(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.DatePickerTheme,
            DatePickerDialog.OnDateSetListener { _, selectedYear, selectedMonth, selectedDay ->
                // 在 OnDateSetListener 中创建 Date 对象
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                val date = selectedDate.time

                // 调用回调函数，将选定的 Date 对象传递出去
                onDateSelected(date)
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