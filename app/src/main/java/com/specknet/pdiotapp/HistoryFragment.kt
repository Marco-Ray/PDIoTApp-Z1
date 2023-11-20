package com.specknet.pdiotapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.specknet.pdiotapp.utils.CustomSpinnerAdapter
import com.specknet.pdiotapp.utils.TaskViewModel
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
class HistoryFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var horizontalBarChart: HorizontalBarChart
    private lateinit var taskViewModel: TaskViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        taskViewModel = ViewModelProvider(requireActivity()).get(TaskViewModel::class.java)

        val spinner = view.findViewById<Spinner>(R.id.spinner)
        // 创建一个适配器（可以使用 ArrayAdapter、CursorAdapter 等）
        val taskList = listOf("Task1", "Task2", "Task3")
        val adapter = CustomSpinnerAdapter(requireContext(), taskList)        // 设置适配器
        spinner.adapter = adapter
        // 设置选择监听器
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 处理选择项的操作
                taskViewModel.updateTask(position)
                val selectedItem = taskList[position]
                // 在这里执行相应的操作
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 当没有选项被选中时的处理
            }
        }

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

        task1Labels = arrayOf(
            "sitting/standing",
            "lyingLeft",
            "lyingRight",
            "lyingStomach",
            "lyingBack",
            "normalWalking",
            "running",
            "shuffleWalking",
            "ascending",
            "descending",
            "miscMovement",
        )

        task2Labels = arrayOf(
            "sitting/standing + normalBreath",
            "lyingLeft + normalBreath",
            "lyingRight + normalBreath",
            "lyingBack + normalBreath",
            "lyingStomach + normalBreath",
            "sitting/standing + coughing",
            "lyingLeft + coughing",
            "lyingRight + coughing",
            "lyingBack + coughing",
            "lyingStomach + coughing",
            "sitting/standing + hyperventilating",
            "lyingLeft + hyperventilating",
            "lyingRight + hyperventilating",
            "lyingBack + hyperventilating",
            "lyingStomach + hyperventilating",
        )

        task3Labels = arrayOf(
            "sitting/standing + normalBreath",
            "lyingLeft + normalBreath",
            "lyingRight + normalBreath",
            "lyingBack + normalBreath",
            "lyingStomach + normalBreath",
            "sitting/standing + coughing",
            "lyingLeft + coughing",
            "lyingRight + coughing",
            "lyingBack + coughing",
            "lyingStomach + coughing",
            "sitting/standing + hyperventilating",
            "lyingLeft + hyperventilating",
            "lyingRight + hyperventilating",
            "lyingBack + hyperventilating",
            "lyingStomach + hyperventilating",
            "sitting/standing + other",
            "lyingLeft + other",
            "lyingRight + other",
            "lyingBack + other",
            "lyingStomach + other",
        )

        return view
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.history_container, fragment)
        transaction.addToBackStack(null) // 可选，用于将事务添加到返回栈
        transaction.commit()
    }

    companion object {
        lateinit var task1Labels: Array<String>
            private set
        lateinit var task2Labels: Array<String>
            private set
        lateinit var task3Labels: Array<String>
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

        fun generateColorList(num: Int): List<Int> {
            val colorList = mutableListOf<Int>()

            // 生成num种颜色
            for (i in 0 until num) {
                val hue = (i * 30) % 360  // 通过改变hue值生成不同的颜色
                val color = Color.HSVToColor(floatArrayOf(hue.toFloat(), 1f, 1f))
                colorList.add(color)
            }

            return colorList
        }
    }
}