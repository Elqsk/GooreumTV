package com.example.gooreumtv

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage

// 전체 사용자 정보를 편하게 불러오기 위한 클래스
class MyFirebase {
    companion object {
        private const val COLLECTION_NAME = "users"

        /**
         * "users" 컬렉션 객체를 얻어온다.
         */
        fun getUsers(): CollectionReference {
            return Firebase.firestore
                .collection(COLLECTION_NAME)
        }

        /**
         * Storage에 접근하려면 Firebase에 인증해야 한다.
         */
        fun authenticate(email: String, password: String): Task<AuthResult> {
            return Firebase.auth
                .createUserWithEmailAndPassword(email, password)
        }

        /**
         * 사용할 수 있는 계정인지 검사한다. 이미 존재하는 계정은 사용할 수 없다.
         */
        fun findUserWithEmail(email: String): Task<QuerySnapshot> {
            // 'email' 필드의 데이터가 '사용자가 입력한 이메일'과 일치하는 문서를 검색
            return getUsers().whereEqualTo("email", email).get()
        }

        /**
         * 새로 가입한 사용자의 데이터를 추가한다.
         */
        fun signUp(data: Any): Task<DocumentReference> {
            return Firebase.firestore
                .collection(COLLECTION_NAME)
                .add(data)
        }

        fun signIn(email: String, password: String): Task<AuthResult> {
            val currentUser = FirebaseAuth.getInstance()
            return currentUser.signInWithEmailAndPassword(email, password)
        }

        fun signOut() {
            Firebase.auth.signOut()
        }

        fun uploadUserImage(bytes: ByteArray, filename: String): UploadTask {
            return Firebase.storage
                .reference
                .child(filename)
                .putBytes(bytes)
        }

        fun downloadUserImage(filename: String?): Task<ByteArray>? {
            val maxSize: Long = 1024 * 1024 * 5
            return if (filename != null)
                Firebase.storage
                    .reference
                    .child(filename)
                    .getBytes(maxSize)
            else null
        }
    }
}