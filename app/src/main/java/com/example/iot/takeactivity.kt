package com.example.iot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

//package com.example.takephoto2;
class takeactivity : AppCompatActivity() {
    private var imageUri: Uri? = null
    private var ivAvatar: ImageView? = null
    private var imageBase64: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_takeactivity)
        initView()
        initData()
        initEvent()
    }

    private fun initData() {
        ivAvatar = findViewById(R.id.iv_avatar)
        dataFromSpf
    }

    private fun initView() {}
    private fun initEvent() {}
    private val dataFromSpf: Unit
        private get() {
            val spfRecord = getSharedPreferences("spfRecord", MODE_PRIVATE)
            val image64 = spfRecord.getString("image_64", "")
            ivAvatar!!.setImageBitmap(ImageUtil.base64ToImage(image64))
        }

    fun takePhoto(view: View?) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            //真正去拍照
            doTake()
        } else {
            //去申请权限
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doTake()
            } else {
                Toast.makeText(this, "没有摄像头权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun doTake() {
        val imageTemp = File(externalCacheDir, "imageOut.jpeg")
        if (imageTemp.exists()) {
//            imageTemp.delete();
        }
        try {
            imageTemp.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        imageUri = if (Build.VERSION.SDK_INT > 24) {
            //contentProvider
            FileProvider.getUriForFile(this, "com.example.iot.fileprovider", imageTemp)
        } else {
            Uri.fromFile(imageTemp)
        }
        val intent = Intent()
        intent.action = "android.media.action.IMAGE_CAPTURE"
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, REQUEST_CODE_TAKE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_TAKE) {
            if (resultCode == RESULT_OK) {
                try {
                    //获取拍摄的图片
                    val inputStream = contentResolver.openInputStream(imageUri!!)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    ivAvatar!!.setImageBitmap(bitmap)
                    //保存图片
                    val imageTobase64 = ImageUtil.imageToBase64(bitmap)
                    imageBase64 = imageTobase64
                } catch (e: FileNotFoundException) {
//                    e.printStackTrace();
                }
            }
        }
    }

    fun choosePhoto(view: View?) {}
    fun save(view: View?) {
        val spfRecord = getSharedPreferences("spfRecord", MODE_PRIVATE)
        val edit = spfRecord.edit()
        edit.putString("image_64", imageBase64)
        edit.apply()
        finish()
    }

    companion object {
        const val REQUEST_CODE_TAKE = 1
        const val REQUEST_CODE_CHOOSE = 0
    }
}