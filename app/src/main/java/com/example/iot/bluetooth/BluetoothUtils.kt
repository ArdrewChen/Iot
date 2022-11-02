package com.example.iot.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

class BluetoothUtils private constructor() {
    val TAG = javaClass.name
    var context: Context? = null
    var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothInterface: BluetoothInterface? = null
    private var dev_mac_adress = ""
    private var bluetoothManager: BluetoothManager? = null
    var deviceBeans: ArrayList<BluetoothDevice> = ArrayList()
    fun initBluetooth(context: Context) {
        if (this.context != null) {
            return
        }
        this.context = context
        bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager!!.adapter

        registerBroadcas(context)
    }

    fun setBluetoothListener(bluetoothInterface: BluetoothInterface?) {
        this.bluetoothInterface = bluetoothInterface
    }

    private fun registerBroadcas(context: Context) {
        val intent = IntentFilter()
        intent.addAction(BluetoothDevice.ACTION_FOUND)
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//状态改变
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);//行动扫描模式改变了
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//动作状态发生了变化
        context.registerReceiver(bluetoothBroadcast, intent)
        Log.i(TAG, "registerReceiver")
    }

    var bluetoothBroadcast: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.e(TAG, "onReceive: ${intent.action}")
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> { //搜索结果
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    if (device.name != null && !dev_mac_adress.contains(device.address)) {
                        deviceBeans.add(device)
                        dev_mac_adress += device.address
                        Log.i(TAG, device.name + "：" + device.address)
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> { // 搜索完成
                    dev_mac_adress = ""
                    bluetoothInterface!!.getBluetoothList(deviceBeans)
                    bluetoothInterface?.getBluetoothBondList(bluetoothAdapter!!.bondedDevices)
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> { // 配对成功，或者取消配对
                    bluetoothInterface?.getBluetoothBondList(bluetoothAdapter!!.bondedDevices)
                    cancelDiscovery()
                    startDiscovery()
                    Log.e(TAG, "onReceive: 绑定列表有变，开始重新搜索", )
                }
            }
        }
    }

    /** 开启蓝牙  */
    fun enable() {
        if (bluetoothAdapter != null && !bluetoothAdapter!!.isEnabled) {
            bluetoothAdapter!!.enable()
        }
    }

    /** 关闭蓝牙  */
    fun disable() {
        if (bluetoothAdapter != null && bluetoothAdapter!!.isEnabled) {
            bluetoothAdapter!!.disable()
        }
    }

    /** 取消搜索  */
    fun cancelDiscovery() {
        if (isDiscovering) {
            bluetoothAdapter!!.cancelDiscovery()
        }
    }

    /** 开始搜索  */
    fun startDiscovery() {
        bluetoothInterface?.getBluetoothBondList(bluetoothAdapter!!.bondedDevices)
        if (bluetoothAdapter != null && bluetoothAdapter!!.isEnabled) {
            deviceBeans.clear()
            bluetoothAdapter!!.startDiscovery()
        }
    }

    /** 判断蓝牙是否打开  */
    val isEnabled: Boolean
        get() = if (bluetoothAdapter != null) {
            bluetoothAdapter!!.isEnabled
        } else false

    /** 判断当前是否正在查找设备，是返回true  */
    val isDiscovering: Boolean
        get() = if (bluetoothAdapter != null) {
            bluetoothAdapter!!.isDiscovering
        } else false

    fun onDestroy() {
        context!!.unregisterReceiver(bluetoothBroadcast)
    }

    interface BluetoothInterface {
        /* 获取蓝牙列表 */
        fun getBluetoothList(deviceBeans: ArrayList<BluetoothDevice>)

        fun getBluetoothBondList(deviceBeans: Set<BluetoothDevice>)
    }

    companion object {  // 单例模式
        private var bluetoothInstance: BluetoothUtils? = null
        val instance: BluetoothUtils
            get() {
                if (bluetoothInstance == null) {
                    bluetoothInstance = BluetoothUtils()
                }
                return bluetoothInstance!!
            }
    }
}