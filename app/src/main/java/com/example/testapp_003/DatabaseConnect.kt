package com.example.testapp_003

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Serializable
data class Product(                 // 製品マスタ
    val product_id: Int,
    val product_name: String
)

@Serializable
data class Parts(                   // 部品マスタ
    val parts_id: Int,
    val parts_name: String
)

@Serializable
data class ProductStructure(        // 製品構成マスタ
    val product_id: Int,
    val parts_id: Int,
    val parts_count: Int
)

@Serializable
data class User(                    // ユーザーマスタ
    val user_id: Int,
    val user_name: String
)

@Serializable
data class Device(                  // デバイスマスタ
    val device_id: Int,
    val device_name: String,
    val user_id: Int,
    val mac_address: String
)

@Serializable
data class InventoryRegistType(     // 在庫登録種別マスタ
    val inventory_regist_type_id: Int,
    val inventory_regist_type_name: String
)

@Serializable
data class PartsInventory(          // 部品在庫テーブル
    val parts_id: Int,
    val parts_inventory_count: Int
)

@Serializable
data class PartsInventoryHistory(   // 部品在庫履歴テーブル
    val parts_inventory_history_id: Int,
    val parts_id: Int,
    val product_id: Int?,   // データベース上でNULLを許可
    val device_id: Int?,    // データベース上でNULLを許可
    val inventory_regist_type_id: Int,
    val variable_count: Int,
    val created_by: Int,
    val create_at: String
)

class DatabaseConnect : AppCompatActivity() {
    // テーブルデータINDEX
    var m_tableDataIndex: Int = 0

    // テーブル名リスト
    var m_tableNameList: Array<String?> = arrayOf(
        "m_product",
        "m_parts",
        "m_product_structure",
        "m_user",
        "m_device",
        "m_inventory_regist_type",
        "t_parts_inventory",
        "t_parts_inventory_history"
    )

    // ベースURL
    val m_baseUrl: String = "http://hoge0609.php.xdomain.jp/index.php/"
    // 末尾URL
    val m_endUrl: String = "/get_all/"

    // アクティビティ作成
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database_connect)

        Log.d("DatabaseConnect", "onCreate")
//        MessageUtils.toast("DatabaseConnect::onCreate")

        // コンポーネント初期化
        initComponent()
    }

    // アクティビティ再開
    override fun onResume() {
        super.onResume()

        Log.d("DatabaseConnect", "onResume")
//        MessageUtils.toast("DatabaseConnect::onResume")
    }

    // アクティビティ破棄
    override fun onDestroy() {
        super.onDestroy()

        Log.d("DatabaseConnect", "onDestroy")
//        MessageUtils.toast("DatabaseConnect::onDestroy")
    }

    // コンポーネント初期化
    fun initComponent(): Boolean {
        var ret: Boolean = false
        var msg: String = "コンポーネント初期化エラー"

        try {
            // テーブルデータINDEX登録
            setTableDataIndex(m_tableDataIndex)

            ret = true
        } catch (ex: Exception) {
            Log.d("DatabaseConnect", ("initComponent -> " + msg))
            MessageUtils.toast("DatabaseConnect::initComponent -> " + msg)
            ret = false
        } finally {
            if (ret == false) {
                // アクティビティ終了
                finish()
            }
        }

        return ret
    }

    // 「戻る」ボタンクリック
    fun onClickButton_24(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_24", buttonText.toString())

        // ボタンのテキストを判定
        when (buttonText) {
            "戻る" -> {
                finish()
            }
        }
    }

    // 「テーブル選択」ボタンクリック
    fun onClickButton_25(view: View) {
        // ボタンのテキストを取得
        var buttonText = (view as Button).text
        Log.d("button_25", buttonText.toString())
        // 選択アイテムINDEX
        var selectIndex: Int = m_tableDataIndex

        // ボタンのテキストを判定
        when (buttonText) {
            "テーブル選択" -> {
                // テーブル選択ダイアログ表示
                AlertDialog.Builder(this)
                    .setTitle("テーブル選択")
                    .setSingleChoiceItems(m_tableNameList, m_tableDataIndex, { dialog, which ->
                        // アイテム選択
                        // 選択アイテムINDEX更新
                        selectIndex = which
                    })
                    .setPositiveButton("OK", { dialog, which ->
                        // 決定ボタン押下
                        // テーブルデータINDEXと選択アイテムINDEXを比較
                        if (m_tableDataIndex != selectIndex) {
                            // テーブルデータINDEX登録
                            setTableDataIndex(selectIndex)
                        }
                    })
                    .show()
            }
        }
    }

    // 「データ取得」ボタンクリック
    fun onClickButton_26(view: View) {
        // データベース接続
        connect()
    }

    // TextView_09テキストの登録
    fun setTextView_09Text(setStr: String) {
        // テキストビュー取得
        var textView: TextView = findViewById<TextView>(R.id.textView_09)
        textView.text = setStr
    }

    // テーブルデータINDEX登録
    private fun setTableDataIndex(index: Int) {
        m_tableDataIndex = index
        // テーブル名を登録
        // TextView_09テキストの登録
        setTextView_09Text(m_tableNameList[m_tableDataIndex].toString())
    }

    // データベース接続
    private fun connect() {
        var msg: String = "データベース接続エラー"
        var outputStr: String = ""
        var connection: HttpURLConnection? = null
        var dataStr: String
        // 接続テーブル名
        var targetTable: String = m_tableNameList[m_tableDataIndex].toString()

        // 非同期処理
        GlobalScope.launch {
            val job = launch {
                try {
                    connection = URL(m_baseUrl + targetTable + m_endUrl).openConnection() as HttpURLConnection
                    connection?.connectTimeout = 1000
                    connection?.readTimeout = 1000
                    connection?.useCaches = false
                    connection?.requestMethod = "GET"
                    // 接続
                    connection?.connect()
                    // データ取得
                    dataStr = BufferedReader(InputStreamReader(connection?.inputStream)).readLines()[0].toString()

                    when (targetTable) {
                        "m_product" -> {
                            // 製品マスタ
                            outputStr = getProduct(Json.decodeFromString<List<Product>>(dataStr))
                        }
                        "m_parts" -> {
                            // 部品マスタ
                            outputStr = getParts(Json.decodeFromString<List<Parts>>(dataStr))
                        }
                        "m_product_structure" -> {
                            // 製品構成マスタ
                            outputStr = getProductStructure(Json.decodeFromString<List<ProductStructure>>(dataStr))
                        }
                        "m_user" -> {
                            // ユーザーマスタ
                            outputStr = getUser(Json.decodeFromString<List<User>>(dataStr))
                        }
                        "m_device" -> {
                            // デバイスマスタ
                            outputStr = getDevice(Json.decodeFromString<List<Device>>(dataStr))
                        }
                        "m_inventory_regist_type" -> {
                            // 在庫登録種別マスタ
                            outputStr = getInventoryRegistType(Json.decodeFromString<List<InventoryRegistType>>(dataStr))
                        }
                        "t_parts_inventory" -> {
                            // 部品在庫テーブル
                            outputStr = getPartsInventory(Json.decodeFromString<List<PartsInventory>>(dataStr))
                        }
                        "t_parts_inventory_history" -> {
                            // 部品在庫履歴テーブル
                            outputStr = getPartsInventoryHistory(Json.decodeFromString<List<PartsInventoryHistory>>(dataStr))
                        }
                        else -> {
                            throw Exception()
                        }
                    }
                    if (outputStr.length < 1) {
                        throw Exception()
                    }
                } catch (e: Exception) {
                    Log.d("DatabaseConnect", ("connect -> " + msg))
                    outputStr = ("DatabaseConnect::connect -> " + msg)
                } finally {
                    if (connection != null) {
                        connection?.disconnect()
                    }
                }
            }
            // jobの処理完了まで待機
            job.join()
        }
        // 実行中の全スレッドを止める
        Thread.sleep(1000)
        // 処理結果を表示
        MessageUtils.toast(outputStr)
    }

    // 製品マスタ
    private fun getProduct(data: List<Product>): String {
        var ret: String = ""
        try {
            for (obj in data) {
                ret += ("[" + obj.product_id + "]") + ", "
                ret += obj.product_name
                ret += "\n"
            }
        } catch (e: Exception) {
            ret = ""
        }
        return ret
    }

    // 部品マスタ
    private fun getParts(data: List<Parts>): String {
        var ret: String = ""
        try {
            for (obj in data) {
                ret += ("[" + obj.parts_id + "]") + ", "
                ret += obj.parts_name
                ret += "\n"
            }
        } catch (e: Exception) {
            ret = ""
        }
        return ret
    }

    // 製品構成マスタ
    private fun getProductStructure(data: List<ProductStructure>): String {
        var ret: String = ""
        try {
            for (obj in data) {
                ret += ("[" + obj.product_id + "]") + ", "
                ret += ("[" + obj.parts_id + "]") + ", "
                ret += obj.parts_count.toString() + ", "
                ret += "\n"
            }
        } catch (e: Exception) {
            ret = ""
        }
        return ret
    }

    // ユーザーマスタ
    private fun getUser(data: List<User>): String {
        var ret: String = ""
        try {
            for (obj in data) {
                ret += ("[" + obj.user_id + "]") + ", "
                ret += obj.user_name
                ret += "\n"
            }
        } catch (e: Exception) {
            ret = ""
        }
        return ret
    }

    // デバイスマスタ
    private fun getDevice(data: List<Device>): String {
        var ret: String = ""
        try {
            for (obj in data) {
                ret += ("[" + obj.device_id + "]") + ", "
                ret += obj.device_name + ", "
                ret += obj.user_id.toString() + ", "
                ret += obj.mac_address + ", "
                ret += "\n"
            }
        } catch (e: Exception) {
            ret = ""
        }
        return ret
    }

    // 在庫登録種別マスタ
    private fun getInventoryRegistType(data: List<InventoryRegistType>): String {
        var ret: String = ""
        try {
            for (obj in data) {
                ret += ("[" + obj.inventory_regist_type_id + "]") + ", "
                ret += obj.inventory_regist_type_name + ", "
                ret += "\n"
            }
        } catch (e: Exception) {
            ret = ""
        }
        return ret
    }

    // 部品在庫テーブル
    private fun getPartsInventory(data: List<PartsInventory>): String {
        var ret: String = ""
        try {
            for (obj in data) {
                ret += ("[" + obj.parts_id + "]") + ", "
                ret += obj.parts_inventory_count.toString() + ", "
                ret += "\n"
            }
        } catch (e: Exception) {
            ret = ""
        }
        return ret
    }

    // 部品在庫履歴テーブル
    private fun getPartsInventoryHistory(data: List<PartsInventoryHistory>): String {
        var ret: String = ""
        try {
            for (obj in data) {
                ret += ("[" + obj.parts_inventory_history_id + "]") + ", "
                ret += obj.parts_id.toString() + ", "
                ret += obj.product_id.toString() + ", "
                ret += obj.device_id.toString() + ", "
                ret += obj.inventory_regist_type_id.toString() + ", "
                ret += obj.variable_count.toString() + ", "
                ret += obj.created_by.toString() + ", "
                ret += obj.create_at
                ret += "\n"
            }
        } catch (e: Exception) {
            ret = ""
        }
        return ret
    }

}