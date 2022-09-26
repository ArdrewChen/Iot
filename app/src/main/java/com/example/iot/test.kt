package com.example.iot

import java.io.*
import java.net.Socket


class ServerFileThread(var socket: Socket) : Thread() {
    var a: InputStream? = null
    var isr: InputStreamReader? = null
    var br: BufferedReader? = null
    override fun run() {
        try {
            //和客户端进行通讯
            a = socket.getInputStream()
            isr = InputStreamReader(a)
            br = BufferedReader(isr)

            //创建文件，打开文件，往文件里面写数据
            val file = File("D:\\TestFile\\android.txt")
            val fos = FileOutputStream(file)
            val osw = OutputStreamWriter(fos)
            val bw = BufferedWriter(osw)

            //把客户端传入的文件中内容读取
            var data = br!!.readLine()
            while (data != null) {
                bw.write(data)
                bw.flush()
                data = br!!.readLine()
            }
            socket.shutdownInput()
            println("我是服务器端，文件已写入")

            //反馈信息给客户端
            val os = socket.getOutputStream()
            val pw = PrintWriter(os)
            pw.write("欢迎你~")
            pw.flush()
            socket.shutdownOutput()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (br != null) {
                try {
                    br!!.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (isr != null) {
                try {
                    isr!!.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (a != null) {
                try {
                    a!!.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
