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
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Date
import java.util.concurrent.TimeUnit

class PredictFragment : Fragment() {
    private lateinit var interpreter: Interpreter
    private var inputDataBuff = Array(1) { FloatArray(75) { 0f } }
    private var outputDataBuff = Array(1) { FloatArray(12) { 0f } }
    lateinit var looperRespeck: Looper
    private lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)

    private lateinit var recordDao: RecordDao
    private lateinit var currentTime: Date
    private var previousTime: Date? = null
    private var recordingActivity: String? = null
    private var isRecording: Boolean = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_predict, container, false)

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
            userName.text = if (newData.isNullOrEmpty()) {
                "Anonymous"
            } else {
                newData
            }
        })

        val togglePredict = rootView.findViewById<ToggleButton>(R.id.togglePredict)
        togglePredict.setOnCheckedChangeListener { buttonView, isChecked ->
            isRecording = isChecked
            if (isChecked) {
                Toast.makeText(requireContext(), "Start Record", Toast.LENGTH_SHORT).show()
            } else {
                recordActivity(userModel.userName.value!!, previousTime!!, recordingActivity!!)
                recordingActivity = null
                previousTime = null
                Toast.makeText(requireContext(), "Record Finish", Toast.LENGTH_SHORT).show()
            }
        }

        // 在Fragment中定义LiveData
        val currentActivityLiveData = MutableLiveData<String>()

        currentActivityLiveData.observe(viewLifecycleOwner) { newActivity ->
            if (userModel.userName.value != null && isRecording) {
                if (recordingActivity == null) {
                    recordingActivity = newActivity
                    previousTime = Date()
                } else {
                    if (recordingActivity != newActivity) {
                        recordActivity(userModel.userName.value!!, previousTime!!, recordingActivity!!)
                        recordingActivity = newActivity
                        previousTime = Date()
                    }
                }
            }
        }

// ...

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
                    val currentActivity = activityMap[currentActivityIndex]
                    val currentActivityImage = activityImgMap[currentActivityIndex]

                    currentActivityLiveData.postValue(currentActivity)


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

    private fun recordActivity(userName: String, previousTime: Date,currentActivity: String) {
        // Example: Insert data into the database
        println("Insert")
        currentTime = Date()
        val duration = calDurationInSeconds(previousTime, currentTime)
        // adjust granularity
        if (duration < 1) {
            val entity = Records(
                userName = userName,
                dateTime = currentTime,
                activity = currentActivity,
                duration = calDurationInSeconds(previousTime, currentTime)
            )
            println(entity)
            insertData(entity)
        }
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
