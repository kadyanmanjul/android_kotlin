package com.joshtalks.joshskills.ui.database

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.Build
import androidx.core.database.getStringOrNull
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.DATABASE_NAME
import kotlinx.coroutines.launch

class DatabaseViewModel(application: Application) : AndroidViewModel(application) {
    private val db: SQLiteDatabase by lazy {
        getApplication<Application>().openOrCreateDatabase(DATABASE_NAME, MODE_PRIVATE, null)
    }
    val isProcessing = ObservableBoolean(false)
    val pairList = MutableLiveData<List<Pair<String, String>>>()

    fun getListOfTablesInDb(text: String? = null) {
        viewModelScope.launch {
            try {
                isProcessing.set(true)
                val temp = mutableListOf<Pair<String, String>>()
                val query = "SELECT name FROM sqlite_master WHERE type='table'${
                    if (text != null)
                        " AND name LIKE '%$text%'"
                    else ""
                }"
                val cursor = db.rawQuery(query, null)
                if (cursor.moveToFirst()) {
                    while (!cursor.isAfterLast) {
                        val tableName = cursor.getString(0)
                        temp.add(Pair(tableName, getRecordCount(tableName).toString()))
                        cursor.moveToNext()
                    }
                }
                cursor.close()
                pairList.postValue(temp)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isProcessing.set(false)
            }
        }
    }

    fun getColumnsInTable(tableName: String): List<String> {
        val cursor = db.rawQuery("SELECT * FROM $tableName", null)
        return cursor.columnNames.toList()
    }

    fun getRecordCount(tableName: String): Int {
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $tableName", null)
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
        return count
    }

    fun getTableData(tableName: String, selectedColumns: List<String>? = null): List<Map<Int, String?>> {
        return if (selectedColumns == null)
            executeQuery("SELECT * FROM $tableName")
        else
            executeQuery("SELECT ${selectedColumns.joinToString(",")} FROM $tableName")
    }

    fun deleteData(tableName: String, columnName: String, value: String) {
        viewModelScope.launch {
            val query: String = "DELETE FROM $tableName WHERE $columnName=$value"
            executeQuery(query)
        }
    }

    fun executeQuery(query: String): List<Map<Int, String?>> {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                db.validateSql(query, null)
            }
        } catch (e: Exception) {
            showToast(e.message.toString())
            return emptyList()
        }
        val cursor = db.rawQuery(query, null)
        val tableData = mutableListOf<Map<Int, String?>>()
        val headerData = HashMap<Int, String?>()
        for (i in 0 until cursor.columnCount) {
            headerData[i] = cursor.getColumnName(i)
        }
        tableData.add(headerData)
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast) {
                val rowData = HashMap<Int, String?>()
                for (i in 0 until cursor.columnCount) {
                    rowData[i] = cursor.getStringOrNull(i)
                }
                tableData.add(rowData)
                cursor.moveToNext()
            }
        }
        cursor.close()
        return tableData.toList()
    }

    @Throws(SQLiteException::class)
    fun validateQuery(query: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            db.validateSql(query, null)
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            try {
                isProcessing.set(true)
                PrefManager.clearDatabase()
                isProcessing.set(false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}