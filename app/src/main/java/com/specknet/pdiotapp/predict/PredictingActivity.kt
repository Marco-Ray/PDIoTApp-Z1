package com.specknet.pdiotapp.predict

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.specknet.pdiotapp.R


class PredictingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_predict)

        val textView = findViewById<TextView>(R.id.predicted_activity)

        // TODO -- model's output
        val activityCode = 1

        // TODO -- label map
        val activityMap = mapOf(
            0 to "Sitting",
            1 to "Standing",
            2 to "Lying",
        )

        val currentActivity = activityMap[activityCode]

        runOnUiThread {
            textView.text = "$currentActivity"
        }
    }
}
