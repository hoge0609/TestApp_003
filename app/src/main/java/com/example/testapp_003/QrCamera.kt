package com.example.testapp_003

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.journeyapps.barcodescanner.camera.CameraSettings

// QRカメラクラス
class QrCamera : AppCompatActivity() {
    // バーコードビュー
    private var m_qrView: CompoundBarcodeView? = null
    // バーコードテキスト
    private var m_qrText: String = ""

    // アクティビティ作成
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_camera)
        Log.d("QrCamera", "onCreate")
        Toast.makeText(this, "QrCamera::onCreate", Toast.LENGTH_SHORT).show()

        // コンポーネント初期化
        this.initComponent()
        // カメラオープン
        openCamera()
    }

    // アクティビティ破棄
    override fun onDestroy() {
        super.onDestroy()
        Log.d("QrCamera", "onDestroy")
        Toast.makeText(this, "QrCamera::onDestroy", Toast.LENGTH_SHORT).show()

        // カメラクローズ
        closeCamera()
    }

    // アクティビティ再開
    override fun onResume() {
        super.onResume()
        Log.d("PhotoVideoQrCamera", "onResume")
        Toast.makeText(this, "PhotoVideoQrCamera::onResume", Toast.LENGTH_SHORT).show()

        // カメラオープン
        openCamera()
    }

    // アクティビティ停止
    override fun onPause() {
        super.onPause()

        // カメラクローズ
        closeCamera()
    }

    // コンポーネント初期化
    fun initComponent(): Boolean {
        var ret: Boolean = false
        var msg: String = "コンポーネント初期化エラー"

        try {
            // バーコードビュー初期化
            m_qrView = findViewById<CompoundBarcodeView>(R.id.qr_view_01)
            // バーコードビューテキストをクリア
            m_qrView?.statusView?.text = ""
            // TextView_05テキストの登録
            setTextView_05Text("NO DATA")

            ret = true
        } catch (ex: Exception) {
            Log.d("QrCamera", ("initComponent -> " + msg))
            Toast.makeText(this, ("QrCamera::initComponent -> " + msg), Toast.LENGTH_SHORT).show()
            ret = false
        } finally {
            if (ret == false) {
                // アクティビティ終了
                finish()
            }
        }

        return ret
    }

    // カメラオープン
    private fun openCamera() {
        if (m_qrView == null) {
            return
        }
        // QRコード読み取り開始
        m_qrView?.resume()
        // Button_18テキストの登録
        setButton_18Text("カメラ終了")

        m_qrView?.decodeContinuous(object: BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (result == null) {
                    return
                }
                if (m_qrText != result.text) {
                    // バーコードテキストを更新
                    m_qrText = result.text
                    // TextView_05テキストの登録
                    setTextView_05Text(m_qrText)
                    Log.d("QrCamera::openCamera", m_qrText.toString())
                    Toast.makeText(applicationContext, m_qrText, Toast.LENGTH_SHORT).show()
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) { }
        })
    }

    // カメラクローズ
    private fun closeCamera() {
        if (m_qrView == null) {
            return
        }
        // QRコード読み取り停止
        m_qrView?.pause()
        // Button_18テキストの登録
        setButton_18Text("カメラ起動")
    }

    // Button_18テキストの登録
    fun setButton_18Text(setStr: String) {
        // ボタン取得
        var button: Button = findViewById<Button>(R.id.button_18)
        when (setStr) {
            "カメラ起動" -> {
                button.text = setStr
            }
            "カメラ終了" -> {
                button.text = setStr
            }
            else -> {
                button.text = ""
            }
        }
    }

    // 「戻る」ボタンクリック
    fun onClickButton_17(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_17", buttonText.toString())

        // ボタンのテキストを判定
        when (buttonText) {
            "戻る" -> {
                this.finish()
            }
        }
    }

    // 「カメラ起動」ボタンクリック
    fun onClickButton_18(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_18", buttonText.toString())

        // ボタンのテキストを判定
        when (buttonText) {
            "カメラ起動" -> {
                openCamera()
            }
            "カメラ終了" -> {
                closeCamera()
            }
        }
    }

    // TextView_05テキストの登録
    fun setTextView_05Text(setStr: String) {
        // テキストビュー取得
        var textView: TextView = findViewById<TextView>(R.id.textView_05)
        textView.text = setStr
    }

}