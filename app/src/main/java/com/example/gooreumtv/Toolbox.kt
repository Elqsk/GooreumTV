package com.example.gooreumtv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class Toolbox {
    companion object {
        /**
         * Bitmap을 이진 문자열로 변환한다.
         */
        fun bitmapToString(bitmap: Bitmap?): String {
            val bytes = bitmapToByteArray(bitmap)
            return Base64.encodeToString(bytes, Base64.DEFAULT)
        }

        fun bitmapToByteArray(bitmap: Bitmap?): ByteArray? {
            val stream = ByteArrayOutputStream() // 바이트 배열을 차례대로 읽어 들인다.
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 50, stream) // 비트맵을 quality%로 압축

            return stream.toByteArray()
        }

        fun imageUriToByteArray(context: Context, uri: Uri?): ByteArray? {
            val bitmap = imageUriToBitmap(context, uri)
            return bitmapToByteArray(bitmap)
        }

        fun imageUriToBitmap(context: Context, uri: Uri?): Bitmap? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (uri != null) {
                    val source: ImageDecoder.Source = ImageDecoder.createSource(
                        context.contentResolver,
                        uri
                    )
                    ImageDecoder.decodeBitmap(source)
                } else null
            } else null
        }

        fun byteArrayToBitmap(bytes: ByteArray?): Bitmap? {
            return if (bytes != null) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } else
                null
        }

        /**
         * 이진 문자열로 변환된 이미지를 다시 Bitmap으로 변환한다.
         */
        fun stringToBitmap(string: String?): Bitmap {
            val bytes = Base64.decode(string, Base64.DEFAULT)

            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        fun resizeBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap? {
            val width = bitmap.width
            val height = bitmap.height

            val sx = targetWidth.toFloat() / width
            val sy = targetHeight.toFloat() / height

            val matrix = Matrix()
            // postScale() 안에 들어가는 파라미터들은 증감 비율이다. 마이너스가 들어가면 해당 수치 만큼 이미지의
            // 크기가 작아진다.
            matrix.postScale(sx, sy)

            return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
        }

//        fun resizeUriToByteArray(context: Context, uri: Uri, targetWidth: Int, targetHeight: Int): ByteArray {
//            val bitmap  = imageUriToBitmap(context, uri)
//            val resized = resizeBitmap(bitmap!!, targetWidth, targetHeight)
//            return bitmapToByteArray(resized)
//        }

        fun getCurrentDatetime(): String? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val now = LocalDateTime.now()
//                val formatter = DateTimeFormatter.ISO_DATE_TIME
                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

                now.format(formatter)
            } else {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                //TimeZone  설정 (GMT +9)
//            format.timeZone = TimeZone.getTimeZone("Asia/Seoul")

                format.format(Date().time)
            }
        }

        const val JPEG = ".jpeg"
        const val MP4 = ".mp4"

        /**
         * 임의로 파일 이름을 생성한다.
         */
        fun createFilename(extension: String): String {
            val random = (Random().nextInt(90) + 10).toString() +
                    (Random().nextInt(90) + 10).toString() +
                    (Random().nextInt(90) + 10).toString()
            return getCurrentDatetime() + random + extension
        }

        fun makeToast(context: Context, text: String) {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }

        fun hideKeyboard(context: FragmentActivity, view: View) {
            val imm: InputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
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