package com.specknet.pdiotapp.predict

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.widget.TextView
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
lateinit var looperRespeck: Looper

val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)


class PredictingActivity : AppCompatActivity() {
    private lateinit var interpreter: Interpreter
    private var inputDataBuff = Array(1){ FloatArray(75){0f} }
    private var outputDataBuff = Array(1) { FloatArray(12){0f} }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_predict)

        val textView = findViewById<TextView>(R.id.predicted_activity)

        val modelByteBuffer = loadModelFile("basicModel1DummyLite.tflite")

        interpreter = Interpreter(modelByteBuffer)

        // 获取 SharedPreferences 对象
        val sharedPreferences = this.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // 从 SharedPreferences 中读取用户名
        val savedUsername = sharedPreferences.getString("username", null)
        val userName = findViewById<TextView>(R.id.userName)

        if (savedUsername != null) {
            // 读取到了保存的用户名
            // 在这里使用 savedUsername，例如显示在 UI 上
            userName.text = savedUsername
        } else {
            // SharedPreferences 中没有保存的用户名
            // 你可以执行适当的操作，例如提示用户输入用户名
            userName.text = "Anonymous"
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

                    val currentActivity = predict(x,y,z)
                    Log.d("Live", "Predicted " + currentActivity + "for" + liveData)
                    runOnUiThread {
                        textView.text = "$currentActivity"
                    }
                }
            }
        }

        // register receiver on another thread
        val handlerThreadRespeck = HandlerThread("bgThreadRespeckPredictLive")
        handlerThreadRespeck.start()
        looperRespeck = handlerThreadRespeck.looper
        val handlerRespeck = Handler(looperRespeck)
        this.registerReceiver(respeckLiveUpdateReceiver, filterTestRespeck, null, handlerRespeck)
    }


    private fun loadModelFile(path2Model: String): ByteBuffer {
        val assetManager = this.assets
        val modelInputStream = assetManager.open(path2Model)
        val modelBuffer = modelInputStream.readBytes()

        val modelByteBuffer = ByteBuffer.allocateDirect(modelBuffer.size)
        modelByteBuffer.order(ByteOrder.nativeOrder())
        modelByteBuffer.put(modelBuffer)
        modelByteBuffer.rewind()
        return modelByteBuffer
    }

    private fun predict(x: Float, y: Float, z: Float): String? {
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

        return activityMap[maxIndex]
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckLiveUpdateReceiver)
        looperRespeck.quit()
        interpreter.close()
    }
}
