package com.specknet.pdiotapp.predict

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.specknet.pdiotapp.R
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PredictingActivity : AppCompatActivity() {
    lateinit var interpreter: Interpreter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_predict)

        val textView = findViewById<TextView>(R.id.predicted_activity)

        val assetManager = this.assets
        val modelInputStream = assetManager.open("basicModel1DummyLite.tflite")
        val modelBuffer = modelInputStream.readBytes()

        // 使用直接 ByteBuffer，确保字节顺序为本地字节顺序
        val modelByteBuffer = ByteBuffer.allocateDirect(modelBuffer.size)
        modelByteBuffer.order(ByteOrder.nativeOrder())
        modelByteBuffer.put(modelBuffer)
        modelByteBuffer.rewind()

        val interpreter = Interpreter(modelByteBuffer)
        // Proceed with inference

        // 创建模拟的输入数据
        var inputData = Array(1) { FloatArray(75) }
        // 填充模拟数据，这里填充了一些示例数据
        for (i in 0 until 75) {
            inputData[0][i] = 1.toFloat() /* 设置你的模拟数据 */;
        }

        var outputData = Array(1) { FloatArray(12) }

        // 将输入数据传递给模型
        interpreter.run(inputData, outputData)

        // 获取模型的输出
        var maxIndex = 0 // 初始化最大值的索引为0
        var maxValue = outputData[0][0] // 初始化最大值为数组的第一个元素

        for (i in 1 until outputData[0].size) {
            if (outputData[0][i] > maxValue) {
                maxValue = outputData[0][i]
                maxIndex = i
            }
        }

        // TODO -- model's output
        val activityCode = maxIndex

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

        val currentActivity = activityMap[activityCode]

        runOnUiThread {
            textView.text = "$currentActivity"
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        interpreter.close()
    }
}
