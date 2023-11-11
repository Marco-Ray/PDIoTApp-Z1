package com.specknet.pdiotapp
//
//import android.os.Bundle
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//
//// TODO: Rename parameter arguments, choose names that match
//// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//private const val ARG_PARAM1 = "param1"
//private const val ARG_PARAM2 = "param2"
//
///**
// * A simple [Fragment] subclass.
// * Use the [LoginFragment.newInstance] factory method to
// * create an instance of this fragment.
// */
//class LoginFragment : Fragment() {
//    // TODO: Rename and change types of parameters
//    private var param1: String? = null
//    private var param2: String? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
//        }
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_login, container, false)
//    }
//
//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment LoginFragment.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            LoginFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
//}
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import com.specknet.pdiotapp.utils.UserInfoViewModel
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

class LoginFragment : Fragment() {
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        usernameEditText = view.findViewById(R.id.editTextUsername)
        passwordEditText = view.findViewById(R.id.editTextPassword)
        val loginButton = view.findViewById<Button>(R.id.buttonLogin)

//        // 获取 SharedPreferences 对象
//        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
//        // 获取 SharedPreferences 编辑器
//        val editor = sharedPreferences.edit()

        //这里省略掉其他部分
        val model by activityViewModels<UserInfoViewModel>()

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (isValidLogin(username, password)) {
                if (checkLoginCredentials(username, password)) {
                    showToast("Login Successfully")
                    // 例如，跳转到用户主页：loadFragment(UserHomeFragment())
//                    // 保存用户名
//                    editor.putString("username", username)
//                    // 应用编辑器的更改
//                    editor.apply()

                    // update username
                    model.updateData(username)

                    loadFragment(AccountFragment())
                    true
                } else {
                    showToast("Login Failed, please check your username and password")
                }
            } else {
                showToast("Please provide username and password")
            }
        }

        return view
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.addToBackStack(null) // 可选，用于将事务添加到返回栈
        transaction.commit()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidLogin(username: String, password: String): Boolean {
        return username.isNotEmpty() && password.isNotEmpty()
    }

    private fun checkLoginCredentials(username: String, password: String): Boolean {
        try {
            val csvFile = File(requireContext().getExternalFilesDir(null), "registration.csv")
            if (csvFile.exists()) {
                val reader = BufferedReader(FileReader(csvFile))
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val userInfo = line?.split(",")
                    if (userInfo?.size == 2 && userInfo[0] == username && userInfo[1] == password) {
                        return true // 找到匹配的用户名和密码
                    }
                }
                reader.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false // 没有找到匹配的用户名和密码
    }
}

