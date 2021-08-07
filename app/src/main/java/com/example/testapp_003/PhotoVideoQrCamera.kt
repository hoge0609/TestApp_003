package com.example.testapp_003

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.journeyapps.barcodescanner.camera.CameraSettings
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

// 写真・動画・QRコードカメラクラス
class PhotoVideoQrCamera : AppCompatActivity() {
    // テクスチャビュー
    private lateinit var m_textureView: TextureView
    // カメラマネージャー
    private lateinit var m_cameraManager: CameraManager
    // カメラデータクラスリスト
    private lateinit var m_cameraDataList: Array<CameraData?>

    // カメラデバイス
    private var m_cameraDevice: CameraDevice? = null

    // バーコードビュー
    private lateinit var m_qrView: CompoundBarcodeView
    // バーコードテキスト
    private var m_qrText: String = ""

    // キャプチャーセッション
    private var m_captureSession: CameraCaptureSession? = null
    // プレビューリクエストビルダー
    private lateinit var m_previewRequestBuilder: CaptureRequest.Builder

    // メディアレコーダー
    private var m_mediaRecorder: MediaRecorder? = null
    // 撮影中フラグ
    private var m_isRecordingFlag = false

    // プレビューサイズ
    private lateinit var m_previewSize: Size
    // ビデオサイズ
    private lateinit var m_videoSize: Size

    // センサーの向き
    private var m_sensorOrientation: Int? = null

    // 待機中ボタン色
    private val def_waitingButtonColor: Int = Color.BLACK
    // 撮影中ボタン色
    private val def_recordingButtonColor: Int = Color.RED

    // 写真保存ディレクトリ
    private val def_photoSaveDir: String = "TestApp_003/Pictures"
    // 動画保存ディレクトリ
    private val def_videoSaveDir: String = "TestApp_003/Movies"
    // 出力ファイル名
    private var m_outputFileName: String? = null

    // カメラモード：写真カメラ
    private val def_cameraModePhoto: Int = 0
    // カメラモード：ビデオカメラ
    private val def_cameraModeVideo: Int = 1
    // カメラモード：QRカメラ
    private val def_cameraModeQr: Int = 2

    // 設定データクラス
    private var m_settingData: PhotoVideoQrCameraSettingData? = null

    private val SENSOR_ORIENTATION_DEFAULT_DEGREES = 90
    private val SENSOR_ORIENTATION_INVERSE_DEGREES = 270
    private val DEFAULT_ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 90)
        append(Surface.ROTATION_90, 0)
        append(Surface.ROTATION_180, 270)
        append(Surface.ROTATION_270, 180)
    }
    private val INVERSE_ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 270)
        append(Surface.ROTATION_90, 180)
        append(Surface.ROTATION_180, 90)
        append(Surface.ROTATION_270, 0)
    }

    // アクティビティ作成
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_video_qr_camera)
        Log.d("PhotoVideoQrCamera", "onCreate")
        Toast.makeText(this, "PhotoVideoQrCamera::onCreate", Toast.LENGTH_SHORT).show()

        // コンポーネント初期化
        initComponent()
    }

    // アクティビティ破棄
    override fun onDestroy() {
        super.onDestroy()
        Log.d("PhotoVideoQrCamera", "onDestroy")
        Toast.makeText(this, "PhotoVideoQrCamera::onDestroy", Toast.LENGTH_SHORT).show()

        // カメラクローズ
        closeCamera()
        // 設定データクラス
        m_settingData = null
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
        var count: Int = 0
        var index: Int = 0

        try {
            // 設定データクラス
            m_settingData = PhotoVideoQrCameraSettingData(this)
            if (m_settingData?.m_checkFlag == false) {
                msg = "設定データ初期化エラー"
                throw Exception()
            }

            // テクスチャビュー初期化
            m_textureView = findViewById<TextureView>(R.id.textureView_04)
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
                throw Exception()
            }

            // カメラデータクラスリスト初期化
            m_cameraDataList = arrayOfNulls(count)

            for (cameraId in m_cameraManager.cameraIdList) {
                // カメラデータクラスを作成して、カメラデータクラスリストに追加
                m_cameraDataList.set(index, CameraData(cameraId, m_cameraManager))
                index ++
            }

            // バーコードビュー初期化
            m_qrView = findViewById<CompoundBarcodeView>(R.id.qr_view_02)
            // バーコードビューテキストをクリア
            m_qrView.statusView?.text = ""

            // 撮影中フラグ登録
            setIsRecordingFlag(false)

            ret = true
        } catch (ex: Exception) {
            Log.d("PhotoVideoQrCamera", ("initComponent -> " + msg))
            Toast.makeText(this, ("PhotoVideoQrCamera::initComponent -> " + msg), Toast.LENGTH_SHORT).show()
            ret = false
        } finally {
            if (ret == false) {
                // アクティビティ終了
                finish()
            }
        }

        return ret
    }

    // コンポーネントのセットアップ
    private fun setupComponent() {
        var textureViewVisibility: Int = View.INVISIBLE
        var qrViewVisibility: Int = View.INVISIBLE
        var textView_08Visibility: Int = View.INVISIBLE
        var button_22Visibility: Int = View.INVISIBLE
        var button_22Text: String = ""
        var textView_06Text: String = m_cameraDataList.get(m_settingData?.getCameraIndex()!!)?.m_cameraName.toString()
        var textView_07Text: String = ""
        var textView_08Text: String = ""
        var button_22BackgroundColor: Int = def_waitingButtonColor

        // カメラモードをチェック
        when (m_settingData?.getCameraMode()) {
            def_cameraModePhoto -> {    // 写真カメラ
                textureViewVisibility = View.VISIBLE
                button_22Visibility = View.VISIBLE
                button_22Text = "写真撮影"
                textView_07Text = "写真カメラ"
            }
            def_cameraModeVideo -> {    // ビデオカメラ
                textureViewVisibility = View.VISIBLE
                button_22Visibility = View.VISIBLE
                textView_07Text = "ビデオカメラ"
                // 撮影中フラグをチェック
                if (m_isRecordingFlag) {
                    button_22Text = "撮影終了"
                    button_22BackgroundColor = def_recordingButtonColor
                } else {
                    button_22Text = "動画撮影"
                }
            }
            def_cameraModeQr -> {       // QRカメラ
                qrViewVisibility = View.VISIBLE
                textView_08Visibility = View.VISIBLE
                textView_07Text = "QRカメラ"
                textView_08Text = "NO DATA"
            }
        }

        // テクスチャビューの表示切り替え
        m_textureView.visibility = textureViewVisibility
        // バーコードビューの表示切り替え
        m_qrView.visibility = qrViewVisibility
        // TextView_08表示切り替え
        setTextView_08Visibility(textView_08Visibility)
        // Button_22表示切り替え
        setButton_22Visibility(button_22Visibility)
        // TextView_06テキストの登録
        setTextView_06Text(textView_06Text)
        // TextView_07テキストの登録
        setTextView_07Text(textView_07Text)
        // TextView_08テキストの登録
        setTextView_08Text(textView_08Text)
        // Button_22テキストの登録
        setButton_22Text(button_22Text)
        // Button_22背景色登録
        setButton_22BackgroundColor(button_22BackgroundColor)

        // バーコードテキスト
        m_qrText = ""
    }

    // カメラオープン
    private fun openCamera() {
        // カメラモードをチェック
        when (m_settingData?.getCameraMode()) {
            def_cameraModePhoto, def_cameraModeVideo -> {   // 写真カメラ・ビデオカメラ
                // テクスチャビュー利用可能チェック
                if (m_textureView.isAvailable) {
                    // カメラオープン
                    openCamera(m_textureView.width, m_textureView.height)
                } else {
                    // テクスチャビューイベント登録
                    m_textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        // テクスチャビュー利用可能
                        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                            // カメラオープン
                            openCamera(width, height)
                        }
                        // テクスチャビューサイズ変更
                        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {}
                        // テクスチャビュー更新
                        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
                        // テクスチャビュー破棄
                        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture) = true
                    }
                }
            }
            def_cameraModeQr -> {                           // QRカメラ
                // カレントカメラINDEXを登録
                m_qrView.barcodeView.cameraSettings.requestedCameraId = m_settingData?.getCameraIndex()!!
                // QRコード読み取り開始
                m_qrView.resume()
                // Button_20テキストの登録
                setButton_20Text("カメラ終了")

                m_qrView.decodeContinuous(object: BarcodeCallback {
                    override fun barcodeResult(result: BarcodeResult?) {
                        if (result == null) {
                            return
                        }
                        if (m_qrText != result.text) {
                            // バーコードテキストを更新
                            m_qrText = result.text
                            // TextView_08テキストの登録
                            setTextView_08Text(m_qrText)
                            Log.d("PhotoVideoQrCamera::openCamera", m_qrText.toString())
                            Toast.makeText(applicationContext, m_qrText, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) { }
                })
            }
        }
    }

    private fun openCamera(width: Int, height: Int) {
        var msg: String = "カメラオープンエラー"
        var cameraData: CameraData? = null
        var setSize: Size? = null

        // パーミッションチェック
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            // カメラデータ取得
            cameraData = m_cameraDataList.get(m_settingData?.getCameraIndex()!!)
            // センサーの向き取得
            m_sensorOrientation = cameraData?.m_sensorOrientation
            // カメラサイズリストから録画・プレビューのサイズ取得
            for (size in cameraData?.m_cameraSizeList!!) {
                if (size != null) {
                    // テクスチャビューの幅と比較
                    if (size?.width!! <= m_textureView.width) {
                        setSize = size
                        break
                    }
                }
            }
            if (setSize == null) {
                throw Exception()
            }
            // ビデオサイズ
            m_videoSize = setSize
            // プレビューサイズ
            m_previewSize = m_videoSize

            // メディアレコーダー
            m_mediaRecorder = MediaRecorder()
            // カメラオープンイベント登録
            m_cameraManager.openCamera(
                cameraData?.m_cameraId!!,
                object : CameraDevice.StateCallback() {
                    // カメラ接続完了
                    override fun onOpened(cameraDevice: CameraDevice) {
                        m_cameraDevice = cameraDevice
                        // Button_20テキストの登録
                        setButton_20Text("カメラ終了")
                        // プレビュー開始
                        startPreview()
                    }
                    // カメラ切断
                    override fun onDisconnected(cameraDevice: CameraDevice) {
                        m_cameraDevice?.close()
                        m_cameraDevice = null
                    }
                    // カメラエラー
                    override fun onError(cameraDevice: CameraDevice, error: Int) {
                        m_cameraDevice?.close()
                        m_cameraDevice = null
                        // アクティビティ終了
                        finish()
                    }
                },
                null
            )

        } catch (ex: Exception) {
            Log.d("PhotoVideoQrCamera", ("openCamera -> " + msg))
            Toast.makeText(this, ("PhotoVideoQrCamera::openCamera -> " + msg), Toast.LENGTH_SHORT).show()
        }
    }

    // カメラクローズ
    private fun closeCamera() {
        var msg: String = "カメラクローズエラー"

        try {
            // カメラモードをチェック
            when (m_settingData?.getCameraMode()) {
                def_cameraModePhoto, def_cameraModeVideo -> {   // 写真カメラ・ビデオカメラ
                    // プレビューセッションクローズ
                    closePreviewSession()
                    m_cameraDevice?.close()
                    m_cameraDevice = null
                    m_mediaRecorder?.release()
                    m_mediaRecorder = null
                }
                def_cameraModeQr -> {                           // QRカメラ
                    // QRコード読み取り停止
                    m_qrView.pause()
                }
            }
            // Button_20テキストの登録
            setButton_20Text("カメラ起動")
        } catch (ex: Exception) {
            Log.d("PhotoVideoQrCamera", ("closeCamera -> " + msg))
            Toast.makeText(this, ("PhotoVideoQrCamera::closeCamera -> " + msg), Toast.LENGTH_SHORT).show()
        }
    }

    // プレビュー開始
    private fun startPreview() {
        var msg: String = "プレビュー開始エラー"
        var texture: SurfaceTexture? = null
        var previewSurface: Surface? = null

        // カメラデバイス・テクスチャビューをチェック
        if ((m_cameraDevice == null) ||
            (m_textureView.isAvailable == false)) {
            return
        }

        try {
            // プレビューセッションクローズ
            closePreviewSession()
            // テクスチャビューのサーフェイステクスチャ取得
            texture = m_textureView.surfaceTexture?.apply {
                // プレビューサイズ登録
                setDefaultBufferSize(m_previewSize.width, m_previewSize.height)
            }

            // プレビューサーフェイス作成
            previewSurface = Surface(texture)
            // プレビューリクエスト作成
            m_previewRequestBuilder = m_cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                // プレビューサーフェイスを登録
                addTarget(previewSurface)
            }

            // キャプチャーセッション作成
            m_cameraDevice?.createCaptureSession(
                listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    // セッション作成完了
                    override fun onConfigured(session: CameraCaptureSession) {
                        m_captureSession = session
                        // プレビュー更新
                        updatePreview()
                    }
                    // セッション作成エラー
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        msg = "プレビューセッション作成エラー"
                        throw Exception()
                    }
                },
                null
            )

        } catch (ex: Exception) {
            Log.d("PhotoVideoQrCamera", ("startPreview -> " + msg))
            Toast.makeText(this, ("PhotoVideoQrCamera::startPreview -> " + msg), Toast.LENGTH_SHORT).show()
        }
    }

    // プレビュー更新
    private fun updatePreview() {
        var msg: String = "プレビュー更新エラー"

        try {
            // カメラデバイスをチェック
            if (m_cameraDevice == null) {
                return
            }
            // プレビューリクエストビルダー設定登録
            m_previewRequestBuilder?.set(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO
            )
            // プレビュー開始
            m_captureSession?.setRepeatingRequest(m_previewRequestBuilder.build(), null, null)

        } catch (e: CameraAccessException) {
            Log.d("PhotoVideoQrCamera", ("updatePreview -> " + msg))
            Toast.makeText(this, ("PhotoVideoQrCamera::updatePreview -> " + msg), Toast.LENGTH_SHORT).show()
        }
    }

    // 録画開始
    private fun startRecordingVideo() {
        var msg: String = "録画開始エラー"
        var texture: SurfaceTexture? = null
        var previewSurface: Surface? = null
        var recorderSurface: Surface? = null

        // カメラデバイス・テクスチャビューをチェック
        if ((m_cameraDevice == null) ||
            (m_textureView.isAvailable == false)) {
            return
        }

        try {
            // プレビューセッションクローズ
            closePreviewSession()
            // メディアレコーダー初期化
            if (setUpMediaRecorder() == false) {
                msg = "メディアレコーダー初期化エラー"
                throw Exception()
            }

            // テクスチャビューのサーフェイステクスチャ取得
            texture = m_textureView.surfaceTexture?.apply {
                // プレビューサイズ登録
                setDefaultBufferSize(m_previewSize.width, m_previewSize.height)
            }

            // プレビューサーフェイス作成
            previewSurface = Surface(texture)
            // メディアレコーダーからレコーダーサーフェイス取得
            recorderSurface = m_mediaRecorder!!.surface

            // プレビューサーフェイス・レコーダーサーフェイスをリストに登録
            var surfaces = ArrayList<Surface>().apply {
                add(previewSurface)
                add(recorderSurface)
            }
            // レコーディングリクエスト作成
            m_previewRequestBuilder = m_cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                // プレビューサーフェイスを登録
                addTarget(previewSurface)
                // レコーダーサーフェイスを登録
                addTarget(recorderSurface)
            }

            // キャプチャーセッション作成
            m_cameraDevice?.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    // セッション作成完了
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        m_captureSession = cameraCaptureSession
                        // プレビュー更新
                        updatePreview()
                        // 録画開始
                        m_mediaRecorder?.start()
                        // 撮影中フラグ登録
                        setIsRecordingFlag(true)
                    }
                    // セッション作成エラー
                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        msg = "レコーディングセッション作成エラー"
                        throw Exception()
                    }
                },
                null
            )

        } catch (ex: Exception) {
            // プレビューセッションクローズ
            closePreviewSession()
            Log.d("PhotoVideoQrCamera", ("startRecordingVideo -> " + msg))
            Toast.makeText(this, ("PhotoVideoQrCamera::startRecordingVideo -> " + msg), Toast.LENGTH_SHORT).show()
        }
    }

    // プレビューセッションクローズ
    private fun closePreviewSession() {
        if (m_captureSession != null) {
            m_captureSession?.close()
            m_captureSession = null
        }
    }

    // 録画停止
    private fun stopRecordingVideo() {
        // 録画停止
        m_mediaRecorder?.apply {
            stop()
            reset()
        }

        Log.d("PhotoVideoQrCamera", ("Create Completed : " + m_outputFileName))
        Toast.makeText(this, ("Create Completed : " + m_outputFileName), Toast.LENGTH_SHORT).show()
        // 出力ファイル名
        m_outputFileName = null

        // プレビュー開始
        startPreview()
        // 撮影中フラグ登録
        setIsRecordingFlag(false)
    }

    // メディアレコーダー初期化
    private fun setUpMediaRecorder(): Boolean {
        var ret: Boolean = false

        try {
            // 出力ファイル名をチェック
            if (m_outputFileName.isNullOrEmpty()) {
                // ファイル保存ディレクトリ
                var saveDir = File(Environment.getExternalStorageDirectory().path, def_videoSaveDir)
                // ファイル保存ディレクトリの存在チェック
                if (saveDir.exists() == false) {
                    // ファイル保存ディレクトリが存在しない場合は作成
                    saveDir.mkdirs()
                }
                // ファイル名に現在日時をセット
                var fileName = (LocalDateTime.now().toString() + ".mp4")
                // 出力ファイル名登録
                m_outputFileName = File(saveDir, fileName).toString()
            }

            // 画面の回転設定取得
            val rotation = windowManager.defaultDisplay.rotation
            // センサーの向きをチェック
            when (m_sensorOrientation) {
                SENSOR_ORIENTATION_DEFAULT_DEGREES -> {
                    m_mediaRecorder?.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation))
                }
                SENSOR_ORIENTATION_INVERSE_DEGREES -> {
                    m_mediaRecorder?.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation))
                }
            }

            // パーミッションチェック
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED) {
                throw Exception()
            }

            // メディアレコーダーの設定登録
            m_mediaRecorder?.apply {
                // 音声の入力ソースを指定
                setAudioSource(MediaRecorder.AudioSource.MIC)
                // 録画の入力ソースを指定
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                // ファイルフォーマットを指定
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                // 出力ファイル名を指定
                setOutputFile(m_outputFileName)
                // ビットレート登録
                setVideoEncodingBitRate(10000000)
                // 動画のフレームレートを指定
                setVideoFrameRate(30)
                // 動画のサイズを指定
                setVideoSize(m_videoSize.width, m_videoSize.height)
                // ビデオエンコーダを指定
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                // 音声エンコーダを指定
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                // 録画準備
                prepare()
            }

            ret = true
        } catch (ex: Exception) {
            ret = false
        }

        return ret
    }

    // TextView_06テキストの登録
    fun setTextView_06Text(setStr: String) {
        // テキストビュー取得
        var textView: TextView = findViewById<TextView>(R.id.textView_06)
        textView.text = setStr
    }

    // TextView_07テキストの登録
    fun setTextView_07Text(setStr: String) {
        // テキストビュー取得
        var textView: TextView = findViewById<TextView>(R.id.textView_07)
        textView.text = setStr
    }

    // TextView_08テキストの登録
    fun setTextView_08Text(setStr: String) {
        // テキストビュー取得
        var textView: TextView = findViewById<TextView>(R.id.textView_08)
        textView.text = setStr
    }

    // TextView_08表示切り替え
    fun setTextView_08Visibility(visibility: Int) {
        // テキストビュー取得
        var textView: TextView = findViewById<TextView>(R.id.textView_08)
        textView.visibility = visibility
    }

    // Button_20テキストの登録
    fun setButton_20Text(setStr: String) {
        // ボタン取得
        var button: Button = findViewById<Button>(R.id.button_20)
        button.text = setStr
    }

    // Button_22テキストの登録
    fun setButton_22Text(setStr: String) {
        // ボタン取得
        var button: Button = findViewById<Button>(R.id.button_22)
        button.text = setStr
    }

    // Button_22表示切り替え
    fun setButton_22Visibility(visibility: Int) {
        // ボタン取得
        var button: Button = findViewById<Button>(R.id.button_22)
        button.visibility = visibility
    }

    // Button_22背景色登録
    fun setButton_22BackgroundColor(backgroundColor: Int) {
        // ボタン取得
        var button: Button = findViewById<Button>(R.id.button_22)
        button.setBackgroundColor(backgroundColor)
    }

    // 「戻る」ボタンクリック
    fun onClickButton_19(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_19", buttonText.toString())

        // ボタンのテキストを判定
        when (buttonText) {
            "戻る" -> {
                finish()
            }
        }
    }

    // 「カメラ起動」ボタンクリック
    fun onClickButton_20(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_20", buttonText.toString())

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

    // 「カメラ切替」ボタンクリック
    fun onClickButton_21(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_21", buttonText.toString())
        var index: Int = 0
        // 選択アイテムINDEX
        var selectIndex: Int = m_settingData?.getCameraIndex()!!
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
                    .setSingleChoiceItems(cameraNameList, m_settingData?.getCameraIndex()!!, { dialog, which ->
                        // アイテム選択
                        // 選択アイテムINDEX更新
                        selectIndex = which
                    })
                    .setPositiveButton("OK", { dialog, which ->
                        // 決定ボタン押下
                        // カレントカメラINDEXと選択アイテムINDEXを比較
                        if (m_settingData?.getCameraIndex() != selectIndex) {
                            // カレントカメラINDEXを更新
                            m_settingData?.setCameraIndex(selectIndex)
                            // カメラクローズ
                            closeCamera()
                            // 撮影中フラグ登録
                            setIsRecordingFlag(false)
                            // カメラオープン
                            openCamera()
                        }
                    })
                    .show()
            }
        }
    }

    // 「動画撮影」ボタンクリック
    fun onClickButton_22(view: View) {
        // カレントカメラモードをチェック
        when (m_settingData?.getCameraMode()) {
            def_cameraModePhoto -> {
                // 写真撮影
                photoShooting(Bitmap.CompressFormat.JPEG)
            }
            def_cameraModeVideo -> {
                // 撮影中フラグをチェック
                when (m_isRecordingFlag) {
                    true -> {
                        // 録画停止
                        stopRecordingVideo()
                    }
                    false -> {
                        // 録画開始
                        startRecordingVideo()
                    }
                }
            }
        }
    }

    // 「モード切替」ボタンクリック
    fun onClickButton_23(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_23", buttonText.toString())
        // 選択アイテムINDEX
        var selectIndex: Int = m_settingData?.getCameraMode()!!
        // モード名リスト
        var modeNameList: Array<String?> = arrayOf("写真撮影", "動画撮影", "QRコード")

        // ボタンのテキストを判定
        when (buttonText) {
            "モード切替" -> {
                // モード選択ダイアログ表示
                AlertDialog.Builder(this)
                    .setTitle("モード選択")
                    .setSingleChoiceItems(modeNameList, m_settingData?.getCameraMode()!!, { dialog, which ->
                        // アイテム選択
                        // 選択アイテムINDEX更新
                        selectIndex = which
                    })
                    .setPositiveButton("OK", { dialog, which ->
                        // 決定ボタン押下
                        // カレントカメラモードと選択アイテムINDEXを比較
                        if (m_settingData?.getCameraMode() != selectIndex) {
                            // カメラクローズ
                            closeCamera()
                            // カレントカメラモードを更新
                            m_settingData?.setCameraMode(selectIndex)
                            // 撮影中フラグ登録
                            setIsRecordingFlag(false)
                            // カメラオープン
                            openCamera()
                        }
                    })
                    .show()
            }
        }
    }

    // 撮影中フラグ登録
    fun setIsRecordingFlag(recording: Boolean) {
        // 撮影中フラグを更新
        m_isRecordingFlag = recording
        // コンポーネントのセットアップ
        setupComponent()
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
                if (createPhotoFile(saveFile, m_textureView, format) == true) {
                    Log.d("PhotoVideoQrCamera", ("Create Completed : " + fileName))
                    Toast.makeText(this, ("Create Completed : " + fileName), Toast.LENGTH_SHORT).show()
                } else {
                    msg = "写真ファイル作成エラー"
                    throw Exception(msg)
                }
            } else {
                throw Exception(msg)
            }
        } catch (ex: Exception) {
            Log.d("PhotoVideoQrCamera", ("photoShooting -> " + msg))
            Toast.makeText(this, ("PhotoVideoQrCamera::photoShooting -> " + msg), Toast.LENGTH_SHORT).show()
        } finally {
            // プレビューの更新を再開
            m_captureSession?.setRepeatingRequest(m_previewRequestBuilder.build(), null, null)
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