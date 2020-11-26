package com.example.wordsactivity

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.baidu.ocr.sdk.OCR
import com.baidu.ocr.sdk.OnResultListener
import com.baidu.ocr.sdk.exception.OCRError
import com.baidu.ocr.sdk.model.AccessToken
import com.baidu.ocr.sdk.model.GeneralBasicParams
import com.baidu.ocr.sdk.model.GeneralResult
import com.baidu.ocr.sdk.model.WordSimple
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.entity.LocalMedia
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var btn : Button
    private lateinit var tv : TextView

    private val REQUESTCODE = 11

    private var hasGotToken : Boolean = false
    private lateinit var selectList : List<LocalMedia>
    private var dialog:ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        check()
        initView()
        initAccessTokenWithAkSk()
    }

    /**
     * 权限检查，Android6.0以后需要
     */
    private fun check() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val check = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if(check == PackageManager.PERMISSION_DENIED){
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA),REQUESTCODE)
            }
        }
    }

    private fun initView() {
        dialog = ProgressDialog(this)
        dialog?.setMessage("结果加载中")
        dialog?.create()
        selectList = emptyList()
        btn = findViewById(R.id.btn_ws)
        tv = findViewById(R.id.tv)
        btn.setOnClickListener {
            tv.text = "识别结果"
            if(hasGotToken) {//如果通过AK、SK获取token就拍照
                PictureSelector.create(this)
                    .openCamera(PictureMimeType.ofImage())
                    .previewImage(false)
                    .isCamera(true)
                    .imageFormat(PictureMimeType.PNG)
                    .enableCrop(false)
                    .compress(true)
                    .glideOverride(160,160)
                    .withAspectRatio(16,9)
                    .freeStyleCropEnabled(true)
                    .selectionMedia(selectList)
                    .minimumCompressSize(100)
                    .forResult(PictureConfig.CHOOSE_REQUEST)
                dialog?.show()
            }else {
                Toast.makeText(applicationContext,"获取AK、SK失败",Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 初始化获取Token
     */
    private fun initAccessTokenWithAkSk() {
        OCR.getInstance(this@MainActivity).initAccessTokenWithAkSk(
            object : OnResultListener<AccessToken> {
                override fun onResult(result: AccessToken) {
                    val token = result.accessToken
                    Log.e("TAG","token:$token")
                    hasGotToken = true
                }

                override fun onError(error: OCRError) {
                    Log.e("TAG", "AK、SK获取token失败")
                }
            }, applicationContext, "TZXFZuyZpzPyPu4CmwGp2TNG","KahmROk9cjuO0eFNzsGRckUpW3kOnfVZ"
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            when(requestCode){
                PictureConfig.CHOOSE_REQUEST -> {
                    selectList = PictureSelector.obtainMultipleResult(data)
                    val  images = selectList[0].path
                    currency(images)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUESTCODE){
            if(permissions[0] == Manifest.permission.WRITE_EXTERNAL_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(applicationContext,"存储卡授权成功",Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(applicationContext,"授权失败",Toast.LENGTH_LONG).show()
            }
            if(permissions[1] == Manifest.permission.CAMERA && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(applicationContext,"媒体库授权成功",Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(applicationContext,"授权失败",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun currency(imagePath: String) {
        val sb = StringBuffer()
        //通用文字识别参数设置，具体可看百度文字识别api
        val params = GeneralBasicParams()
        params.setDetectDirection(true)
        params.imageFile = File(imagePath)
        //调用通用文字识别服务
        OCR.getInstance(this).recognizeGeneralBasic(params,
        object : OnResultListener<GeneralResult> {
            override fun onResult(p0: GeneralResult) {
                for (wordSimple:WordSimple in p0.wordList){
                    sb.append(wordSimple.words)
                    sb.append("\n")
                }
                dialog?.dismiss()
                tv.text = sb.toString()
            }

            override fun onError(p0: OCRError) {
                dialog?.dismiss()
                tv.text = "数据返回异常，请重新再试"
            }
        })
    }

    /**
     * 释放资源
     */
    override fun onDestroy() {
        super.onDestroy()
        OCR.getInstance(this).release()
    }
}