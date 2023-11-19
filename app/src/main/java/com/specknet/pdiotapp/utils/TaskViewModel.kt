package com.specknet.pdiotapp.utils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskViewModel : ViewModel() {
    // 在此处定义视图相关的数据和方法
    // 创建一个可变的 LiveData 对象
    private val _currentTask = MutableLiveData<Int>().apply {
        value = 0
    }
    // 公开一个不可变的 LiveData 对象
    val currentTask: LiveData<Int>
        get() = _currentTask
    // 在需要时更新 LiveData 数据
    fun updateTask(newData: Int) {
        _currentTask.value = newData
    }

    private val _selectedDate = MutableLiveData<String>().apply {
        value = dateToString(Date())
    }
    val selectedDate: LiveData<String>
        get() = _selectedDate
    fun updateDate(newData: String) {
        _selectedDate.value = newData
    }
    private fun dateToString(date: Date): String {
        // 定义日期格式
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // 使用日期格式将 Date 对象转换为字符串
        return dateFormat.format(date)
    }
}
