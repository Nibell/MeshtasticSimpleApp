package com.example.meshtasticapp

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import java.util.*

class MeshtasticBleManager(context: android.content.Context) : BleManager(context) {

    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("6ba1b218-15a8-461f-9fa8-5dcae273eafd")
        val FROM_RADIO_UUID: UUID = UUID.fromString("2c55e69e-4993-11ed-b878-0242ac120002")
        val TO_RADIO_UUID: UUID = UUID.fromString("f75c76d2-129e-4dad-a1dd-7866124401e7")
        val NOTIFY_UUID: UUID = UUID.fromString("ed9da18c-a800-4f66-a670-aa7547e34453")
    }

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        val service = gatt.getService(SERVICE_UUID)
        if (service != null) {
            txCharacteristic = service.getCharacteristic(TO_RADIO_UUID)
            rxCharacteristic = service.getCharacteristic(FROM_RADIO_UUID)
        }
        return txCharacteristic != null && rxCharacteristic != null
    }

    override fun initialize() {
        setNotificationCallback(rxCharacteristic).with { _, data ->
            onDataReceived?.invoke(data)
        }
        enableNotifications(rxCharacteristic).enqueue()

        // Send WantConfig packet automatically after ready
        requestWantConfig()
    }

    override fun onServicesInvalidated() {
        txCharacteristic = null
        rxCharacteristic = null
    }

    var onDataReceived: ((Data) -> Unit)? = null

    fun sendMessage(message: String) {
        txCharacteristic?.let {
            writeCharacteristic(it, message.toByteArray()).enqueue()
        }
    }

    fun sendRaw(bytes: ByteArray) {
        txCharacteristic?.let {
            writeCharacteristic(it, bytes).enqueue()
        }
    }

    private fun requestWantConfig() {
        val wantConfigId = 0x04 // 4 = WantConfigId in Meshtastic protocol
        val payload = byteArrayOf(wantConfigId.toByte())

        sendRaw(payload)
    }
}
