package com.example.testapp_003

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaCodec
import android.util.Size

// カメラデータクラス
class CameraData {
    // チェックフラグ
    public var m_checkFlag: Boolean = false
    // カメラID
    public var m_cameraId: String = ""
    // カメラ名
    public var m_cameraName: String = ""
    // カメラの向き
    public var m_direction: Int? = null
    // センサーの向き
    public var m_sensorOrientation: Int? = null
    // カメラサイズリスト
    public var m_cameraSizeList: Array<Size?>? = null

    // コンストラクタ
    constructor(cameraId:String, manager: CameraManager) {
        var setName: String = ""
        var count: Int = 0
        var index: Int = 0
        val characteristics: CameraCharacteristics

        try {
            // カメラID登録
            m_cameraId = cameraId
            setName = ("[" + m_cameraId + "] ")

            // カメラの向きを取得
            characteristics = manager.getCameraCharacteristics(m_cameraId)
            m_direction = characteristics.get(CameraCharacteristics.LENS_FACING)

            when (m_direction) {
                CameraCharacteristics.LENS_FACING_FRONT -> {
                    // インカメラ
                    setName += ("IN CAMERA")
                }
                CameraCharacteristics.LENS_FACING_BACK -> {
                    // アウトカメラ
                    setName += ("OUT CAMERA")
                }
                CameraCharacteristics.LENS_FACING_EXTERNAL -> {
                    // 外部カメラ
                    setName += ("EXTERNAL CAMERA")
                }
                else -> {
                    setName += ("UNKNOWN")
                }
            }
            // カメラ名登録
            m_cameraName = setName
            // センサーの向き
            m_sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

            // コンフィグ取得
            val configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            if (configs == null) {
                throw Exception()
            }
            // サイズデータ取得
            val sizes = configs.getOutputSizes(MediaCodec::class.java)
            if (sizes == null) {
                throw Exception()
            }
            count = sizes.size

            m_cameraSizeList = arrayOfNulls(count)

            for (size in sizes) {
                // カメラサイズリストに登録
                m_cameraSizeList?.set(index, size)
                index ++
            }

            m_checkFlag = true
        } catch (ex: Exception) {
            m_checkFlag = false
        }
    }

}