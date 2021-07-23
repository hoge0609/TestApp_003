package com.example.testapp_003

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

// 写真カメラクラス
class PhotoCamera : AppCompatActivity() {
    // テクスチャビュー
    private lateinit var m_textureView: TextureView
    // カメラマネージャー
    private lateinit var m_cameraManager: CameraManager
    // カメラデータクラスリスト
    private lateinit var m_cameraDataList: Array<CameraData?>

    // カメラデバイス
    private var m_cameraDevice: CameraDevice? = null
    // カメラキャプチャーセッション
    private var m_captureSession: CameraCaptureSession? = null
    // カレントカメラINDEX
    private var m_currentCameraIndex: Int = 0
    // キャプチャーリクエスト
    private var m_captureRequest: CaptureRequest.Builder? = null

    // 待機中ボタン色
    private val def_waitingButtonColor: Int = Color.BLACK
    // 撮影中ボタン色
    private val def_shootingButtonColor: Int = Color.RED

    // 写真保存ディレクトリ
    private val def_photoSaveDir: String = "TestApp_003/Pictures"

    // アクティビティ作成
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_camera)

        Log.d("PhotoCamera", "onCreate")
        Toast.makeText(this, "PhotoCamera::onCreate", Toast.LENGTH_SHORT).show()

        // コンポーネント初期化
        if(this.initComponent() == false) {
            // アクティビティ終了
            this.finish()
        }
    }

    // アクティビティ再開
    override fun onResume() {
        super.onResume()

        Log.d("PhotoCamera", "onResume")
        Toast.makeText(this, "PhotoCamera::onResume", Toast.LENGTH_SHORT).show()

        // テクスチャビュー利用可能チェック
        if (m_textureView.isAvailable) {
            // カメラオープン
            openCamera()
        } else {
            // テクスチャビューイベント登録
            m_textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                // テクスチャビュー利用可能
                override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                    // カメラオープン
                    openCamera()
                }

                // テクスチャビューサイズ変更
                override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {}

                // テクスチャビュー更新
                override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {}

                // テクスチャビュー破棄
                override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean = true
            }
        }
    }

    // アクティビティ破棄
    override fun onDestroy() {
        super.onDestroy()

        Log.d("PhotoCamera", "onDestroy")
        Toast.makeText(this, "PhotoCamera::onDestroy", Toast.LENGTH_SHORT).show()

        // カメラクローズ
        closeCamera()
    }

    // コンポーネント初期化
    fun initComponent(): Boolean {
        var ret: Boolean = false
        var msg: String = "コンポーネント初期化エラー"
        var count: Int = 0
        var index: Int = 0
        var setName: String = ""

        try {
            // ボタン初期化
            // 撮影ボタン
            var button = findViewById<Button>(R.id.button_07)
            // 背景色をセット（待機中）
            button.setBackgroundColor(def_waitingButtonColor)

            // テクスチャビュー初期化
            m_textureView = findViewById<TextureView>(R.id.textureView_01)
            // サーフェイステクスチャのサイズ登録
            m_textureView.surfaceTexture?.setDefaultBufferSize(
                m_textureView.width,    // テクスチャビューの幅
                m_textureView.height    // テクスチャビューの高さ
            )

            // カメラマネージャー初期化
            m_cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            // カメラIDリストをチェック
            if (m_cameraManager.cameraIdList != null) {
                // カメラの数を取得
                count = m_cameraManager.cameraIdList.size
            }
            if (count < 1) {
                msg = "端末にカメラが見つかりませんでした"
                throw Exception(msg)
            }

            // カメラデータクラスリスト初期化
            m_cameraDataList = arrayOfNulls(count)

            for (cameraId in m_cameraManager.cameraIdList) {
                // カメラデータクラスを作成して、カメラデータクラスリストに追加
                m_cameraDataList.set(index, CameraData(cameraId, m_cameraManager))
                index ++
            }

            ret = true
        } catch (ex: Exception) {
            Log.d("PhotoCamera", ("initComponent -> " + msg))
            Toast.makeText(this, ("PhotoCamera::initComponent -> " + msg), Toast.LENGTH_SHORT).show()
        }

        return ret
    }

    // カメラオープン
    private fun openCamera() {
        // パーミッションチェック
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED) {
            return
        }

        m_cameraManager.openCamera(m_currentCameraIndex.toString(), object: CameraDevice.StateCallback() {
            // カメラ接続完了
            override fun onOpened(camera: CameraDevice) {
                // カメラデバイス登録
                m_cameraDevice = camera
                // カメラキャプチャーセッション作成
                if (createCameraPreviewSession() == false) {
                    closeCamera()
                } else {
                    // Button_05テキストの登録
                    setButton_05Text("カメラ終了")
                    // TextView_01テキストの登録
                    setTextView_01Text(m_cameraDataList.get(m_currentCameraIndex)?.m_cameraName.toString())
                }
            }

            // カメラ切断
            override fun onDisconnected(camera: CameraDevice) {
                closeCamera()
            }

            // カメラエラー
            override fun onError(camera: CameraDevice, p1: Int) {
                closeCamera()
            }
        }, null)
    }

    // カメラクローズ
    private fun closeCamera() {
        // カメラデバイス
        if (m_cameraDevice != null) {
            m_cameraDevice?.close()
            m_cameraDevice = null
        }
        // カメラキャプチャーセッション
        if (m_captureSession != null) {
            m_captureSession?.close()
            m_captureSession = null
        }
        // キャプチャーリクエスト
        m_captureRequest = null
        // Button_05テキストの登録
        setButton_05Text("カメラ起動")
    }

    // カメラキャプチャーセッション作成
    private fun createCameraPreviewSession(): Boolean {
        var ret: Boolean = false
        var msg: String = "カメラキャプチャーセッション作成エラー"
        var surface: Surface? = null

        try {
            // カメラデバイス取得
            if (m_cameraDevice == null) {
                msg = "カメラデバイス取得エラー"
                throw Exception(msg)
            }

            // サーフェイス初期化
            surface = Surface(m_textureView.surfaceTexture)
            if (surface.isValid() == false) {
                msg = "サーフェイス初期化エラー"
                throw Exception(msg)
            }

            // カメラデバイスへのキャプチャーリクエスト作成
            m_captureRequest = m_cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            if (m_captureRequest == null) {
                msg = "キャプチャーリクエスト作成エラー"
                throw Exception(msg)
            }

            // キャプチャーリクエストにサーフェイスを追加
            m_captureRequest?.addTarget(surface)

            // キャプチャーセッション作成
            m_cameraDevice?.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    // セッション作成完了
                    override fun onConfigured(session: CameraCaptureSession) {
                        m_captureSession = session
                        m_captureSession?.setRepeatingRequest(
                            m_captureRequest!!.build(),
                            null,
                            null
                        )
                    }

                    // セッション作成エラー
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        msg = "キャプチャーセッション作成エラー"
                        throw Exception(msg)
                    }
                },
                null
            )

            ret = true
        } catch (ex: Exception) {
            Log.d("PhotoCamera", ("createCameraPreviewSession -> " + msg))
            Toast.makeText(this, ("PhotoCamera::createCameraPreviewSession -> " + msg), Toast.LENGTH_SHORT).show()
        }

        return ret
    }

    // TextView_01テキストの登録
    fun setTextView_01Text(setStr: String) {
        // テキストビュー取得
        var textView: TextView = findViewById<TextView>(R.id.textView_01)
        textView.text = setStr
    }

    // Button_05テキストの登録
    fun setButton_05Text(setStr: String) {
        // ボタン取得
        var button: Button = findViewById<Button>(R.id.button_05)
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

    // ボタンクリックイベント
    fun onClickButton_04(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_04", buttonText.toString())

        // ボタンのテキストを判定
        when (buttonText) {
            "戻る" -> {
                this.finish()
            }
        }
    }

    fun onClickButton_05(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_05", buttonText.toString())

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

    fun onClickButton_06(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_06", buttonText.toString())
        var index: Int = 0
        // 選択アイテムINDEX
        var selectIndex: Int = m_currentCameraIndex
        // カメラ名リスト
        var cameraNameList: Array<String?>

        cameraNameList = arrayOfNulls(m_cameraDataList.size)
        for (cameraData in m_cameraDataList) {
            // カメラ名を追加
            cameraNameList.set(index, cameraData?.m_cameraName)
            index ++
        }

        // ボタンのテキストを判定
        when (buttonText) {
            "カメラ切替" -> {
                // カメラ選択ダイアログ表示
                AlertDialog.Builder(this)
                    .setTitle("カメラ選択")
                    .setSingleChoiceItems(cameraNameList, m_currentCameraIndex, { dialog, which ->
                        // アイテム選択
                        // 選択アイテムINDEX更新
                        selectIndex = which
                    })
                    .setPositiveButton("OK", { dialog, which ->
                        // 決定ボタン押下
                        // カレントカメラINDEXと選択アイテムINDEXを比較
                        if (m_currentCameraIndex != selectIndex) {
                            // カレントカメラINDEXを更新
                            m_currentCameraIndex = selectIndex
                            // カメラクローズ
                            closeCamera()
                            // カメラオープン
                            openCamera()
                        }
                    })
                    .show()
            }
        }
    }

    fun onClickButton_07(view: View) {
        // ボタン取得
        var button = (view as Button)
        // ボタンのテキストを取得
        var buttonText = button.text
        Log.d("button_04", buttonText.toString())

        // ボタンのテキストを判定
        when (buttonText) {
            "写真撮影" -> {
                // 背景色をセット（撮影中）
                button.setBackgroundColor(def_shootingButtonColor)
                // 写真撮影
                photoShooting(Bitmap.CompressFormat.JPEG)
                // 背景色をセット（待機中）
                button.setBackgroundColor(def_waitingButtonColor)
            }
        }
    }

    // 写真撮影
    fun photoShooting(format: Bitmap.CompressFormat) {
        var msg: String = "写真撮影処理でエラー"
        var fileName: String
        var ext: String
        var saveDir: File
        var saveFile: File

        try {
            // ファイル保存ディレクトリ
            saveDir = File(Environment.getExternalStorageDirectory().path, def_photoSaveDir)
            // ファイル保存ディレクトリの存在チェック
            if (saveDir.exists() == false) {
                // ファイル保存ディレクトリが存在しない場合は作成
                saveDir.mkdirs()
            }

            // ファイル名に現在日時をセット
            fileName = LocalDateTime.now().toString()
            // ファイルフォーマットをチェック
            when (format) {
                Bitmap.CompressFormat.JPEG -> {
                    ext = ".jpg";
                }
                Bitmap.CompressFormat.PNG -> {
                    ext = ".png";
                }
                else -> {
                    throw Exception()
                }
            }
            fileName += ext
            // ファイル保存ディレクトリ、ファイル名を結合
            saveFile = File(saveDir, fileName)

            // プレビューの更新を停止
            m_captureSession?.stopRepeating()

            if (m_textureView.isAvailable) {
                // 写真ファイル作成
                if (createPhotoFile(
                        saveFile,
                        m_textureView,
                        format
                    ) == true) {
                    Log.d("PhotoCamera", ("Create Completed : " + fileName))
                    Toast.makeText(this, ("Create Completed : " + fileName), Toast.LENGTH_SHORT).show()
                } else {
                    msg = "写真ファイル作成エラー"
                    throw Exception(msg)
                }
            } else {
                throw Exception(msg)
            }
        } catch (ex: Exception) {
            Log.d("PhotoCamera", ("photoShooting -> " + msg))
            Toast.makeText(this, ("PhotoCamera::photoShooting -> " + msg), Toast.LENGTH_SHORT).show()
        } finally {
            // プレビューの更新を再開
            m_captureSession?.setRepeatingRequest(
                m_captureRequest!!.build(),
                null,
                null
            )
        }
    }

    // 写真ファイル作成
    fun createPhotoFile(savefile: File, textureView: TextureView, format: Bitmap.CompressFormat) : Boolean {
        var ret: Boolean = false
        var outputStream: FileOutputStream? = null
        var bitmap: Bitmap? = null

        try {
            // ファイルストリーム初期化
            outputStream = FileOutputStream(savefile)
            // テクスチャビューからBitmapを作成
            bitmap = textureView.getBitmap(textureView.width, textureView.height)
            // ファイル作成
            bitmap?.compress(format, 100, outputStream)

            ret = true
        } catch (ex: Exception) {
            ret = false
        } finally {
            if (outputStream != null) {
                outputStream.close()
                outputStream = null
            }
        }

        return ret
    }

}