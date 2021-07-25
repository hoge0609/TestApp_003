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
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

// 写真・動画カメラクラス
class PhotoVideoCamera : AppCompatActivity() {
    // テクスチャビュー
    private lateinit var m_textureView: TextureView
    // カメラマネージャー
    private lateinit var m_cameraManager: CameraManager
    // カメラデータクラスリスト
    private lateinit var m_cameraDataList: Array<CameraData?>
    // カレントカメラINDEX
    private var m_currentCameraIndex: Int = 0

    // カメラデバイス
    private var m_cameraDevice: CameraDevice? = null

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
    // カレントカメラモード
    private var m_currentCameraMode: Int = def_cameraModePhoto

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
        setContentView(R.layout.activity_photo_video_camera)
        Log.d("PhotoVideoCamera", "onCreate")
        Toast.makeText(this, "PhotoVideoCamera::onCreate", Toast.LENGTH_SHORT).show()

        // コンポーネント初期化
        this.initComponent()
    }

    // アクティビティ破棄
    override fun onDestroy() {
        super.onDestroy()
        Log.d("PhotoVideoCamera", "onDestroy")
        Toast.makeText(this, "PhotoVideoCamera::onDestroy", Toast.LENGTH_SHORT).show()

        // カメラクローズ
        closeCamera()
    }

    // アクティビティ再開
    override fun onResume() {
        super.onResume()
        Log.d("PhotoVideoCamera", "onResume")
        Toast.makeText(this, "PhotoVideoCamera::onResume", Toast.LENGTH_SHORT).show()

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
            // 撮影中フラグ登録
            this.setIsRecordingFlag(false)

            // テクスチャビュー初期化
            m_textureView = findViewById<TextureView>(R.id.textureView_03)
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
            // カレントカメラINDEX
            m_currentCameraIndex = 0

            ret = true
        } catch (ex: Exception) {
            Log.d("PhotoVideoCamera", ("initComponent -> " + msg))
            Toast.makeText(this, ("PhotoVideoCamera::initComponent -> " + msg), Toast.LENGTH_SHORT).show()
            ret = false
        }
        if (ret == false) {
            // アクティビティ終了
            finish()
        }

        return ret
    }

    // カメラオープン
    private fun openCamera() {
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
            cameraData = m_cameraDataList.get(m_currentCameraIndex)
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
                        // Button_13テキストの登録
                        setButton_13Text("カメラ終了")
                        // TextView_03テキストの登録
                        setTextView_03Text(m_cameraDataList.get(m_currentCameraIndex)?.m_cameraName.toString())
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
            Log.d("PhotoVideoCamera", ("openCamera -> " + msg))
            Toast.makeText(this, ("PhotoVideoCamera::openCamera -> " + msg), Toast.LENGTH_SHORT).show()
        }
    }

    // カメラクローズ
    private fun closeCamera() {
        var msg: String = "カメラクローズエラー"

        try {
            // プレビューセッションクローズ
            closePreviewSession()
            m_cameraDevice?.close()
            m_cameraDevice = null
            m_mediaRecorder?.release()
            m_mediaRecorder = null
            // Button_13テキストの登録
            setButton_13Text("カメラ起動")
        } catch (ex: Exception) {
            Log.d("PhotoVideoCamera", ("closeCamera -> " + msg))
            Toast.makeText(this, ("PhotoVideoCamera::closeCamera -> " + msg), Toast.LENGTH_SHORT).show()
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
            Log.d("PhotoVideoCamera", ("startPreview -> " + msg))
            Toast.makeText(this, ("PhotoVideoCamera::startPreview -> " + msg), Toast.LENGTH_SHORT).show()
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
            Log.d("PhotoVideoCamera", ("updatePreview -> " + msg))
            Toast.makeText(this, ("PhotoVideoCamera::updatePreview -> " + msg), Toast.LENGTH_SHORT).show()
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
            Log.d("PhotoVideoCamera", ("startRecordingVideo -> " + msg))
            Toast.makeText(this, ("PhotoVideoCamera::startRecordingVideo -> " + msg), Toast.LENGTH_SHORT).show()
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

        Log.d("PhotoVideoCamera", ("Create Completed : " + m_outputFileName))
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
            val rotation = this.windowManager.defaultDisplay.rotation
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

    // TextView_03テキストの登録
    fun setTextView_03Text(setStr: String) {
        // テキストビュー取得
        var textView: TextView = findViewById<TextView>(R.id.textView_03)
        textView.text = setStr
    }

    // TextView_04テキストの登録
    fun setTextView_04Text() {
        var setStr: String = ""
        // テキストビュー取得
        var textView: TextView = findViewById<TextView>(R.id.textView_04)

        when (m_currentCameraMode) {
            def_cameraModePhoto -> {
                setStr = "写真カメラ"
            }
            def_cameraModeVideo -> {
                setStr = "ビデオカメラ"
            }
            else -> {
                setStr = ""
            }
        }
        textView.text = setStr
    }

    // Button_13テキストの登録
    fun setButton_13Text(setStr: String) {
        // ボタン取得
        var button: Button = findViewById<Button>(R.id.button_13)
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
    fun onClickButton_12(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_12", buttonText.toString())

        // ボタンのテキストを判定
        when (buttonText) {
            "戻る" -> {
                this.finish()
            }
        }
    }

    fun onClickButton_13(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_13", buttonText.toString())

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

    fun onClickButton_14(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_14", buttonText.toString())
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

    fun onClickButton_15(view: View) {
        // カレントカメラモードをチェック
        when (m_currentCameraMode) {
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

    fun onClickButton_16(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_16", buttonText.toString())
        // 選択アイテムINDEX
        var selectIndex: Int = m_currentCameraMode
        // モード名リスト
        var modeNameList: Array<String?> = arrayOf("写真撮影", "動画撮影")

        // ボタンのテキストを判定
        when (buttonText) {
            "モード切替" -> {
                // モード選択ダイアログ表示
                AlertDialog.Builder(this)
                    .setTitle("モード選択")
                    .setSingleChoiceItems(modeNameList, m_currentCameraMode, { dialog, which ->
                        // アイテム選択
                        // 選択アイテムINDEX更新
                        selectIndex = which
                    })
                    .setPositiveButton("OK", { dialog, which ->
                        // 決定ボタン押下
                        // カレントカメラモードと選択アイテムINDEXを比較
                        if (m_currentCameraMode != selectIndex) {
                            // カメラクローズ
                            closeCamera()
                            // カレントカメラモードを更新
                            m_currentCameraMode = selectIndex
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
        // ボタン取得
        var button: Button = findViewById<Button>(R.id.button_15)
        // 撮影中フラグを更新
        m_isRecordingFlag = recording

        // カレントカメラモードをチェック
        when (m_currentCameraMode) {
            def_cameraModePhoto -> {
                // テキスト
                button.text = "写真撮影"
                // 背景色（待機中）
                button.setBackgroundColor(def_waitingButtonColor)
            }
            def_cameraModeVideo -> {
                when (m_isRecordingFlag) {
                    true -> {
                        // テキスト
                        button.text = "撮影終了"
                        // 背景色（撮影中）
                        button.setBackgroundColor(def_recordingButtonColor)
                    }
                    false -> {
                        // テキスト
                        button.text = "動画撮影"
                        // 背景色（待機中）
                        button.setBackgroundColor(def_waitingButtonColor)
                    }
                }
            }
        }
        // TextView_04テキストの登録
        setTextView_04Text()
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
                    Log.d("PhotoVideoCamera", ("Create Completed : " + fileName))
                    Toast.makeText(this, ("Create Completed : " + fileName), Toast.LENGTH_SHORT).show()
                } else {
                    msg = "写真ファイル作成エラー"
                    throw Exception(msg)
                }
            } else {
                throw Exception(msg)
            }
        } catch (ex: Exception) {
            Log.d("PhotoVideoCamera", ("photoShooting -> " + msg))
            Toast.makeText(this, ("PhotoVideoCamera::photoShooting -> " + msg), Toast.LENGTH_SHORT).show()
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