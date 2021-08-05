package com.example.gooreumtv

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gooreumtv.MainActivity.Companion.TAG

class VideoListAdapter(
        private val context: Context,
        private val dataset: ArrayList<VideoData>,
        private val moreVisible: Boolean
    ) : RecyclerView.Adapter<VideoListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView   = view.findViewById(R.id.thumbnail_view)
        val datetimeView: TextView = view.findViewById(R.id.datetime_view)
        val titleView: TextView    = view.findViewById(R.id.title_view)

        val moreButton: ImageView  = view.findViewById(R.id.more_button)
        val containerView: LinearLayout = view.findViewById(R.id.container_view)
    }

    // onCreateViewHolder()에서 아이템 레이아웃을 뷰로 만든 뒤, 그 안에 있는 뷰들에게 접근할 수 있도록 참조를
    // 제공하는 ViewHolder 클래스의 형태로 반환한다.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return ViewHolder(view)
    }

    // 아이템 전용 클릭 인터페이스
    interface OnItemClickListener {
        fun onItemClick(v: View, position: Int)
    }
    // 아이템 뷰에 데이터 설정
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "VideoListAdapter > onBindHolderView / dir:   ${dataset[position].thumbnail}")
        Log.d(TAG, "                                      title: ${dataset[position].title}")
        Log.d(TAG, "                                      datetime: ${dataset[position].datetime}")
        Log.d(TAG, "                                      context:  $context")

        if (!moreVisible) {
            holder.moreButton.visibility = View.INVISIBLE
        }

        MyFirebase.downloadFileWithPath(dataset[position].thumbnail)
            ?.addOnSuccessListener {
                Log.d(TAG, "                                      이미지 다운로드 성공! ${position + 1}/${dataset.size}")

                Glide.with(context)
                    .load(it)
                    .into(holder.imageView)
                val datetime = dataset[position].datetime!!
                holder.datetimeView.text = Toolbox.getRelativeDatetime(datetime)
                holder.titleView.text    = dataset[position].title

            }?.addOnFailureListener { e ->
                Toolbox.showErrorMessage(context, "                            이미지 다운로드 실패 $e", null)
            }

        holder.containerView.setOnClickListener {
            if (position != RecyclerView.NO_POSITION) {
                if (listener != null) {
                    // (리스트가 선언된 액티비티 또는 프래그먼트에서) 아이템을 클릭하면 어댑터 밖에서도 해당 뷰와
                    // 포지션을 사용할 수 있게 인터페이스를 제공한다.
                    listener!!.onItemClick(it, position)
                }
            }
        }
    }
    // 아이템 전용 클릭 리스너
    private var listener: OnItemClickListener? = null
    // (리스트가 선언된 액티비티 또는 프래그먼트에서 사용할) 아이템 전용 클릭 리스너 설정 함수
    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    // 아이템의 수 많큼 어댑터를 반복한다.
    override fun getItemCount(): Int {
        return dataset.size
    }
}