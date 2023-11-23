package com.specknet.pdiotapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.specknet.pdiotapp.utils.UserInfoViewModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AccountFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AccountFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_account, container, false)

//        // 获取 SharedPreferences 对象
//        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
//        // 从 SharedPreferences 中读取用户名
//        val savedUsername = sharedPreferences.getString("username", null)
        //这里省略掉其他部分
        val model by activityViewModels<UserInfoViewModel>()
        val logoutMenu = view.findViewById<LinearLayout>(R.id.logout_menu)
        val loginMenu = view.findViewById<LinearLayout>(R.id.login_menu)
        val userName = model.userName.value
        val userNameTextView = view.findViewById<TextView>(R.id.userName)
        userNameTextView.text = userName

        if (userName.isNullOrEmpty()) {
            logoutMenu.visibility = View.GONE
            loginMenu.visibility = View.VISIBLE
        } else {
            logoutMenu.visibility = View.VISIBLE
            loginMenu.visibility = View.GONE
        }
        model.userName.observe(this, Observer { newData ->
            userNameTextView.text = newData
            if (newData.isNullOrEmpty()) {
                logoutMenu.visibility = View.GONE
                loginMenu.visibility = View.VISIBLE
            } else {
                logoutMenu.visibility = View.VISIBLE
                loginMenu.visibility = View.GONE
            }
        })


        val loginButton = view.findViewById<Button>(R.id.button_login)
        val signupButton = view.findViewById<Button>(R.id.button_signup)
        val logoutButton = view.findViewById<Button>(R.id.button_logout)

        loginButton.setOnClickListener {
            loadFragment(LoginFragment())
            true
        }

        signupButton.setOnClickListener {
            loadFragment(SignupFragment())
            true
        }

        logoutButton.setOnClickListener {
//            // 获取 SharedPreferences 对象
//            val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
//            // 获取 SharedPreferences 编辑器
//            val editor = sharedPreferences.edit()
//            // 保存用户名
//            editor.putString("username", null)
//            // 应用编辑器的更改
//            editor.apply()

            model.updateData("")
        }

        return view
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.addToBackStack(null) // 可选，用于将事务添加到返回栈
        transaction.commit()
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AccountFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AccountFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}