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
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
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

//        fun resizeUriToByteArray(context: Context, uri: Uri, targetWidth: Int, targetHeight: Int): ByteArray {
//            val bitmap  = imageUriToBitmap(context, uri)
//            val resized = resizeBitmap(bitmap!!, targetWidth, targetHeight)
//            return bitmapToByteArray(resized)
//        }





        fun videoUriToByteArray(uri: Uri?): ByteArray? {
            val fileStream = FileInputStream(File(uri.toString()))
            val byteStream = ByteArrayOutputStream()
            val buf = ByteArray(1024)

            var n: Int
            while (-1 != fileStream.read(buf).also { n = it })
                byteStream.write(buf, 0, n)

            return byteStream.toByteArray()
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





        fun getCurrentDatetime(): String? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val now = LocalDateTime.now()
//                val formatter = DateTimeFormatter.ISO_DATE_TIME
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                now.format(formatter)
            } else {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                //TimeZone  설정 (GMT +9)
//            format.timeZone = TimeZone.getTimeZone("Asia/Seoul")

                format.format(Date().time)
            }
        }
        private fun getCurrentDate(): String? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val now = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                now.format(formatter)
            } else {
                val format = SimpleDateFormat("yyyy-MM-dd")
                format.format(Date().time)
            }
        }
        private fun getCurrentTime(): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val now = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                now.format(formatter)
            } else {
                val format = SimpleDateFormat("HH:mm:ss")
                format.format(Date().time)
            }
        }
        fun getRelativeDatetime(datetime: String): String {
            val currentDate = getCurrentDate()
            val date = datetime.subSequence(0, 10)

            if (currentDate == date) {
                val currentTime = getCurrentTime().split(":")
                val time = datetime.subSequence(11, 19).split(":")

                val currentSec  = (currentTime[0].toInt() * 3600) + (currentTime[1].toInt() * 60) + currentTime[2].toInt()
                val sec = (time[0].toInt() * 3600) + (time[1].toInt() * 60) + time[2].toInt()

                val diff = currentSec - sec
                // 5분 이내에 올라온 컨텐츠는 '방금'으로 표기
                return if (diff <= 300) {
                    "방금"
                } else {
                    // 1시간 이내에 올라온 컨텐츠는 'n분 전'으로 표기
                    if (diff < 3600) {
                        val min = (currentSec / 60) - (sec / 60)
                        min.toString() + "분 전"
                    } else {
                        // 나머지는 'n시간 전'으로 표기
                        val hour = (currentSec / 3600) - (sec / 3600)
                        hour.toString() + "시간 전"
                    }
                }
            } else {
                // 지금과 날짜가 다르면(어제 이전에 올라온 컨텐츠일 경우) 날짜 그대로 표기
                // 2021-08-05로 저장된 것을 2021/08/05로 구분자만 살짝 바꿔준다.
                val temp = date.subSequence(0, 10).split("-")
                return temp[0] + "/" + temp[1] + "/" + temp[2]
            }
        }





        private fun _getCurrentDate(): String? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val now = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
                now.format(formatter)
            } else {
                val format = SimpleDateFormat("yyyyMMdd")
                format.format(Date().time)
            }
        }
        /**
         * 임의로 파일 이름을 생성한다. ex) 20210803214597
         */
        fun createFilename(): String {
            val random = (Random().nextInt(90) + 10).toString() +
                    (Random().nextInt(90) + 10).toString() +
                    (Random().nextInt(90) + 10).toString()
            return _getCurrentDate() + random
        }

        fun makeToast(context: Context, text: String) {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }

        /**
         * @param view 현재 포커스 된 뷰
         */
        fun hideKeyboard(context: FragmentActivity, view: View) {
            val imm: InputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        /**
         * @param ui 프로그래스 바가 보여질 때 가릴 뷰
         * @param visible 프로그래스 바 보이기/가리기
         */
        fun showProgressBar(ui: View, progressBar: View, visible: Boolean) {
            if (visible) {
                ui.visibility = View.INVISIBLE
                progressBar.visibility = View.VISIBLE
            } else {
                ui.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
            }
        }

        fun showErrorMessage(context: Context, log: String?, toast: String?) {
            if (log != null) {
                Log.e(MainActivity.TAG, "$log")
            }
            if (toast != null) {
                makeToast(context, toast)
            }
        }





        fun initDefaultParams() : RelativeLayout.LayoutParams {
            return RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }
}