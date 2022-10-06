package com.example.iot

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.IOException
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    //socket变量
    var socket: Socket? = null

    //判断连接标志位
    private var isConnect = false

    //连接次数设定
    private var i = 0

    //接收数据变量
    var data: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //连接服务器IP与端口
        val port = "2333"
        val ip = "192.168.3.126"

        //设置UI变量
        val button3: Button? = findViewById(R.id.button3)
        val button4: Button? = findViewById(R.id.button4)
        val button5: Button? = findViewById(R.id.button5)




        button3?.setOnClickListener {

            if (!isConnect) {
                thread {
                    initConnect(ip, port)

                }
                Thread.sleep(300)
            } else {
                Toast.makeText(this, "连接失败", Toast.LENGTH_SHORT).show()
            }

        }
        button4?.setOnClickListener {
            socket?.close()
            isConnect = false
        }
        button5?.setOnClickListener {
            val out = creatJson()
            if (isConnect) {
                thread {
                    sendMessage(out)
                }
            } else {
                Toast.makeText(this, "通信已断开", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //创建连接
    private fun initConnect(ip: String, port: String) {
        val port1 = port.toInt()
        if (i > 5) {
            i = 0
            return
        }
        try {
            socket = Socket(ip, port1)
            //设置连接超时限制
            socket!!.soTimeout = 10000
            //判断一下是否连上，避免NullPointException
            if (socket != null) {
                isConnect = true
            } else {
                initConnect(ip, port)
                i += 1
            }
            //循环监听是否有消息需要接收
            while (isConnect) {
                val text: TextView? = findViewById(R.id.textView)
                data = receiveMessage()
                if (data != null) { //判断服务器是否有数据发送
                    text?.text = this.data
                }
//                text?.text = info
            }

        } catch (e: Exception) {
            when (e) {
                is SocketTimeoutException -> {
                    Log.e("连接超时，重新连接", "dd")
                    e.printStackTrace()
                }
                is NoRouteToHostException -> {
                    Log.e("该地址不存在，请检查", "DD")
                    e.printStackTrace()
                }
                is ConnectException -> {
                    Log.e("连接异常或被拒绝，请检查", "DD")
                    e.printStackTrace()
                }
                else -> {
                    e.printStackTrace()
                    Log.e("连接结束", e.toString())
                }
            }
        }
    }

    //创建json数据
    private fun creatJson(): String {

        //燃气表参数
        val id = "1"
        val time = "202209202301"
        val voc = "3.0"
        val vol = "233"
        val key = "open"

        //json对象
        val jsout = JSONObject()

        jsout.put("id", id)
        jsout.put("time", time)
        jsout.put("voc", voc)
        jsout.put("vol", vol)
        jsout.put("key", key)

        return jsout.toString()

    }

    //向服务端发送信息
    private fun sendMessage(message: String?) {

        val dout = socket?.getOutputStream()     //获取输出流
        try {
            if (dout != null && message != null) {
                //判断输出流或者消息是否为空，为空的话会产生nullpoint错
                dout.write(
                    """${message}
    """.toByteArray(charset("utf-8"))           // 写入需要发送的数据到输出流对象中
                ) // 数据的结尾加上换行符才可让服务器端的readline()停止阻塞
                dout.flush() // 发送数据到服务端
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    //接收服务器消息
    private fun receiveMessage(): String? {
        val din = InputStreamReader(socket?.getInputStream(), "gb2312")//获取输入流
        var info: String? = data
        try {
            info = if (isConnect) {
                val inMessage = CharArray(1024)     //设置接受缓冲，避免接受数据过长占用过多内存
                val a = din.read(inMessage) //a存储返回消息的长度
                if (a <= -1) {
                    return null
                }
                String(inMessage, 0, a)
            } else {
                "检查连接情况"
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return info
    }
}