package com.example.testapp_003

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.os.Build
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast

// デバイス情報クラス
class DeviceInfo : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_info)

        Log.d("DeviceInfo", "onCreate")
        Toast.makeText(this, "DeviceInfo::onCreate", Toast.LENGTH_SHORT).show()

        // リストビュー初期化
        this.initListView_02()
    }

    // リストビュー初期化
    fun initListView_02() {
        var array: ArrayList<String> = arrayListOf()

        array.add("Build.BOARD: ${Build.BOARD}")
        array.add("Build.BOOTLOADER: ${Build.BOOTLOADER}")
        array.add("Build.BRAND: ${Build.BRAND}")
        array.add("Build.DEVICE: ${Build.DEVICE}")
        array.add("Build.DISPLAY: ${Build.DISPLAY}")
        array.add("Build.FINGERPRINT: ${Build.FINGERPRINT}")
        array.add("Build.HARDWARE: ${Build.HARDWARE}")
        array.add("Build.HOST: ${Build.HOST}")
        array.add("Build.ID: ${Build.ID}")
        array.add("Build.MANUFACTURER: ${Build.MANUFACTURER}")
        array.add("Build.MODEL: ${Build.MODEL}")
        array.add("Build.PRODUCT: ${Build.PRODUCT}")
        array.add("Build.SUPPORTED_32_BIT_ABIS: ${Build.SUPPORTED_32_BIT_ABIS}")
        array.add("Build.SUPPORTED_64_BIT_ABIS: ${Build.SUPPORTED_64_BIT_ABIS}")
        array.add("Build.SUPPORTED_ABIS: ${Build.SUPPORTED_ABIS}")
        array.add("Build.TAGS: ${Build.TAGS}")
        array.add("Build.TIME: ${Build.TIME}")
        array.add("Build.TYPE: ${Build.TYPE}")
        array.add("Build.USER: ${Build.USER}")
        array.add("Build.VERSION.BASE_OS: ${Build.VERSION.BASE_OS}")
        array.add("Build.VERSION.CODENAME: ${Build.VERSION.CODENAME}")
        array.add("Build.VERSION.INCREMENTAL: ${Build.VERSION.INCREMENTAL}")
        array.add("Build.VERSION.PREVIEW_SDK_INT: ${Build.VERSION.PREVIEW_SDK_INT}")
        array.add("Build.VERSION.RELEASE: ${Build.VERSION.RELEASE}")
        array.add("Build.VERSION.SDK_INT: ${Build.VERSION.SDK_INT}")
        array.add("Build.VERSION.SECURITY_PATCH: ${Build.VERSION.SECURITY_PATCH}")

        Build.SUPPORTED_32_BIT_ABIS.forEach {
            array.add("SUPPORTED_32_BIT_ABIS: $it")
        }
        Build.SUPPORTED_64_BIT_ABIS.forEach {
            array.add("SUPPORTED_64_BIT_ABIS: $it")
        }
        Build.SUPPORTED_ABIS.forEach {
            array.add("SUPPORTED_ABIS: $it")
        }

        // リストビュー取得
        val listView = findViewById<ListView>(R.id.listView_02)
        // ArrayAdapterの生成
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, array)
        // ListViewに、生成したAdapterを設定
        listView.adapter = adapter
    }

    // ボタンクリックイベント
    fun onClickButton_02(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_02", buttonText.toString())

        // ボタンのテキストを判定
        when (buttonText) {
            "戻る" -> this.finish()
        }
    }

}