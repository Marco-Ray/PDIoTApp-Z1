package com.specknet.pdiotapp.predict


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
// * Use the [PredictFragment.newInstance] factory method to
// * create an instance of this fragment.
// */
//class PredictFragment : Fragment() {
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
//        return inflater.inflate(R.layout.fragment_predict, container, false)
//    }
//
//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment PredictFragment.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            PredictFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
//}

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.gms.internal.zzhu.runOnUiThread
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.UserInfoViewModel
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PredictFragment : Fragment() {
    private lateinit var interpreter: Interpreter
    private var inputDataBuff = Array(1) { FloatArray(75) { 0f } }
    private var outputDataBuff = Array(1) { FloatArray(12) { 0f } }
    lateinit var looperRespeck: Looper
    private lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_predict, container, false)

        // TODO -- label map
        val activityMap = mapOf(
            0 to "Sitting",
            1 to "Standing",
            2 to "Lying",
            3 to "3",
            4 to "4",
            5 to "5",
            6 to "6",
            7 to "7",
            8 to "8",
            9 to "9",
            10 to "10",
            11 to "11",
        )

        // TODO
        val activityImgMap = mapOf(
            0 to R.drawable.sitting,
            1 to R.drawable.standing,
            2 to R.drawable.lying,
            3 to R.drawable.sitting,
            4 to R.drawable.sitting,
            5 to R.drawable.sitting,
            6 to R.drawable.sitting,
            7 to R.drawable.sitting,
            8 to R.drawable.sitting,
            9 to R.drawable.sitting,
            10 to R.drawable.sitting,
            11 to R.drawable.sitting,
        )

        // 示例：获取 TextView 并设置文本
        val textView = rootView.findViewById<TextView>(R.id.predicted_activity)
        // Find the ImageView by its ID
        val imageView = rootView.findViewById<ImageView>(R.id.current_activity_image)

        // 设置点击事件监听器
        rootView.setOnClickListener {
            // 处理点击事件
        }

        // set up the broadcast receiver
        respeckLiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "Predictor receiver is running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ

                    val currentActivityIndex = predict(x,y,z)
//                    val currentActivityIndex = 1
                    val currentActivity = activityMap[currentActivityIndex]
                    val currentActivityImage = activityImgMap[currentActivityIndex]

                    Log.d("Live", "Predicted " + currentActivity + "for" + liveData)
                    runOnUiThread {
                        textView.text = "$currentActivity"
                        // Set the new image resource
                        imageView.setImageResource(currentActivityImage as Int)
                    }
                }
            }
        }


        val handlerThreadRespeck = HandlerThread("bgThreadRespeckPredictLive")
        handlerThreadRespeck.start()
        looperRespeck = handlerThreadRespeck.looper
        val handlerRespeck = Handler(looperRespeck)
        this.requireActivity().registerReceiver(
            respeckLiveUpdateReceiver,
            IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST),
            null,
            handlerRespeck
        )

        // 获取 ViewModel
        val model by activityViewModels<UserInfoViewModel>()
        val userName = rootView.findViewById<TextView>(R.id.userName)
        userName.text = if (model.userName.value.isNullOrEmpty()) {
            "Anonymous"
        } else {
            model.userName.value
        }
        model.userName.observe(viewLifecycleOwner, Observer { newData ->
            userName.text = if (newData.isNullOrEmpty()) {
                "Anonymous"
            } else {
                newData
            }
        })

        // 加载模型和其他初始化操作
        val modelByteBuffer = loadModelFile("basicModel1DummyLite.tflite")
        interpreter = Interpreter(modelByteBuffer)

        return rootView
    }

    private fun loadModelFile(path2Model: String): ByteBuffer {
        val assetManager = requireContext().assets
        val modelInputStream = assetManager.open(path2Model)
        val modelBuffer = modelInputStream.readBytes()

        val modelByteBuffer = ByteBuffer.allocateDirect(modelBuffer.size)
        modelByteBuffer.order(ByteOrder.nativeOrder())
        modelByteBuffer.put(modelBuffer)
        modelByteBuffer.rewind()
        return modelByteBuffer
    }

    private fun predict(x: Float, y: Float, z: Float): Int? {
        inputDataBuff[0][0] = 1f
        inputDataBuff[0][1] = 2f
        inputDataBuff[0][2] = 3f
        inputDataBuff[0] = inputDataBuff[0].drop(3).toFloatArray() // drop first 3 elements (oldest reading)
        inputDataBuff[0] = inputDataBuff[0] + x + y + z

        interpreter.run(inputDataBuff, outputDataBuff)

        var maxIndex = 0
        var maxValue = outputDataBuff[0][0]

        for (i in 1 until outputDataBuff[0].size) {
            if (outputDataBuff[0][i] > maxValue) {
                maxValue = outputDataBuff[0][i]
                maxIndex = i
            }
        }

        return maxIndex
    }


    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(respeckLiveUpdateReceiver)
        looperRespeck.quit()
        interpreter.close()
    }
}
