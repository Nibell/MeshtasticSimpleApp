package com.example.meshtasticapp

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import com.example.meshtasticapp.protobufs.MeshtasticappProtobufs.MeshPacket
import com.example.meshtasticapp.protobufs.MeshtasticappProtobufs.MeshData
import java.util.*
import build.buf.gen.meshtastic.toRadio

class MeshtasticBleManager(context: Context) : BleManager(context) {

    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    var onDataReceived: ((Data) -> Unit)? = null
    var onStatusUpdate: ((String) -> Unit)? = null

    var myNodeId: Int? = null

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("6ba1b218-15a8-461f-9fa8-5dcae273eafd")
        val FROM_RADIO_UUID: UUID = UUID.fromString("2c55e69e-4993-11ed-b878-0242ac120002")
        val TO_RADIO_UUID: UUID = UUID.fromString("f75c76d2-129e-4dad-a1dd-7866124401e7")
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
            data.value?.let { bytes ->
                try {
                    val meshPacket = MeshPacket.parseFrom(bytes)
                    handleIncomingPacket(meshPacket)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        enableNotifications(rxCharacteristic).enqueue()

        requestWantNodeInfo()
    }

    private fun handleIncomingPacket(packet: MeshPacket) {
        if (packet.hasFrom()) {
            myNodeId = packet.from
            onStatusUpdate?.invoke("Connected to Node ID: ${myNodeId}")
        }
        onDataReceived?.invoke(Data(packet.toByteArray()))
    }

    override fun onServicesInvalidated() {
        txCharacteristic = null
        rxCharacteristic = null
    }

    fun sendMessage(message: String) {
        val packet = buildTextPacket(message)
        sendRaw(packet)
    }

    private fun buildTextPacket(text: String): ByteArray {
        val dataPayload = MeshData.newBuilder()
            .setText(text)
            .build()

        val fromId = myNodeId ?: 2053895156 // fallback if not yet detected

        val meshPacket = MeshPacket.newBuilder()
            .setFrom(fromId)
            .setTo(0xFFFFFFFF.toInt()) // broadcast
            .setChannel(0)
            .setPayload(dataPayload.toByteString())
            .build()

        return meshPacket.toByteArray()
    }

    private fun requestWantNodeInfo() {
        // Send special packet requesting Node Info
        val tr = toRadio {
            wantConfigId = 1
        }
        val payload = tr.toByteArray()
        sendRaw(payload)
    }

    fun sendRaw(bytes: ByteArray) {
        txCharacteristic?.let {
            writeCharacteristic(it, bytes).enqueue()
        }
    }
}
