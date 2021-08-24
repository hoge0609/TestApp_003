package com.example.testapp_003

import android.content.Context

// アプリケーションコンテキストクラス
public class AppContext {
    // static変数・関数定義
    companion object {
        // インスタンス
        private var m_instance: AppContext? = null

        // アプリケーションコンテキスト登録
        public fun setAppContext(appContext: Context) {
            // コンストラクタ呼び出し
            m_instance = AppContext(appContext)
        }

        // インスタンス取得
        public fun getInstance(): AppContext? {
            return AppContext.m_instance
        }

        // アプリケーションコンテキスト取得
        public fun getAppContext(): Context? {
            return AppContext.getInstance()?.m_appContext
        }
    }

    // アプリケーションコンテキスト
    private var m_appContext: Context? = null

    // コンストラクタ
    constructor(appContext: Context) {
        this.m_appContext = appContext
    }

}