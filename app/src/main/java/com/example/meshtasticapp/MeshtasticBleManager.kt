package com.example.meshtasticapp

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import build.buf.gen.meshtastic.toRadio
import com.example.meshtasticapp.protobufs.MeshtasticappProtobufs.Decoded
import com.example.meshtasticapp.protobufs.MeshtasticappProtobufs.MeshData
import com.example.meshtasticapp.protobufs.MeshtasticappProtobufs.MeshPacket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import java.util.*


class MeshtasticBleManager(context: Context) : BleManager(context) {

    private val bleCharacteristics = mutableMapOf<String, BluetoothGattCharacteristic?>()

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
            bleCharacteristics[TO_RADIO_UUID.toString()] = service.getCharacteristic(TO_RADIO_UUID)
            bleCharacteristics[FROM_RADIO_UUID.toString()] = service.getCharacteristic(FROM_RADIO_UUID)
        }
        return bleCharacteristics[TO_RADIO_UUID.toString()] != null && bleCharacteristics[FROM_RADIO_UUID.toString()] != null
    }

    override fun initialize() {
        val rxCharacteristic = bleCharacteristics[FROM_RADIO_UUID.toString()]
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
        if (packet.from != 0) {
            myNodeId = packet.from
            onStatusUpdate?.invoke("Connected to Node ID: ${myNodeId}")
        }
        onDataReceived?.invoke(Data(packet.toByteArray()))
    }

    override fun onServicesInvalidated() {
        bleCharacteristics.clear()
    }

    // Send normal text message
    fun sendTextMessage(message: String) {
        val packet = buildTextPacket(message)
        sendRaw(packet)
    }

    private fun buildTextPacket(text: String): ByteArray {
        val dataPayload = MeshData.newBuilder()
            .setText(text)
            .build()

        val decoded = Decoded.newBuilder()
            .setPortnum(Decoded.PortNum.TEXT_MESSAGE_APP)  // Set portnum for TEXT message
            .setPayload(dataPayload.toByteString())
            .build()

        val fromId = myNodeId ?: 1 // fallback to 1 if myNodeId is null

        val meshPacket = MeshPacket.newBuilder()
            .setFrom(fromId)
            .setTo(0xFFFFFFFF.toInt()) // broadcast to all nodes
            .setChannel(0)
            .setWantAck(true)
            .setDecoded(decoded)
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
        bleCharacteristics[TO_RADIO_UUID.toString()]?.let {
            writeCharacteristic(it, bytes).enqueue()
        }
    }
}
