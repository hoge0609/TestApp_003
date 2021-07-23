package com.example.testapp_003

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

// メインクラス
class MainActivity : AppCompatActivity() {
    // パーミッションリクエストコード
    private val REQUEST_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "onCreate")
        Toast.makeText(this, "MainActivity::onCreate", Toast.LENGTH_SHORT).show()

        // パーミッションリスト作成
        val permissions: Array<String?> = arrayOf(
            Manifest.permission.CAMERA,                     // カメラ
            Manifest.permission.WRITE_EXTERNAL_STORAGE      // ストレージへの書き込み
        )
        // パーミッションのチェック
        checkPermission(permissions, REQUEST_CODE)
    }

    // パーミッションのチェック
    fun checkPermission(permissions: Array<String?>?, requestCode: Int) {
        // 許可されていないものだけダイアログを表示
        ActivityCompat.requestPermissions(this, permissions!!, requestCode)
    }

    // requestPermissionsのコールバック
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // 拒否されたパーミッション数
        var rejectedCount: Int = 0

        if (requestCode != REQUEST_CODE){
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        when (requestCode) {
            REQUEST_CODE -> {
                var i = 0
                while (i < permissions.size) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
//                        Log.d("Added Permission", permissions[i])
//                        Toast.makeText(this, ("Added Permission::" + permissions[i]), Toast.LENGTH_SHORT).show()
                    } else {
                        // 「今後表示しない」のチェックがONにされていたかチェック
                        if (shouldShowRequestPermissionRationale(permissions[i]) == true) {
                            // 「今後表示しない」のチェックOFF
                            Log.d("Rejected Permission", permissions[i])
                            Toast.makeText(this, ("Rejected Permission::" + permissions[i]), Toast.LENGTH_SHORT).show()
                        } else {
                            // 「今後表示しない」のチェックON
                            Log.d("Forever Rejected Permission", permissions[i])
                            Toast.makeText(this, ("Forever Rejected Permission::" + permissions[i]), Toast.LENGTH_SHORT).show()
                        }
                        // 拒否されたパーミッション数をインクリメント
                        rejectedCount ++
                    }
                    i++
                }
            }
        }

        // 拒否されたパーミッション数をチェック
        if (0 < rejectedCount) {
            // アプリ終了
            this.closeApp()
        } else {
            // リストビュー初期化
            this.initListView_01()
        }
    }

    // リストビュー初期化
    fun initListView_01() {
        // リストビューに登録するデータ配列
        val array = arrayOf("デバイス情報", "カメラ情報", "写真撮影")
        // リストビュー取得
        val listView = findViewById<ListView>(R.id.listView_01)
        // ArrayAdapterの生成
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, array)
        // ListViewに、生成したAdapterを設定
        listView.adapter = adapter

        // リストビュークリックイベント
        listView.setOnItemClickListener { parent, view, position, id ->
            // リストビューのテキストを取得
            var listViewText = view.findViewById<TextView>(android.R.id.text1).text
            Log.d("listView_01", listViewText.toString())

            // リストビューのテキストを判定
            when (listViewText) {
                "デバイス情報" -> {
                    // デバイス情報クラス開始
                    startActivity(Intent(this, DeviceInfo::class.java))
                }
                "カメラ情報" -> {
                    // カメラ情報クラス開始
                    startActivity(Intent(this, CameraInfo::class.java))
                }
                "写真撮影" -> {
                    // 写真カメラクラス開始
                    startActivity(Intent(this, PhotoCamera::class.java))
                }
            }
        }
    }

    // ボタンクリックイベント
    fun onClickButton_01(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_01", buttonText.toString())

        // ボタンのテキストを判定
        when (buttonText) {
            "終了" -> {
                // アプリ終了
                this.closeApp()
            }
        }
    }

    // アプリ終了
    fun closeApp() {
        this.finish();
        this.moveTaskToBack(true);
    }

}