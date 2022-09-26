package com.example.iot

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.IOException
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
        val text: TextView? = findViewById(R.id.textView)
        if (isConnect) {
            thread {
                try {
//                    var a: InputStream? = null
//                    var isr: InputStreamReader? = null
//                    var br: BufferedReader? = null
//                    a = socket?.getInputStream()
//                    isr = InputStreamReader(a)
//                    br = BufferedReader(isr)
//                    val data = br.readText()         //将文件内容转化为字符串，只适合小文件的读取，不适合大文件的读取
                    val data = "connected"
                    text?.text = data

                } catch (e: IOException) {
                    e.printStackTrace()
                }


            }
        }


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
    fun sendMessage(message: String?) {

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
}