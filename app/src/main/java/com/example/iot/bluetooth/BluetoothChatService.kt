package com.example.iot.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothChatService(val mAdapter: BluetoothAdapter) {
    private var mAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null
    private var mState: Int
    private var connectBluetoothListener: ConnectBluetoothListener?=null

    //构造方法，接收UI主线程传递的对象
    init {
        //构造方法完成蓝牙对象的创建
        mState = STATE_NONE
    }

    @get:Synchronized
    @set:Synchronized
    var state: Int
        get() = mState
        private set(state) {
            mState = state
        }

    @Synchronized
    fun start() {
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
        if (mAcceptThread == null) {
            mAcceptThread = AcceptThread()
            mAcceptThread!!.start()
        }
        state = STATE_LISTEN
    }

    //取消 CONNECTING 和 CONNECTED 状态下的相关线程，然后运行新的 mConnectThread 线程
    @Synchronized
    fun connect(device: BluetoothDevice,connectBluetoothListener: ConnectBluetoothListener) {
        this.connectBluetoothListener=connectBluetoothListener
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread!!.cancel()
                mConnectThread = null
            }
        }
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
        mConnectThread = ConnectThread(device)
        mConnectThread!!.start()
        state = STATE_CONNECTING
    }

    /*
        开启一个 ConnectedThread 来管理对应的当前连接。之前先取消任意现存的 mConnectThread 、
        mConnectedThread 、 mAcceptThread 线程，然后开启新 mConnectedThread ，传入当前刚刚接受的
        socket 连接。最后通过 Handler来通知UI连接
     */
    @Synchronized
    fun connected(socket: BluetoothSocket?, device: BluetoothDevice) {
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
        if (mAcceptThread != null) {
            mAcceptThread!!.cancel()
            mAcceptThread = null
        }
        mConnectedThread = ConnectedThread(socket)
        mConnectedThread!!.start()
        state = STATE_CONNECTED
    }

    // 停止所有相关线程，设当前状态为 NONE
    @Synchronized
    fun stop() {
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
        if (mAcceptThread != null) {
            mAcceptThread!!.cancel()
            mAcceptThread = null
        }
        state = STATE_NONE
        connectBluetoothListener?.changeStatus(0)
    }

    // 在 STATE_CONNECTED 状态下，调用 mConnectedThread 里的 write 方法，写入 byte
    fun write(out: ByteArray?) {
        var r: ConnectedThread?
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread
        }
        r!!.write(out)
    }

    // 连接失败的时候处理，通知 ui ，并设为 STATE_LISTEN 状态
    private fun connectionFailed() {
        state = STATE_LISTEN
        connectBluetoothListener?.changeStatus(0)
    }

    // 当连接失去的时候，设为 STATE_LISTEN 状态并通知 ui
    private fun connectionLost() {
        connectBluetoothListener?.changeStatus(0)
        state = STATE_LISTEN

    }

    // 创建监听线程，准备接受新连接。使用阻塞方式，调用 BluetoothServerSocket.accept()
    private inner class AcceptThread : Thread() {
        private val mmServerSocket: BluetoothServerSocket?

        init {
            var tmp: BluetoothServerSocket? = null
            try {
                //使用射频端口（RF comm）监听
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
            } catch (e: IOException) {
            }
            mmServerSocket = tmp
        }

        override fun run() {
            name = "AcceptThread"
            var socket: BluetoothSocket? = null
            while (mState != STATE_CONNECTED) {
                socket = try {
                    mmServerSocket!!.accept()
                } catch (e: IOException) {
                    break
                }
                if (socket != null) {
                    synchronized(this@BluetoothChatService) {
                        when (mState) {
                            STATE_LISTEN, STATE_CONNECTING -> connected(
                                socket,
                                socket.getRemoteDevice()
                            )
                            STATE_NONE, STATE_CONNECTED -> try {
                                socket.close()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }

        fun cancel() {
            try {
                mmServerSocket!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mmDevice: BluetoothDevice

        init {
            mmDevice = device
            var tmp: BluetoothSocket? = null
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mmSocket = tmp
        }

        override fun run() {
            name = "ConnectThread"
            try {
                mmSocket!!.connect()
                connectBluetoothListener?.changeStatus(1)
            } catch (e: IOException) {
                connectionFailed()
                try {
                    mmSocket!!.close()
                } catch (e2: IOException) {
                    e.printStackTrace()
                }
                return
            }
            synchronized(this@BluetoothChatService) { mConnectThread = null }
            connected(mmSocket, mmDevice)
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private inner class ConnectedThread(socket: BluetoothSocket?) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            mmSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = socket!!.inputStream
                tmpOut = socket!!.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = mmInStream!!.read(buffer)
                    connectBluetoothListener?.message(String(buffer.copyOf(bytes)))
                    Thread.sleep(7000)
                    connectBluetoothListener?.savejson(String(buffer.copyOf(bytes)))
                } catch (e: IOException) {
                    connectionLost()
                    break
                }
            }
        }

        fun write(buffer: ByteArray?) {
            try {
                mmOutStream!!.write(buffer)

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    interface ConnectBluetoothListener{
        fun changeStatus(state: Int)

        fun message(msg:String)

        fun savejson(msg: String)
    }

    companion object {
        //本应用的主Activity组件名称
        private const val NAME = "BluetoothChat"

        // UUID：通用唯一识别码,是一个128位长的数字，一般用十六进制表示
        //算法的核心思想是结合机器的网卡、当地时间、一个随机数来生成
        //在创建蓝牙连接
        private val MY_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        const val STATE_NONE = 0
        const val STATE_LISTEN = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECTED = 3
    }
}