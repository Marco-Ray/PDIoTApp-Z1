package com.specknet.pdiotapp.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BLEStatusViewModel: ViewModel() {
    // 在此处定义视图相关的数据和方法
    // 创建一个可变的 LiveData 对象
    private val _respeckStatus = MutableLiveData<Boolean>().apply {
        value = false
    }
    // 公开一个不可变的 LiveData 对象
    val respeckStatus: LiveData<Boolean>
        get() = _respeckStatus
    // 在需要时更新 LiveData 数据
    fun updateREspeckStatus(newData: Boolean) {
        _respeckStatus.value = newData
    }

    private val _thingyStatus = MutableLiveData<Boolean>().apply {
        value = false
    }
    // 公开一个不可变的 LiveData 对象
    val thingyStatus: LiveData<Boolean>
        get() = _thingyStatus
    // 在需要时更新 LiveData 数据
    fun updateThingyStatus(newData: Boolean) {
        _thingyStatus.value = newData
    }
}