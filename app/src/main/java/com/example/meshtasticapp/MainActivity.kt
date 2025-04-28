package com.example.meshtasticapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelUuid
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import no.nordicsemi.android.support.v18.scanner.*

class MainActivity : AppCompatActivity() {

    private lateinit var bleManager: MeshtasticBleManager
    private var connectedDevice: BluetoothDevice? = null

    private lateinit var connectButton: Button
    private lateinit var sendButton: Button
    private lateinit var messageEditText: EditText
    private lateinit var receiveTextView: TextView
    private lateinit var statusTextView: TextView

    private val scanner by lazy {
        BluetoothLeScannerCompat.getScanner()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            scanner.stopScan(this)
            connectToDevice(result.device)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Toast.makeText(this@MainActivity, "Scan failed: $errorCode", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectButton = findViewById(R.id.connectButton)
        sendButton = findViewById(R.id.sendButton)
        messageEditText = findViewById(R.id.messageEditText)
        receiveTextView = findViewById(R.id.receiveTextView)
        statusTextView = findViewById(R.id.statusTextView)

        bleManager = MeshtasticBleManager(this)

        connectButton.setOnClickListener { startScan() }

        sendButton.setOnClickListener {
            val message = messageEditText.text.toString()
            if (connectedDevice != null) {
                bleManager.sendTextMessage(message)
            } else {
                Toast.makeText(this, "Not connected!", Toast.LENGTH_SHORT).show()
            }
        }

        bleManager.onStatusUpdate = { status ->
            runOnUiThread {
                statusTextView.text = status
            }
        }

        bleManager.onDataReceived = { data ->
            runOnUiThread {
                val rawText = data.getStringValue(0)
                receiveTextView.append("\nReceived: $rawText")
            }
        }
    }

    private fun startScan() {
        if (!hasPermissions()) {
            requestPermissions()
            return
        }

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            Toast.makeText(this, "Bluetooth is off!", Toast.LENGTH_SHORT).show()
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(MeshtasticBleManager.SERVICE_UUID))
                .build()
        )

        scanner.startScan(filters, settings, scanCallback)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        connectedDevice = device

        bleManager.connect(device)
            .retry(3, 100)
            .useAutoConnect(false)
            .done {
                runOnUiThread {
                    Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show()
                }
            }
            .fail { _, status ->
                runOnUiThread {
                    Toast.makeText(this, "Failed: $status", Toast.LENGTH_SHORT).show()
                }
            }
            .enqueue()
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_CODE_PERMISSIONS
        )
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1
    }
}
