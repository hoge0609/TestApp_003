package com.example.testapp_003

import android.content.Context
import android.hardware.camera2.*
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// カメラ情報クラス
class CameraInfo : AppCompatActivity() {
    // カメラデータクラスリスト
    private lateinit var m_cameraDataList: Array<CameraData?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_info)

        Log.d("CameraInfo", "onCreate")
        Toast.makeText(this, "CameraInfo::onCreate", Toast.LENGTH_SHORT).show()

        // リストビュー初期化
        this.initListView_03()
    }

    // リストビュー初期化
    fun initListView_03() {
        var count: Int = 0
        var index: Int = 0
        var array: ArrayList<String> = arrayListOf()

        // カメラマネジャーの取得
        val manager: CameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        // カメラIDリストをチェック
        if (manager.cameraIdList != null) {
            // カメラの数を取得
            count = manager.cameraIdList.size
        }

        if (0 < count) {
            // カメラデータクラスリスト初期化
            m_cameraDataList = arrayOfNulls(count)

            for (cameraId in manager.cameraIdList) {
                // カメラデータクラスを作成して、カメラデータクラスリストに追加
                m_cameraDataList.set(index, CameraData(cameraId, manager))
                // カメラ名を登録
                array.add(m_cameraDataList.get(index)?.m_cameraName.toString())
                index ++
            }
        } else {
            array.add("カメラなし")
        }

        // リストビュー取得
        val listView = findViewById<ListView>(R.id.listView_03)
        // ArrayAdapterの生成
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, array)
        // ListViewに、生成したAdapterを設定
        listView.adapter = adapter
    }

    // ボタンクリックイベント
    fun onClickButton_03(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_03", buttonText.toString())

        // ボタンのテキストを判定
        when (buttonText) {
            "戻る" -> {
                this.finish()
            }
        }
    }

}