package com.example.testapp_003

import android.content.Context
import android.content.SharedPreferences

// 設定データクラス
class SettingData {
    // チェックフラグ
    public var m_checkFlag: Boolean = false
    // 共有設定オブジェクト
    private lateinit var m_sharedPreferences: SharedPreferences

    // カメラINDEX
    private var m_cameraIndex: Int = 0
    // カメラモード
    private var m_cameraMode: Int = 0

    // コンストラクタ
    constructor(context: Context) {
        try {
            // インスタンス取得
            m_sharedPreferences = context.getSharedPreferences("settingData", Context.MODE_PRIVATE)

            // カメラINDEX
            m_cameraIndex = m_sharedPreferences.getInt("cameraIndex", 0)
            // カメラモード
            m_cameraMode = m_sharedPreferences.getInt("cameraMode", 0)

            m_checkFlag = true
        } catch (ex: Exception) {
            m_checkFlag = false
        }
    }

    // カメラINDEX取得
    fun getCameraIndex(): Int {
        return m_cameraIndex
    }

    // カメラINDEX登録
    fun setCameraIndex(value: Int) {
        if (m_cameraIndex != value) {
            m_cameraIndex = value
            m_sharedPreferences.edit().putInt("cameraIndex", m_cameraIndex).apply()
        }
    }

    // カメラモード取得
    fun getCameraMode(): Int {
        return m_cameraMode
    }

    // カメラモード登録
    fun setCameraMode(value: Int) {
        if (m_cameraMode != value) {
            m_cameraMode = value
            m_sharedPreferences.edit().putInt("cameraMode", m_cameraMode).apply()
        }
    }

}