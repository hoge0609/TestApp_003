package com.example.testapp_003

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaCodec
import android.util.Size

// カメラデータクラス
class CameraData {
    // カメラID
    public var m_cameraId: String = ""
    // カメラ名
    public var m_cameraName: String = ""
    // カメラサイズリスト
    public lateinit var m_cameraSizeList: Array<Size?>

    // コンストラクタ
    constructor(cameraId:String, manager: CameraManager) {
        var setName: String = ""
        var count: Int = 0
        var index: Int = 0
        val characteristics: CameraCharacteristics

        try {
            // カメラID登録
            m_cameraId = cameraId
            setName = ("[" + cameraId + "] ")

            // カメラの向きを取得
            characteristics = manager.getCameraCharacteristics(cameraId)

            when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
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
                m_cameraSizeList.set(index, size)
                index ++
            }
        } catch (ex: Exception) {
        }
    }

}