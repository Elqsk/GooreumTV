package com.example.gooreumtv

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity

// 로컬에 저장된 현재 사용자의 정보를 편하게 불러오기 위한 클래스
class CurrentUser {
    companion object {
        private const val PREFS_NAME = "current_user"
        private const val PREFS_KEY_UID = "uid"

        private fun get(context: Context): SharedPreferences? {
            return context.getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
        }
        fun signIn(context: Context, uid: String) {
            if (get(context) != null) {
                val editor = get(context)!!.edit()
                editor.putString(PREFS_KEY_UID, uid)
                editor.apply()
            }
        }
        fun getUid(context: Context): String? {
            return if (get(context) != null) {
                if (get(context)!!.contains(PREFS_KEY_UID)) {
                    get(context)!!.getString(PREFS_KEY_UID, null).toString()
                } else {
                    null
                }
            } else {
                null
            }
        }
        fun signOut(context: Context) {
            if (get(context) != null) {
                val editor = get(context)!!.edit()
                editor.remove(PREFS_KEY_UID)
                editor.apply()
            }
        }
    }
}