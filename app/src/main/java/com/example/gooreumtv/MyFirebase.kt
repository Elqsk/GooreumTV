package com.example.gooreumtv

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import java.net.URL

// 서버 DB에 저장된 데이터(사용자 정보와 파일)를 편하게 불러오기 위한 클래스
class MyFirebase {
    companion object {
        private const val COLLECTION_USER = "users"

        /**
         * "users" 컬렉션 객체를 얻어온다.
         */
        private fun getUserCollection(): CollectionReference {
            return Firebase.firestore.collection(COLLECTION_USER)
        }

        fun getMyData(uid: String): Task<DocumentSnapshot> {
            return Firebase.firestore.collection(COLLECTION_USER).document(uid).get()
        }

        /**
         * Storage에 접근하려면 Firebase 인증을 거쳐야 한다.
         */
        fun authenticate(email: String, password: String): Task<AuthResult> {
            return Firebase.auth
                .createUserWithEmailAndPassword(email, password)
        }

        fun findUserWithEmail(email: String): Task<QuerySnapshot> {
            // 사용할 수 있는 계정인지 검사한다. 이미 존재하는 계정은 사용할 수 없다.
            // 'email' 필드의 데이터가 '사용자가 입력한 이메일'과 일치하는 문서를 검색한다.
            return getUserCollection().whereEqualTo("email", email).get()
        }

        fun findUserWithUid(uid: String): Task<DocumentSnapshot> {
            return getUserCollection().document(uid).get()
        }

        /**
         * 새로 가입한 사용자의 데이터를 추가한다.
         */
        fun signUp(data: Any): Task<DocumentReference> {
            return getUserCollection().add(data)
        }

        fun signIn(email: String, password: String): Task<AuthResult> {
            return FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
        }





        fun uploadFileWithByteArray(bytes: ByteArray, filename: String): UploadTask {
            return Firebase.storage
                .reference
                .child(filename)
                .putBytes(bytes)
        }

        fun uploadFileWithUri(uri: Uri, filename: String): UploadTask {
            return Firebase.storage
                .reference
                .child(filename)
                .putFile(uri)
        }

        /**
         * 여기서의 path는 서버 스토리지 내의 경로를 말한다. ex) images/20210803456710.jpeg
         */
        fun downloadFileWithPath(path: String?): Task<ByteArray>? {
            val maxSize: Long = Long.MAX_VALUE
            return if (path != null)
                Firebase.storage
                    .reference
                    .child(path)
                    .getBytes(maxSize)
            else null
        }

        fun downloadUrl(filename: String?): Task<Uri>? {
            return if (filename != null)
                Firebase.storage
                    .reference
                    .child(filename)
                    .downloadUrl
            else null
        }





        private val videoCollection = Firebase.firestore.collection("videos")
        private val videoOrdered    = videoCollection.orderBy("uid", Query.Direction.DESCENDING)

        fun uploadVideoWithAutoIndex(data: Any): Task<DocumentReference> {
            return videoCollection.add(data)
        }

        fun uploadVideo(uid: String, data: Any): Task<Void> {
            return videoCollection.document(uid).set(data)
        }

        fun getVideos(): Task<QuerySnapshot> {
            return videoOrdered.get()
        }

        fun getMyVideos(userUid: String): Task<QuerySnapshot> {
            return videoOrdered.whereEqualTo("user", userUid).get()
        }

        /**
         * 기존 동영상의 uid를 얻어오는 것이 아닌, 새로 저장할 동영상의 uid를 구한다.
         */
        fun getNextVideoUid(): Int? {
            return videoCollection.get().result?.size()?.plus(1)
        }
    }
}