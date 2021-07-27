package com.example.gooreumtv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class Toolbox {
    companion object {
        fun convertBitmapToString(bitmap: Bitmap?): String {
            // 바이트 배열을 차례대로 읽어 들이는 클래스
            val stream = ByteArrayOutputStream()

            bitmap?.compress(Bitmap.CompressFormat.PNG, 70, stream) // 비트맵을 quality%로 압축
            val bytes = stream.toByteArray() // 비트맵을 바이트 배열로 바꾼다.

            return Base64.encodeToString(bytes, Base64.DEFAULT)
        }

        fun convertStringToBitmap(string: String?): Bitmap {
            // 문자열로 변환된 이미지를 Base 64 방식으로 인코딩하여 byte 배열로 만든다.
            val bytes = Base64.decode(string, Base64.DEFAULT)

            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        fun convertImageUriToBitmap(context: Context, uri: Uri): Bitmap? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source: ImageDecoder.Source = ImageDecoder.createSource(
                    context.contentResolver,
                    uri
                )
                ImageDecoder.decodeBitmap(source)
            } else {
                null
            }
        }

        fun getCurrentDatetime(): String? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val now = LocalDateTime.now()
                val formatter = DateTimeFormatter.ISO_DATE_TIME

                now.format(formatter)
            } else {
                val format = SimpleDateFormat("yyyy-mm-ddThh:mm:ss")
                //TimeZone  설정 (GMT +9)
//            format.timeZone = TimeZone.getTimeZone("Asia/Seoul")

                format.format(Date().time)
            }
        }

        fun extractThumbnailFromVideo(context: Context, uri: Uri): Bitmap? {
            var retriever: MediaMetadataRetriever? = null
            var bitmap: Bitmap? = null
            try {
                retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                // timeUs는 마이크로 초 이므로 1000000초 곱해줘야 초단위다.
                bitmap = retriever.getFrameAtTime(
                    1000000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever?.release()
            }
            return bitmap
        }
    }
}