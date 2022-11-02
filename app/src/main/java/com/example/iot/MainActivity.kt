package com.example.iot

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.iot.bluetooth.BluetoothActivity
import org.json.JSONObject
import java.io.*
import java.net.*
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

    //创建json数据
    var json = JSONObject()
    var jsout = JSONObject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //连接服务器IP与端口
        val port = "8888"
        val ip = "192.168.3.126"
//        val ip = "192.168.31.62"
        //设置UI变量
//        val button2: Button? = findViewById(R.id.button2)
        val button3: Button? = findViewById(R.id.button3)
        val button4: Button? = findViewById(R.id.button4)
        val button5: Button? = findViewById(R.id.button5)
        val button: Button? = findViewById(R.id.button)


        if (!isConnect) {
            thread {
                initConnect(ip, port)
//                val statue = "download"
//                jsout.put("type", "connecting")
//                jsout.put("equipmentId", 836039386)
//                jsout.put("job", statue)
//                val answer = jsout.toString() + "\n"
//                sendMessage(answer)  //可能导致解码失败

            }
            Thread.sleep(300)
        } else {
            Toast.makeText(this, "连接失败", Toast.LENGTH_SHORT).show()
        }


        button?.setOnClickListener {
            val out = creatJson()
            if (isConnect) {
                thread {
                    sendMessage(out)
                }
            } else {
                Toast.makeText(this, "通信已断开", Toast.LENGTH_SHORT).show()
            }
        }


        button3?.setOnClickListener {

            if (!isConnect) {
                thread {
                    initConnect(ip, port)
//                    val statue = "upload"
//                    jsout.put("type", "connecting")
//                    jsout.put("equipmentId", 836039386)
//                    jsout.put("job", statue)
//                    val answer = jsout.toString() + "\n"
//                    sendMessage(answer)  //可能导致解码失败
                }
                Thread.sleep(300)
            } else {
                Toast.makeText(this, "连接失败", Toast.LENGTH_SHORT).show()
            }

        }
        button4?.setOnClickListener {
            socket?.close()
            isConnect = false
            Toast.makeText(this, "连接已断开", Toast.LENGTH_SHORT).show()
        }

        button5?.setOnClickListener {
            if (isConnect) {
                thread {
                    val spfRecord = getSharedPreferences("spfRecord", MODE_PRIVATE)
                    val image64 = spfRecord.getString("image_64", "")
                    sendImage(image64)
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
                print(data)
                if (data != null) { //判断服务器是否有数据发送
                    text?.text = this.data
                    if (data == "101\n" || data == "102\n" || data == "100\n" || data == "105\n" || data == "106\n") {
                        data=data
                    } else {
                        json = data?.let { JSONObject(it) }!!
                    }
                }
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
        val equipmentId = 836039386
        val diagnosisId = "0001"
        val diagnosisTime = "2022-10-10 19:59:53"
        val diagnosisPerson = false
//        val value = 3.00
        val picture = 1
        //仪器数据
        val spfRecord = getSharedPreferences("spfRecord", MODE_PRIVATE)
        val id_acp = spfRecord.getString("name", "")
        val time_acp = spfRecord.getString("time", "")
        val voc_acp = spfRecord.getString("dianya", "")
        val value = spfRecord.getString("yql", "")
//        val key_acp = spfRecord.getBoolean("kg","")
        println(value)

        if (json.has("equipmentId")) {
            json.put("equipmentId", equipmentId)
        }
        if (json.has("diagnosisId")) {
            json.put("diagnosisId", diagnosisId)
        }
        if (json.has("diagnosisTime")) {
            json.put("diagnosisTime", diagnosisTime)
        }
//        if (json.has("diagnosisPerson")) {
//            json.put("diagnosisPerson", diagnosisPerson)
//        }
        if (json.has("value")) {
            json.put("value", value)
        }
        if (json.has("dianya")) {
            json.put("dianya", voc_acp)
        }
        if (json.has("picture")) {
            json.put("picture", picture)
        }
        return json.toString() + "\n"

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
                println("发送成功")
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

    fun sendImage(img: String?) {
        try {
            val os = socket?.getOutputStream()
            val pw = os?.let { PrintWriter(it) }
            img?.let { pw?.write(it) }
            pw?.flush()
            socket?.shutdownOutput()
        }catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun image(view: View) {
        val intent = Intent(this@MainActivity, takeactivity::class.java)
        startActivity(intent)
    }

    fun bluetooth(view: View) {
        val intent = Intent(this@MainActivity, BluetoothActivity::class.java)
        startActivity(intent)
    }

}