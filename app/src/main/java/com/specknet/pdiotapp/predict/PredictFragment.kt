package com.specknet.pdiotapp.predict

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
import android.widget.Toast
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.internal.zzhu.runOnUiThread
import com.specknet.pdiotapp.MainActivity
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.database.RecordDao
import com.specknet.pdiotapp.database.Records
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.UserInfoViewModel
import kotlinx.android.synthetic.main.fragment_predict.togglePredict
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PredictFragment : Fragment() {
    private lateinit var interpreter: Interpreter
    private var inputDataBuff = Array(1) { FloatArray(75) { 0f } }
    private var outputDataBuff = Array(1) { FloatArray(12) { 0f } }
    lateinit var looperRespeck: Looper
    private lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)

    private lateinit var recordDao: RecordDao
    private lateinit var currentDate: String
    private lateinit var currentTime: Date
    private var previousTime: Date? = null
    private var recordingActivityIndex: Int? = null
    private var isRecording: Boolean = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_predict, container, false)

        currentDate = dateToString(Date())

        // Get the database instance
        recordDao = MainActivity.database.RecordDao()
        // 加载模型和其他初始化操作
        val modelByteBuffer = loadModelFile("basicModel1DummyLite.tflite")
        interpreter = Interpreter(modelByteBuffer)

        // Example: Query data from the database
//        queryData()

        // 获取 ViewModel
        val userModel by activityViewModels<UserInfoViewModel>()
        val userName = rootView.findViewById<TextView>(R.id.userName)
        userName.text = if (userModel.userName.value.isNullOrEmpty()) {
            "Anonymous"
        } else {
            userModel.userName.value
        }
        userModel.userName.observe(viewLifecycleOwner, Observer { newData ->
             if (newData.isNullOrEmpty()) {
                userName.text = "Anonymous"
            } else {
                 userName.text = newData
                 togglePredict.isEnabled = true
                 togglePredict.text = "Start Record"
            }
        })



        val togglePredict = rootView.findViewById<ToggleButton>(R.id.togglePredict)
        togglePredict.isEnabled = false
        togglePredict.text = "Login First"

        togglePredict.setOnCheckedChangeListener { buttonView, isChecked ->
            isRecording = isChecked
            if (userModel.userName.value != null) {
                if (isChecked) {
                    Toast.makeText(requireContext(), "Start Record", Toast.LENGTH_SHORT).show()
                } else {
                    recordActivity(userModel.userName.value!!, previousTime!!, recordingActivityIndex!!)
                    recordingActivityIndex = null
                    previousTime = null
                    Toast.makeText(requireContext(), "Record Finish", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please login First", Toast.LENGTH_SHORT).show()
            }

        }

        // 在Fragment中定义LiveData
        val currentActivityIndexLiveData = MutableLiveData<Int>()

        currentActivityIndexLiveData.observe(viewLifecycleOwner) { newActivityIndex ->
            if (userModel.userName.value != null && isRecording) {
                if (recordingActivityIndex == null) {
                    recordingActivityIndex = newActivityIndex
                    previousTime = Date()
                } else {
                    if (recordingActivityIndex != newActivityIndex) {
                        recordActivity(userModel.userName.value!!, previousTime!!, recordingActivityIndex!!)
                        recordingActivityIndex = newActivityIndex
                        previousTime = Date()
                    }
                }
            }
        }

// ...

        // TODO -- label map
        val activityMap = mapOf(
            0 to "Sitting",
            1 to "standing",
            2 to "lying down on left side",
            3 to "lying down on right side",
            4 to "lying down on stomach",
            5 to "lying down on back",
            6 to "normal walking",
            7 to "ascending stairs",
            8 to "descending stairs",
            9 to "shuffle walking",
            10 to "running/jogging",
            11 to "miscellaneous movements",
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
                    val currentActivity = activityMap[currentActivityIndex]
                    val currentActivityImage = activityImgMap[currentActivityIndex]

                    currentActivityIndexLiveData.postValue(currentActivityIndex)


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
        requireContext().registerReceiver(
            respeckLiveUpdateReceiver,
            IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST),
            null,
            handlerRespeck
        )

        return rootView
    }

    private fun recordActivity(userName: String, previousTime: Date,currentActivityIndex: Int) {
        // Example: Insert data into the database
        println("Insert")
        currentTime = Date()
        val duration = calDurationInSeconds(previousTime, currentTime)
        // adjust granularity
        if (duration >= 3) {
            val entity = Records(
                userName = userName,
                date = currentDate,
                activity = currentActivityIndex,
                duration = calDurationInSeconds(previousTime, currentTime)
            )
            println(entity)
            insertData(entity)
        }
    }

    private fun dateToString(date: Date): String {
        // 定义日期格式
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // 使用日期格式将 Date 对象转换为字符串
        return dateFormat.format(date)
    }

    private fun calDurationInSeconds(startDate: Date, endDate: Date): Long {
        val durationInMillis = endDate.time - startDate.time
        return TimeUnit.MILLISECONDS.toSeconds(durationInMillis)
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

    private fun insertData(entity: Records) {
        viewLifecycleOwner.lifecycleScope.launch {
            recordDao.insert(entity)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(respeckLiveUpdateReceiver)
        looperRespeck.quit()
        interpreter.close()
    }
}
