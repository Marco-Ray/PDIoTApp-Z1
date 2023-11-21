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
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.internal.zzhu.runOnUiThread
import com.specknet.pdiotapp.MainActivity
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.database.RecordDao
import com.specknet.pdiotapp.database.Records
import com.specknet.pdiotapp.utils.BLEStatusViewModel
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.CustomSpinnerAdapter
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.UserInfoViewModel
import kotlinx.android.synthetic.main.fragment_predict.togglePredict
import kotlinx.coroutines.launch
import org.jtransforms.fft.DoubleFFT_1D
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

class PredictFragment : Fragment() {
    private var interpreter11: Interpreter? = null
    private var interpreter12: Interpreter? = null
    private var interpreter13: Interpreter? = null
    private var interpreter21: Interpreter? = null
    private var interpreter22: Interpreter? = null
    private var interpreter23: Interpreter? = null
    private var interpreter24: Interpreter? = null
    private var interpreter25: Interpreter? = null
    private var interpreter26: Interpreter? = null
    private var interpreter31: Interpreter? = null
    private var interpreter32: Interpreter? = null
    private var interpreter33: Interpreter? = null
    private var interpreter34: Interpreter? = null
    private var interpreter35: Interpreter? = null
    private var interpreter36: Interpreter? = null
    private var rawInputDataBuff = Array(1) { FloatArray(300) { 0f } }
    private var accXInputDataBuff = Array<Double>(50) { 0.0 }
    private var accYInputDataBuff = Array<Double>(50) { 0.0 }
    private var accZInputDataBuff = Array<Double>(50) { 0.0 }
    private var gyroXInputDataBuff = Array<Double>(50) { 0.0 }
    private var gyroYInputDataBuff = Array<Double>(50) { 0.0 }
    private var gyroZInputDataBuff = Array<Double>(50) { 0.0 }
    private var binaryOutputDataBuff = Array(1) { FloatArray(2) { 0f } }
    private var stationOutputDataBuff = Array(1) { FloatArray(5) { 0f } }
    private var nonStationOutputDataBuff =  Array(1) { FloatArray(6) { 0f } }
    private var task2ActivityOutputDataBuff =  Array(1) { FloatArray(5) { 0f } }
    private var task2SymptomOutputDataBuff =  Array(1) { FloatArray(3) { 0f } }
    private var task3ActivityOutputDataBuff =  Array(1) { FloatArray(5) { 0f } }
    private var task3SymptomOutputDataBuff =  Array(1) { FloatArray(4) { 0f } }
    lateinit var looperRespeck: Looper
    private lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)
    private lateinit var handlerThread: HandlerThread

    private lateinit var recordDao: RecordDao
    private lateinit var currentDate: String
    private lateinit var currentTime: Date
    private var previousTime: Date? = null
    private var recordingActivityIndex: Int? = null
    private var isRecording: Boolean = false
    private var counter: Int = 0
    private var currentTask: Int = 0
    private var currentActivity: String = "Unknown"
    private var currentActivityImage: Int = R.drawable.unknown

    private lateinit var bleStatusViewModel: BLEStatusViewModel
    private val currentSymptomIndexLiveData = MutableLiveData<Int>()

    // Labels
    private val task1Map = mapOf(
        0 to "sitting/standing",
        1 to "lyingLeft",
        2 to "lyingRight",
        3 to "lyingStomach",
        4 to "lyingBack",
        5 to "normalWalking",
        6 to "running",
        7 to "shuffleWalking",
        8 to "ascending",
        9 to "descending",
        10 to "miscMovement",
    )
    // TODO
    private val taskImgMap = mapOf(
        0 to R.drawable.sitting,
        1 to R.drawable.lying,
        2 to R.drawable.lying,
        3 to R.drawable.lying,
        4 to R.drawable.lying,
        5 to R.drawable.walking,
        6 to R.drawable.walking,
        7 to R.drawable.walking,
        8 to R.drawable.ascending,
        9 to R.drawable.descending,
        10 to R.drawable.unknown,
    )

    private val task2ActivityMap = mapOf(
        0 to "sitting/standing",
        1 to "lyingLeft",
        2 to "lyingRight",
        3 to "lyingBack",
        4 to "lyingStomach",
    )

    private val task2SymptomMap = mapOf(
        0 to "normalBreath",
        1 to "coughing",
        2 to "hyperventilating",
        3 to "other"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_predict, container, false)

        currentDate = dateToString(Date())

        // Get the database instance
        recordDao = MainActivity.database.RecordDao()
        loadPredictTask()


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

        val respeckStatus = rootView.findViewById<TextView>(R.id.respeck_status)
        bleStatusViewModel = ViewModelProvider(requireActivity()).get(BLEStatusViewModel::class.java)
        bleStatusViewModel.respeckStatus.observe(viewLifecycleOwner, Observer {newStatus ->
            respeckStatus.text = if (newStatus) {
                "Respeck: Connected"
            } else {
                "Respeck: Disconnected"
            }
        })
        val thingyStatus = rootView.findViewById<TextView>(R.id.thingy_status)
        bleStatusViewModel.thingyStatus.observe(viewLifecycleOwner, Observer {newStatus ->
            thingyStatus.text = if (newStatus) {
                "Thingy: Connected"
            } else {
                "Thingy: Disconnected"
            }
        })

        val spinner = rootView.findViewById<Spinner>(R.id.spinner)
        // 创建一个适配器（可以使用 ArrayAdapter、CursorAdapter 等）
        val taskList = listOf("Task1", "Task2", "Task3")
        val adapter = CustomSpinnerAdapter(requireContext(), taskList)
        // 设置适配器
        spinner.adapter = adapter
        // 设置选择监听器
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 处理选择项的操作
                currentTask = position
                val selectedItem = taskList[position]
                // 在这里执行相应的操作
                closeInterpreters()
                loadPredictTask()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 当没有选项被选中时的处理
            }
        }

        val togglePredict = rootView.findViewById<ToggleButton>(R.id.togglePredict)
        togglePredict.isEnabled = false
        togglePredict.text = "Login First"

        togglePredict.setOnCheckedChangeListener { buttonView, isChecked ->
            isRecording = isChecked
            if (userModel.userName.value != null) {
                if (isChecked) {
                    Toast.makeText(requireContext(), "Start Record", Toast.LENGTH_SHORT).show()
                } else {
                    recordActivity(
                        userName = userModel.userName.value!!,
                        previousTime = previousTime!!,
                        currentActivityIndex = recordingActivityIndex!!,
                        task = currentTask
                    )
                    recordingActivityIndex = null
                    previousTime = null
                    Toast.makeText(requireContext(), "Record Finish", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please login First", Toast.LENGTH_SHORT).show()
            }

        }

        // 示例：获取 TextView 并设置文本
        val textView = rootView.findViewById<TextView>(R.id.predicted_activity)
        // Find the ImageView by its ID
        val imageView = rootView.findViewById<ImageView>(R.id.current_activity_image)

        // 设置点击事件监听器
        rootView.setOnClickListener {
            // 处理点击事件
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
                        recordActivity(
                            userName = userModel.userName.value!!,
                            previousTime = previousTime!!,
                            currentActivityIndex = recordingActivityIndex!!,
                            task = currentTask
                        )
                        recordingActivityIndex = newActivityIndex
                        previousTime = Date()
                    }
                }
            }
        }


        currentSymptomIndexLiveData.observe(viewLifecycleOwner) { newSymptomIndex ->
            if (newSymptomIndex in arrayOf(1,2)) {
                val redColor = ContextCompat.getColor(requireContext(), R.color.red)
                textView.setTextColor(redColor)
            } else {
                val blackColor = ContextCompat.getColor(requireContext(), R.color.black)
                textView.setTextColor(blackColor)
            }

        }

        handlerThread = HandlerThread("InterpreterThread")
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        handler.post {
            // TODO
            while (true) {
                println("BackupPredict")
                val currentActivityIndex = predictTask()
                currentActivityIndexLiveData.postValue(currentActivityIndex)
                runOnUiThread {
                    textView.text = "$currentActivity"
    //                         Set the new image resource
                    imageView.setImageResource(currentActivityImage)
                }

                Thread.sleep(1000) // 1000 毫秒，可以根据需求进行调整
            }
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

                    updateBuffer(liveData)
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

    private fun closeInterpreters() {
        interpreter11?.close()
        interpreter11 = null
        interpreter12?.close()
        interpreter12 = null
        interpreter13?.close()
        interpreter13 = null
        interpreter21?.close()
        interpreter21 = null
        interpreter22?.close()
        interpreter22 = null
        interpreter23?.close()
        interpreter23 = null
        interpreter24?.close()
        interpreter24 = null
        interpreter25?.close()
        interpreter25 = null
        interpreter26?.close()
        interpreter26 = null
        interpreter31?.close()
        interpreter31 = null
        interpreter32?.close()
        interpreter32 = null
        interpreter33?.close()
        interpreter33 = null
        interpreter34?.close()
        interpreter34 = null
        interpreter35?.close()
        interpreter35 = null
        interpreter36?.close()
        interpreter36 = null
    }

    private fun loadPredictTask() {
        if (currentTask == 0) {
            // 加载模型和其他初始化操作 for task1
            var modelByteBuffer = loadModelFile("StationaryNonStationaryBinaryModelLite.tflite")
            interpreter11 = Interpreter(modelByteBuffer)
            modelByteBuffer = loadModelFile("StationaryModelLite.tflite")
            interpreter12 = Interpreter(modelByteBuffer)
            modelByteBuffer = loadModelFile("NonStationaryModelLite.tflite")
            interpreter13 = Interpreter(modelByteBuffer)
        } else if (currentTask == 1) {
            // 加载模型和其他初始化操作 for task2
            var modelByteBuffer = loadModelFile("Task2OnlineActivityLite.tflite")
            interpreter21 = Interpreter(modelByteBuffer)
            modelByteBuffer = loadModelFile("Task2OnlineSittingLite-2.tflite")
            interpreter22 = Interpreter(modelByteBuffer)
            modelByteBuffer = loadModelFile("Task2OnlineLeftLite-2.tflite")
            interpreter23 = Interpreter(modelByteBuffer)
            modelByteBuffer = loadModelFile("Task2OnlineRightLite-2.tflite")
            interpreter24 = Interpreter(modelByteBuffer)
            modelByteBuffer = loadModelFile("Task2OnlineBackLite.tflite")

            interpreter25 = Interpreter(modelByteBuffer)
            modelByteBuffer = loadModelFile("Task2OnlineStomachLite-2.tflite")
            interpreter26 = Interpreter(modelByteBuffer)
        } else {
            // 加载模型和其他初始化操作 for task2
            var modelByteBuffer = loadModelFile("Task3OnlineActivityLite.tflite")
            interpreter31 = Interpreter(modelByteBuffer)
            modelByteBuffer = loadModelFile("Task3OnlineSittingLite.tflite")
            interpreter32 = Interpreter(modelByteBuffer)
            modelByteBuffer = loadModelFile("Task3OnlineLeftLite.tflite")
            interpreter33 = Interpreter(modelByteBuffer)
            modelByteBuffer = loadModelFile("Task3OnlineRightLite.tflite")
            interpreter34 = Interpreter(modelByteBuffer)
            modelByteBuffer = loadModelFile("Task3OnlineBackLite.tflite")
            interpreter35 = Interpreter(modelByteBuffer)
            modelByteBuffer = loadModelFile("Task3OnlineStomachLite.tflite")
            interpreter36 = Interpreter(modelByteBuffer)
        }
    }

    private fun predictTask(): Int {
        var currentActivityIndex = 0
        if (currentTask == 0) {
            val isStation = predictStationOrNonstation()
            currentActivityIndex = if (isStation == 0) {
                predictStation()
            } else {
                val fftData = getFFTData()
                predictNonstation(fftData)
            }
            currentSymptomIndexLiveData.postValue(0)
            currentActivity = task1Map[currentActivityIndex]!!
            currentActivityImage = taskImgMap[currentActivityIndex]!!
        } else if (currentTask == 1) {
            val fftData = getFFTData()
            val activityIndex = predictTask2Activity(fftData)
            val currentSymptomIndex = predictTask2Symptom(fftData, activityIndex)
            currentSymptomIndexLiveData.postValue(currentSymptomIndex)
            currentActivity = "${task2ActivityMap[activityIndex]!!} + ${task2SymptomMap[currentSymptomIndex]!!}"
            currentActivityImage = taskImgMap[activityIndex]!!
            currentActivityIndex = activityIndex + currentSymptomIndex*5
        } else {
            val fftData = getFFTData()
            val activityIndex = predictTask3Activity(fftData)
            val currentSymptomIndex = predictTask3Symptom(fftData, activityIndex)
            currentSymptomIndexLiveData.postValue(currentSymptomIndex)
            currentActivity = "${task2ActivityMap[activityIndex]!!} + ${task2SymptomMap[currentSymptomIndex]!!}"
            currentActivityImage = taskImgMap[activityIndex]!!
            currentActivityIndex = activityIndex + currentSymptomIndex*5
        }
        return currentActivityIndex
    }

    private fun recordActivity(userName: String, previousTime: Date,currentActivityIndex: Int, task: Int) {
        // Example: Insert data into the database
        currentTime = Date()
        val duration = calDurationInSeconds(previousTime, currentTime)
        // adjust granularity
        if (duration >= 3) {
            val entity = Records(
                userName = userName,
                date = currentDate,
                task = task,
                activity = currentActivityIndex,
                duration = calDurationInSeconds(previousTime, currentTime)
            )
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

    private fun updateBuffer(liveData: RESpeckLiveData) {
        // get all relevant intent contents
        val a_x = liveData.accelX
        val a_y = liveData.accelY
        val a_z = liveData.accelZ
        val g_x = liveData.gyro.x
        val g_y = liveData.gyro.y
        val g_z = liveData.gyro.z

        rawInputDataBuff[0] = rawInputDataBuff[0].drop(6).toFloatArray() // drop first 6 elements (oldest reading)
        rawInputDataBuff[0] = rawInputDataBuff[0] + a_x + a_y + a_z + g_x + g_y + g_z

        accXInputDataBuff = accXInputDataBuff.drop(1).plus(a_x.toDouble()).toTypedArray()
        accYInputDataBuff = accYInputDataBuff.drop(1).plus(a_y.toDouble()).toTypedArray()
        accZInputDataBuff = accZInputDataBuff.drop(1).plus(a_z.toDouble()).toTypedArray()
        gyroXInputDataBuff = gyroXInputDataBuff.drop(1).plus(g_x.toDouble()).toTypedArray()
        gyroYInputDataBuff = gyroYInputDataBuff.drop(1).plus(g_y.toDouble()).toTypedArray()
        gyroZInputDataBuff = gyroZInputDataBuff.drop(1).plus(g_z.toDouble()).toTypedArray()
    }

    private fun predictStationOrNonstation(): Int {
        interpreter11?.run(rawInputDataBuff, binaryOutputDataBuff)
        return getMaxIndex(binaryOutputDataBuff)
    }

    private fun predictStation(): Int {
        interpreter12?.run(rawInputDataBuff, stationOutputDataBuff)
        return getMaxIndex(stationOutputDataBuff)
    }

    private fun predictNonstation(fftData: FloatArray): Int {
        val inputDataBuffer = arrayOf(rawInputDataBuff[0] + fftData)
        interpreter13?.run(inputDataBuffer, nonStationOutputDataBuff)
        return getMaxIndex(nonStationOutputDataBuff) + 5
    }

    private fun predictTask2Activity(fftData: FloatArray): Int {
        val inputDataBuffer = arrayOf(rawInputDataBuff[0] + fftData)
        interpreter21?.run(inputDataBuffer, task2ActivityOutputDataBuff)
        return getMaxIndex(task2ActivityOutputDataBuff)
    }

    private fun predictTask2Symptom(fftData: FloatArray, currentActivityIndex: Int): Int {
        val inputDataBuffer = arrayOf(rawInputDataBuff[0] + fftData)
        when (currentActivityIndex) {
            0 -> interpreter22?.run(inputDataBuffer, task2SymptomOutputDataBuff)
            1 -> interpreter23?.run(inputDataBuffer, task2SymptomOutputDataBuff)
            2 -> interpreter24?.run(inputDataBuffer, task2SymptomOutputDataBuff)
            3 -> interpreter25?.run(inputDataBuffer, task2SymptomOutputDataBuff)
            4 -> interpreter26?.run(inputDataBuffer, task2SymptomOutputDataBuff)
        }
        return getMaxIndex(task2SymptomOutputDataBuff)
    }

    private fun predictTask3Activity(fftData: FloatArray): Int {
        val inputDataBuffer = arrayOf(rawInputDataBuff[0] + fftData)
        interpreter31?.run(inputDataBuffer, task3ActivityOutputDataBuff)
        return getMaxIndex(task3ActivityOutputDataBuff)
    }

    private fun predictTask3Symptom(fftData: FloatArray, currentActivityIndex: Int): Int {
        val inputDataBuffer = arrayOf(rawInputDataBuff[0] + fftData)
        when (currentActivityIndex) {
            0 -> interpreter32?.run(inputDataBuffer, task3SymptomOutputDataBuff)
            1 -> interpreter33?.run(inputDataBuffer, task3SymptomOutputDataBuff)
            2 -> interpreter34?.run(inputDataBuffer, task3SymptomOutputDataBuff)
            3 -> interpreter35?.run(inputDataBuffer, task3SymptomOutputDataBuff)
            4 -> interpreter36?.run(inputDataBuffer, task3SymptomOutputDataBuff)
        }
        return getMaxIndex(task3SymptomOutputDataBuff)
    }

    private fun getMaxIndex(outputDataBuff: Array<FloatArray>): Int {
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

    private fun getFFTData(): FloatArray {
        return rfft(accXInputDataBuff) + rfft(accYInputDataBuff) + rfft(accZInputDataBuff) + rfft(gyroXInputDataBuff) + rfft(gyroYInputDataBuff) + rfft(gyroZInputDataBuff)
    }

    private fun hanningWindow(inputSignal: Array<Double>): DoubleArray {
        val result = DoubleArray(inputSignal.size + 2) { 0.0 }
        val hanningWindow = doubleArrayOf(0.0, 0.00410499, 0.01635257, 0.03654162, 0.06434065,
            0.09929319, 0.14082532, 0.1882551 , 0.24080372, 0.29760833,
            0.35773621, 0.42020005, 0.48397421, 0.54801151, 0.61126047,
            0.67268253, 0.73126915, 0.78605833, 0.83615045, 0.88072298,
            0.91904405, 0.95048443, 0.97452787, 0.99077958, 0.9989727 ,
            0.9989727 , 0.99077958, 0.97452787, 0.95048443, 0.91904405,
            0.88072298, 0.83615045, 0.78605833, 0.73126915, 0.67268253,
            0.61126047, 0.54801151, 0.48397421, 0.42020005, 0.35773621,
            0.29760833, 0.24080372, 0.1882551 , 0.14082532, 0.09929319,
            0.06434065, 0.03654162, 0.01635257, 0.00410499, 0.0)
        for (i in 0 until 50) {
            result[i] =
                (inputSignal[i] * hanningWindow[i]).toDouble()
        }
        return result
    }

    private fun rfft(inputSignal: Array<Double>): FloatArray {
        val fftOutput = FloatArray(26) { 0f }

        val newInputSignal = hanningWindow(inputSignal)

        // 创建一个 DoubleFFT_1D 对象，传入输入信号的大小（作为 long 类型）
        val fft = DoubleFFT_1D(newInputSignal.size.toLong())

        // 执行 FFT 变换
        fft.realForward(newInputSignal)

        // 计算振幅谱

        for (i in 0 until newInputSignal.size / 2) {
            val realPart = newInputSignal[2 * i]
            val imaginaryPart = newInputSignal[2 * i + 1]
//            fftOutput[i+1] = (sqrt(realPart * realPart + imaginaryPart * imaginaryPart) / (signalLength / 2.0)).toFloat()
            fftOutput[i] = (sqrt(realPart * realPart + imaginaryPart * imaginaryPart)).toFloat()
        }

        // 输出归一化的单边振幅谱
        return fftOutput
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(respeckLiveUpdateReceiver)
        closeInterpreters()
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(respeckLiveUpdateReceiver)
        looperRespeck.quit()
        handlerThread.quit()
    }
}
