package com.example.testapp_003

import android.content.Context
import android.widget.Toast

// メッセージユーティリティクラス
class MessageUtils {
    // static関数定義
    companion object {
        // コンテキスト登録
        public fun toast(str: String) {
            // アプリケーションコンテキスト取得
            var context: Context? = AppContext.getAppContext()
            Toast.makeText(context, str, Toast.LENGTH_SHORT).show()
        }
    }

}