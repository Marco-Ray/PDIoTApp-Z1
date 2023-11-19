package com.specknet.pdiotapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import com.specknet.pdiotapp.bluetooth.BluetoothSpeckService
import com.specknet.pdiotapp.database.RecordDatabase
import com.specknet.pdiotapp.live.LiveDataFragment
import com.specknet.pdiotapp.onboarding.OnBoardingActivity
import com.specknet.pdiotapp.predict.PredictFragment
import com.specknet.pdiotapp.utils.BLEStatusViewModel
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.UserInfoViewModel
import com.specknet.pdiotapp.utils.Utils
import kotlinx.android.synthetic.main.activity_main.bottomNav
import kotlinx.android.synthetic.main.activity_main.coordinatorLayout
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // permissions
    lateinit var permissionAlertDialog: AlertDialog.Builder

    val permissionsForRequest = arrayListOf<String>()

    var locationPermissionGranted = false
    var cameraPermissionGranted = false
    var readStoragePermissionGranted = false
    var writeStoragePermissionGranted = false

    // broadcast receiver
    val filter = IntentFilter()

    var isUserFirstTime = false

    // buttons and textviews
    lateinit var loginButton: Button
    lateinit var signupButton: Button

    // save userInfo
    val globalBundle = Bundle()

    private lateinit var respeckConnectionReceiver: BroadcastReceiver
    private lateinit var thingyConnectionReceiver: BroadcastReceiver
    private lateinit var bleStatusViewModel: BLEStatusViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadFragment(HomeFragment())
        LoginFragment().arguments = globalBundle

        val newLocale = Locale("en", "UK")
        Locale.setDefault(newLocale)

        database = Room.databaseBuilder(
            this,
            RecordDatabase::class.java,
            "RecordDatabase"
        ).build()

        // check whether the onboarding screen should be shown
        val sharedPreferences = getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)
        if (sharedPreferences.contains(Constants.PREF_USER_FIRST_TIME)) {
            isUserFirstTime = false
        }
        else {
            isUserFirstTime = true
            sharedPreferences.edit().putBoolean(Constants.PREF_USER_FIRST_TIME, false).apply()
            val introIntent = Intent(this, OnBoardingActivity::class.java)
            startActivity(introIntent)
        }

        // 获取 ViewModel
        val userModel = ViewModelProvider(this).get(UserInfoViewModel::class.java)

        bottomNav.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.connect -> {
                    loadFragment(LiveDataFragment())
                    true
                }
                R.id.predict -> {
                    loadFragment(PredictFragment())
                    true
                }
                R.id.history -> {
                    if (userModel.userName.value != null) {
                        loadFragment(HistoryFragment())
                        true
                    } else {
                        showToast("Please login first.")
                        loadFragment(AccountFragment())
                        false
                    }
                }
                R.id.account -> {
                    loadFragment(AccountFragment())
                    true
                }

            }
            true
        }

        permissionAlertDialog = AlertDialog.Builder(this)


//        setupClickListeners()

        setupPermissions()

        setupBluetoothService()

        // register a broadcast receiver for respeck status
        filter.addAction(Constants.ACTION_RESPECK_CONNECTED)
        filter.addAction(Constants.ACTION_RESPECK_DISCONNECTED)


        bleStatusViewModel = ViewModelProvider(this).get(BLEStatusViewModel::class.java)

        respeckConnectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Constants.ACTION_RESPECK_CONNECTION_STATUS) {
                    val isConnected =
                        intent.getBooleanExtra(Constants.EXTRA_BLUETOOTH_CONNECTED, false)
                    // 根据连接状态执行相应操作
                    println("RESpeck status change: $isConnected")
                    bleStatusViewModel.updateREspeckStatus(isConnected)
                }
            }
        }

        thingyConnectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Constants.ACTION_THINGY_CONNECTION_STATUS) {
                    val isConnected =
                        intent.getBooleanExtra(Constants.EXTRA_BLUETOOTH_CONNECTED, false)
                    // 根据连接状态执行相应操作
                    bleStatusViewModel.updateThingyStatus(isConnected)
                }
            }
        }

    }

//    private fun setupClickListeners() {
//        liveProcessingButton.setOnClickListener {
//            val intent = Intent(this, LiveDataActivity::class.java)
//            startActivity(intent)
//        }
//
//        pairingButton.setOnClickListener {
//            val intent = Intent(this, ConnectingActivity::class.java)
//            startActivity(intent)
//        }
//
//        recordButton.setOnClickListener {
//            val intent = Intent(this, RecordingActivity::class.java)
//            startActivity(intent)
//        }
//
//        predictButton.setOnClickListener {
//            val intent = Intent(this, PredictingActivity::class.java)
//            startActivity(intent)
//        }
//    }

    private fun loadFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = this.supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.addToBackStack(null) // 可选，用于将事务添加到返回栈
        transaction.commit()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupPermissions() {
        // request permissions

        // location permission
        Log.i("Permissions", "Location permission = " + locationPermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsForRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsForRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        else {
            locationPermissionGranted = true
        }

        // camera permission
        Log.i("Permissions", "Camera permission = " + cameraPermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permissions", "Camera permission = " + cameraPermissionGranted)
            permissionsForRequest.add(Manifest.permission.CAMERA)
        }
        else {
            cameraPermissionGranted = true
        }

        // read storage permission
        Log.i("Permissions", "Read st permission = " + readStoragePermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permissions", "Read st permission = " + readStoragePermissionGranted)
            permissionsForRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        else {
            readStoragePermissionGranted = true
        }

        // write storage permission
        Log.i("Permissions", "Write storage permission = " + writeStoragePermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permissions", "Write storage permission = " + writeStoragePermissionGranted)
            permissionsForRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        else {
            writeStoragePermissionGranted = true
        }

        if (permissionsForRequest.size >= 1) {
            ActivityCompat.requestPermissions(this,
                permissionsForRequest.toTypedArray(),
                Constants.REQUEST_CODE_PERMISSIONS)
        }

    }

    fun setupBluetoothService() {
        val isServiceRunning = Utils.isServiceRunning(BluetoothSpeckService::class.java, applicationContext)
        Log.i("debug","isServiceRunning = " + isServiceRunning)

        // check sharedPreferences for an existing Respeck id
        val sharedPreferences = getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)
        if (sharedPreferences.contains(Constants.RESPECK_MAC_ADDRESS_PREF)) {
            Log.i("sharedpref", "Already saw a respeckID, starting service and attempting to reconnect")

            // launch service to reconnect
            // start the bluetooth service if it's not already running
            if(!isServiceRunning) {
                Log.i("service", "Starting BLT service")
                val simpleIntent = Intent(this, BluetoothSpeckService::class.java)
                this.startService(simpleIntent)
            }
        }
        else {
            Log.i("sharedpref", "No Respeck seen before, must pair first")
            // TODO then start the service from the connection activity
        }
    }

    override fun onResume() {
        super.onResume()
        val intentFilter1 = IntentFilter(Constants.ACTION_RESPECK_CONNECTION_STATUS)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(respeckConnectionReceiver, intentFilter1)

        val intentFilter2 = IntentFilter(Constants.ACTION_THINGY_CONNECTION_STATUS)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(respeckConnectionReceiver, intentFilter2)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 在 onPause 中取消注册广播接收器，以避免内存泄漏
        LocalBroadcastManager.getInstance(this).unregisterReceiver(respeckConnectionReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(thingyConnectionReceiver)
        System.exit(0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            if(grantResults.isNotEmpty()) {
                for (i in grantResults.indices) {
                    when(permissionsForRequest[i]) {
                        Manifest.permission.ACCESS_COARSE_LOCATION -> locationPermissionGranted = true
                        Manifest.permission.ACCESS_FINE_LOCATION -> locationPermissionGranted = true
                        Manifest.permission.CAMERA -> cameraPermissionGranted = true
                        Manifest.permission.READ_EXTERNAL_STORAGE -> readStoragePermissionGranted = true
                        Manifest.permission.WRITE_EXTERNAL_STORAGE -> writeStoragePermissionGranted = true
                    }

                }
            }
        }

        // count how many permissions need granting
        var numberOfPermissionsUngranted = 0
        if (!locationPermissionGranted) numberOfPermissionsUngranted++
        if (!cameraPermissionGranted) numberOfPermissionsUngranted++
        if (!readStoragePermissionGranted) numberOfPermissionsUngranted++
        if (!writeStoragePermissionGranted) numberOfPermissionsUngranted++

        // show a general message if we need multiple permissions
        if (numberOfPermissionsUngranted > 1) {
            val generalSnackbar = Snackbar
                .make(coordinatorLayout, "Several permissions are needed for correct app functioning", Snackbar.LENGTH_LONG)
                .setAction("SETTINGS") {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
                .show()
        }
        else if(numberOfPermissionsUngranted == 1) {
            var snackbar: Snackbar = Snackbar.make(coordinatorLayout, "", Snackbar.LENGTH_LONG)
            if (!locationPermissionGranted) {
                snackbar = Snackbar
                    .make(
                        coordinatorLayout,
                        "Location permission needed for Bluetooth to work.",
                        Snackbar.LENGTH_LONG
                    )
            }

            if(!cameraPermissionGranted) {
                snackbar = Snackbar
                    .make(
                        coordinatorLayout,
                        "Camera permission needed for QR code scanning to work.",
                        Snackbar.LENGTH_LONG
                    )
            }

            if(!readStoragePermissionGranted || !writeStoragePermissionGranted) {
                snackbar = Snackbar
                    .make(
                        coordinatorLayout,
                        "Storage permission needed to record sensor.",
                        Snackbar.LENGTH_LONG
                    )
            }

            snackbar.setAction("SETTINGS") {
                val settingsIntent = Intent(Settings.ACTION_SETTINGS)
                startActivity(settingsIntent)
            }
                .show()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.show_tutorial) {
            val introIntent = Intent(this, OnBoardingActivity::class.java)
            startActivity(introIntent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        lateinit var database: RecordDatabase
            private set
    }
}