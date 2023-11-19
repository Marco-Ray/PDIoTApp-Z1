package com.specknet.pdiotapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.gms.internal.zzhu
import com.specknet.pdiotapp.database.DayOfWeekDuration
import com.specknet.pdiotapp.database.RecordDao
import com.specknet.pdiotapp.utils.TaskViewModel
import com.specknet.pdiotapp.utils.UserInfoViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

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
    private lateinit var fromDate: Date
    private lateinit var toDate: Date

    private lateinit var recordDao: RecordDao
    // 获取 ViewModel
    private val userModel by activityViewModels<UserInfoViewModel>()
    private lateinit var colorClassArray: List<Int>
    private lateinit var taskLabels: Array<String>
    private lateinit var taskViewModel: TaskViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history_weekly, container, false)
        taskViewModel = ViewModelProvider(requireActivity()).get(TaskViewModel::class.java)
        taskLabels = if (taskViewModel.currentTask.value == 0) {
            HistoryFragment.task1Labels
        } else {
            HistoryFragment.task2Labels
        }
        colorClassArray = HistoryFragment.generateColorList(
            if (taskViewModel.currentTask.value==0) {12} else if (taskViewModel.currentTask.value==1) {15} else {20}
        )

        // init start
        // Get the database instance
        recordDao = MainActivity.database.RecordDao()
        colorClassArray = HistoryFragment.generateColorList(
            if (taskViewModel.currentTask.value==0) {12} else if (taskViewModel.currentTask.value==1) {15} else {20}
        )
        println("Weekly color ${colorClassArray.size}")

        horizontalBarChart = view.findViewById(R.id.hBarChartWeekly)
        // 配置水平堆叠的条形图
        configureHorizontalBarChart()

        firstDateView = view.findViewById(R.id.firstDate)
        secondDateView = view.findViewById(R.id.secondDate)

        toDate = stringToDate(taskViewModel.selectedDate.value!!)!!
        fromDate = addDaysToDate(toDate, -7)
        queryWeeklyData(formatDateToString(fromDate), formatDateToString(toDate))
        firstDateView.text = formatDateToString(fromDate)
        secondDateView.text = formatDateToString(toDate)

        firstDateView.setOnClickListener {
            showDatePickerDialog { selectedDate ->
                fromDate = selectedDate
                if ((toDate == null) || (fromDate!! > toDate!!)) {
                    toDate = addDaysToDate(fromDate!!, 1)
                    taskViewModel.updateDate(formatDateToString(toDate))
                    secondDateView.text = formatDateToString(toDate!!)
                }
                firstDateView.text = formatDateToString(fromDate!!)
                queryWeeklyData(formatDateToString(fromDate), formatDateToString(toDate))

            }
        }

        secondDateView.setOnClickListener {
            showDatePickerDialog { selectedDate ->
                toDate = selectedDate
                taskViewModel.updateDate(formatDateToString(toDate))
                if ((fromDate == null) || (fromDate!! > toDate!!)) {
                    fromDate = addDaysToDate(toDate!!, -1)
                    firstDateView.text = formatDateToString(fromDate!!)
                }
                secondDateView.text = formatDateToString(toDate!!)
                queryWeeklyData(formatDateToString(fromDate), formatDateToString(toDate))
            }
        }

        taskViewModel.currentTask.observe(viewLifecycleOwner, Observer { newTask ->
            setLegend()
            colorClassArray = HistoryFragment.generateColorList(
                if (newTask==0) {12} else if (newTask==1) {15} else {20}
            )
            queryWeeklyData(formatDateToString(fromDate), formatDateToString(toDate))
        })

        return view
    }

    private fun convertToBarEntries(dayOfWeekDurations: List<DayOfWeekDuration>): List<BarEntry> {
        val barEntries = mutableListOf<BarEntry>()

        for (dayOfWeek in (0..6).toList()) {
            val subset = dayOfWeekDurations.filter { it.dayOfWeek == dayOfWeek }
            val floatArray = FloatArray(
                if (taskViewModel.currentTask.value == 0) {12} else if (taskViewModel.currentTask.value==1) {15} else {20}
            )
            for (activity in subset) {
                floatArray[activity.activityType] = activity.totalDuration.toFloat()
            }
            val barEntry = BarEntry(dayOfWeek.toFloat(), floatArray)
            barEntries.add(barEntry)
        }
        return barEntries
    }

    private fun queryWeeklyData(startDate: String, endDate: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val entities = recordDao.getTotalDurationByDayOfWeekInDateRange(userModel.userName.value!!, taskViewModel.currentTask.value!!, startDate, endDate)
            println(entities)
            // 创建数据集
            val entries = convertToBarEntries(entities)
            val dataSet = BarDataSet(entries, "示例数据")
            dataSet.colors = colorClassArray
            // 创建 BarData 对象并设置数据集
            val data = BarData(dataSet)
            // 设置数据到图表
            zzhu.runOnUiThread {
                horizontalBarChart.data = data
                horizontalBarChart.invalidate()
            }
        }
    }

    private fun configureHorizontalBarChart() {
        // 自定义 X 轴标签
        val weekdayLabels = arrayOf(
            "Sunday",
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday",
        )

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
        xAxis.labelCount = taskLabels.size

        xAxis.valueFormatter = IndexAxisValueFormatter(weekdayLabels)

        val leftAxis = horizontalBarChart.axisLeft
        leftAxis.axisMinimum = 0f

        val rightAxis = horizontalBarChart.axisRight
        rightAxis.axisMinimum = 0f

        setLegend()

    }

    private fun setLegend() {
        // 配置 Legend
        taskLabels = if (taskViewModel.currentTask.value == 0) {
            HistoryFragment.task1Labels
        } else if (taskViewModel.currentTask.value==1) {
            HistoryFragment.task2Labels
        } else {
            HistoryFragment.task3Labels
        }
        val legend: Legend = horizontalBarChart.legend
        legend.isWordWrapEnabled = true
        val legendEntries = mutableListOf<LegendEntry>()
        for ((color, label) in colorClassArray.zip(taskLabels)) {
            legendEntries.add(LegendEntry(label, Legend.LegendForm.SQUARE, 8f, 8f, null, color))
        }
        // 设置自定义的 LegendEntry 列表
        legend.setCustom(legendEntries)
    }

    private fun formatDateToString(date: Date?): String {
        date?.let {
            val format = SimpleDateFormat("yyyy-MM-dd")
            return format.format(it)
        }
        return "null" // 或者返回空字符串，取决于你的需求
    }

    fun stringToDate(dateString: String): Date? {
        val format = SimpleDateFormat("yyyy-MM-dd")
        try {
            return format.parse(dateString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
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

    override fun onPause() {
        super.onPause()
        horizontalBarChart.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        horizontalBarChart.clear()
    }
}