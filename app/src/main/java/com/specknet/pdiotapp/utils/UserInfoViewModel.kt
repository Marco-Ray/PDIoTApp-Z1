package com.specknet.pdiotapp.utils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
class UserInfoViewModel : ViewModel() {
    // 在此处定义视图相关的数据和方法
    // 创建一个可变的 LiveData 对象
    private val _userName = MutableLiveData<String>()

    // 公开一个不可变的 LiveData 对象
    val userName: LiveData<String>
        get() = _userName

    // 在需要时更新 LiveData 数据
    fun updateData(newData: String) {
        _userName.value = newData
    }
}
