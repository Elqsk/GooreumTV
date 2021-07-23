package com.example.gooreumtv

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Point
import android.graphics.Typeface
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import com.example.gooreumtv.databinding.ActivityPlayerBinding
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.common.reflect.TypeToken
import com.google.gson.GsonBuilder
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding





    private var playerEventListener: PlayerEventListener? = null
    private var playerView: PlayerView? = null
    private var player: SimpleExoPlayer? = null

    private var playbackPosition: Long = 0
    // 여러 트랙을 재생할 때 사용하는 변수 같은데 지금은 크게 필요하지 않다.
    private var currentWindow = 0
    // prepare()는 말 그대로 플레이어를 준비하는(영상을 가져오는) 작업이고, playWhenReady가 true가 되면 영상을
    // 재생한다.
    private var playWhenReady = false





    // 댓글 입력란의 변화를 감지해 작성 버튼이 활성화 또는 비활성화되게 한다.
    private var isAbleToComment = false





    // 애니메이션 중인 댓글들을 리스트에 넣고 관리한다. 영상이 재생 및 일시 정시 상태일 때 애니메이션 상태를 제어한다.
    private var animatingComments = arrayOfNulls<AnimatingComment>(30)
    // 애니메이션 시작도 안 했는데 간발의 차로 먼저 시작한 영사의 재생 시점에 맞춰서 애니 관련 코드가 돌아가다 에러가
    // 나서 애니 시작 신호를 주었다.
    private var animationStarted = false

    private var commentList: ArrayList<Comment>? = null
    // 위의 전체 리스트와는 별개로 새로 작성된 댓글들만 또 따로 모아뒀다 저장한다.
    private var newCommentList: ArrayList<Comment>? = null

    // 전체 댓글 데이터를 가진 로컬 저장소(파일)
    private var prefs: SharedPreferences? = null
    // 댓글을 불러올 때 작성된 순서대로 불러와야 하므로 인덱스를 함께 저장하는데, 새 인덱스는 마지막 숫자에서 1을 올려
    // 넣는다.
    private var lastIndex = -1

    // 현재 재생 시점에 배치되어야 하는 댓글들을 미리 화면 오른쪽 밖에 준비해둔다. 모든 댓글들을 텍스트 뷰로 준비할 수
    // 없으므로 당장 필요한 댓글 데이터만 가져와서 배치하고, 왼쪽 화면 밖으로 벗어나 애니메이션이 종료된 댓글은 텍스트
    // 뷰를 클리어한다. 준비되는 텍스트 뷰는 총 30개이기 때문에, 특정 시점에 댓글이 너무 많을 경우에는 먼저 작성된
    // 댓글 순서대로 영상 위에서 뷰는 사라지지만, 전체 댓글 보기를 하면 데이터는 여전히 남아있다.
    var thread: Thread? = null
    var handler: Handler? = null










    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        playerView = findViewById(R.id.video_view)
        playerEventListener = PlayerEventListener()

        commentList = ArrayList()
        newCommentList = ArrayList()

        // 기존의 댓글 데이터를 가져온다.
        loadData()

        // 화면 크기에 맞게 플레이어 뷰 크기를 조절한다.
        resizePlayerView()
        // 댓글 입력란의 변화를 감지해 작성 버튼이 활성화 또는 비활성화되게 한다.
        addTextChangedListener()
        // 재생 바의 이동을 감지해 구름자막을 다시 배치한다.
        addScrubListener()
    }

    private fun loadData() {
        prefs = getSharedPreferences("comments", MODE_PRIVATE)
//        prefs?.all?.clear()
        val size = prefs!!.all.size

        val token: TypeToken<Comment> = object : TypeToken<Comment>() {}
        val gson = GsonBuilder().create()

        if (size != 0) {
            for (i in 0 until size) {

                val value = prefs?.getString("$i", null)
                commentList!!.add(i, gson.fromJson(value, token.type))

                Log.d(TAG, "                                           [$i] $value")
            }
            Log.d(TAG, " ")

            lastIndex = size - 1

            for (i in 0..lastIndex) {
                Log.d(TAG, "                                           [$i] ${commentList!![i].position} - ${commentList!![i].text}")
            }
        }
        Log.d(TAG, "PlayerActivity > onCreate() / loadData() / size: $size")
        Log.d(TAG, "                                           lastIndex: $lastIndex")
        Log.d(TAG, " ")
    }

    public override fun onStart() {
        super.onStart()

        Log.d(TAG, "PlayerActivity > onStart() / playbackPosition: $playbackPosition")
        Log.d(TAG, "PlayerActivity > onStart() / prefs:   $prefs")
        Log.d(TAG, "PlayerActivity > onStart() / handler: $handler")
        Log.d(TAG, " ")

        if (Util.SDK_INT > 23 || player == null) {
            ready()
        }
    }

    public override fun onResume() {
        super.onResume()
        // 전체 화면 모드
//        hideSystemUi()

        Log.d(TAG, "PlayerActivity > onResume() / playbackPosition: $playbackPosition")
        Log.d(TAG, "PlayerActivity > onResume() / prefs:   $prefs")
        Log.d(TAG, "PlayerActivity > onResume() / handler: $handler")
        Log.d(TAG, " ")

        if (Util.SDK_INT <= 23 || player == null) {
            ready()
        }
    }










    // 처음부터 플레이어를 재생하는 것이 아니고, 플레이어와 (댓글을 화면 밖에 미리 배치하는) 스레드, 핸들러,
    // (시작하자마자 작성된) 기존의 댓글들을 배치하고 애니메이션 적용 까지 마쳐야 (동시에) 시작할 수 있다.
    private fun ready() {

        Log.d(TAG, "PlayerActivity > ready() $playbackPosition")
        Log.d(TAG, " ")

        initializePlayer()
        initializeThreadAndHandler()
        addViews()
        // 애니메이션을 초기화할 때에는 시작하지 않고 있다가, 플레이어 리스너가 영상 재생 상태를 감지해서 시작하게
        // 한다. 따라서 따로 시작하는 코드가 있다기 보다는, 플레이어를 초기화 할 때 prepare()와 playWhenReady
        // 처럼 모든 준비를 다 끝마쳐 놓았다가 변수 하나만 바뀌면 바로 시작하는 것과 같은 원리다.

        go()
    }

    private fun initializePlayer() {

        Log.d(TAG, "PlayerActivity > initializePlayer() $playbackPosition")
        Log.d(TAG, " ")

        if (player == null) {

            val trackSelector = DefaultTrackSelector(this)
            trackSelector.setParameters(
                trackSelector.buildUponParameters().setMaxVideoSizeSd()
            )
            player = SimpleExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build()
        }
        playerView!!.player = player

        player!!.playWhenReady = playWhenReady
//        player!!.seekTo(currentWindow, playbackPosition)
        player!!.seekTo(playbackPosition)
        player!!.addListener(playerEventListener!!)

        Log.d(TAG, "PlayerActivity > ready() / initializePlayer() / playerView.player:    ${playerView!!.player}")
        Log.d(TAG, "                                                playWhenReady:        $playWhenReady")
        Log.d(TAG, "                                                player.playWhenReady: ${player!!.playWhenReady}")
        Log.d(TAG, "                                                playerEventListener:  $playerEventListener")
        Log.d(TAG, "                                                player.seekTo($currentWindow, $playbackPosition)")
        Log.d(TAG, " ")

        // 내부 파일 재생
        val uri = "https://www.youtube.com/api/manifest/dash/id/bf5bb2419360daf1/source/youtube?as=fmp4_audio_clear,fmp4_sd_hd_clear&sparams=ip,ipbits,expire,source,id,as&ip=0.0.0.0&ipbits=0&expire=19000000000&signature=51AF5F39AB0CEC3E5497CD9C900EBFEAECCCB5C7.8506521BFC350652163895D4C26DEE124209AA9E&key=ik0"
        val uri2 = getString(R.string.media_url_dash)
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(MimeTypes.APPLICATION_MPD)
            .build()
        player!!.setMediaItem(mediaItem)
//        player!!.prepare()

        /*
         * (인터넷상의) 외부 파일 재생
         *
         * 외부 파일이라 그런지 화면을 나갔다 들어왔을 때 재생 지점이 초기화되어 있다. 외부 파일 재생 전용 재생 유지
         * 코드를 따로 만들어줘야 할 것 같다.
         */
        val url = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
        val mediaSource = buildMediaSource2(Uri.parse(url))
        // prepare()는 말 그대로 플레이어를 준비하는(영상을 가져오는) 작업이고, playWhenReady가 true가 되면
        // 영상을 재생한다.
        player!!.prepare(mediaSource, true, false)
//        playWhenReady = true
//        player!!.playWhenReady = playWhenReady
    }

    // 곧 애니메이팅하며 오른쪽에서부터 화면에 보일 댓글들을 준비한다.
    private fun initializeThreadAndHandler() {
        // 현재 재생 시점에 배치되어야 하는 댓글들을 미리 화면 오른쪽 밖에 준비해둔다. 모든 댓글들을 텍스트 뷰로
        // 준비할 수 없으므로 당장 필요한 댓글 데이터만 가져와서 배치하고, 왼쪽 화면 밖으로 벗어나 애니메이션이 종료된
        // 댓글은 텍스트 뷰를 클리어한다. 준비되는 텍스트 뷰는 총 30개이기 때문에, 특정 시점에 댓글이 너무 많을
        // 경우에는 먼저 작성된 댓글 순서대로 영상 위에서 뷰는 사라지지만, 전체 댓글 보기를 하면 데이터는 그대로다.
        if (thread == null) {
            thread = Thread(Runnable() {
                run() {
                    Log.d(TAG, "PlayerActivity > thread: Thread / 시작")

                    var currentPosition: Long = 0

                    while (true) {
                        // 애니메이션 시작도 안 했는데 간발의 차로 먼저 시작한 영사의 재생 시점에 맞춰서 애니 관련
                        // 코드가 돌아가다 에러가 나서 애니 시작 신호를 주었다.
                        if (animatingComments[0] != null)
                            if (animatingComments[0]!!.animator != null)
                                animationStarted = true

                        if (player != null && commentList != null) {

                            currentPosition = player!!.currentPosition

                            if (player!!.isPlaying && commentList!!.size != 0 &&
                                animationStarted) {

                                for (i in 0 until commentList!!.size) {
                                    // 8초 동안 1080 만큼 이동하고(화면의 끝에서 끝으로 가고), 초당 135 만큼
                                    // 이동한다. 현재 시점에 작성되는 구름자막의 왼쪽 마진이 600이면, 그 뒤의
                                    // 미치 배치해야 하는 댓글들의 마진은 초당 600＋135n이다. 따라서 포지션
                                    // 0일 때에는 0초에서 4초 미만 사이에 작성된 댓글들이 화면에 보이고, 4초
                                    // 이상 부터는 아직 안 보이므로 스레드를 사용해서 조금씩 미리 불러와 배치해
                                    // 놓는다.
                                    if (commentList!![i].position!! >= currentPosition + 4000 &&
                                        commentList!![i].position!! < currentPosition + 5000) {
                                        Log.d(TAG, "PlayerActivity > thread: Thread / " +
                                                "[$i] $currentPosition - ${commentList!![i].position}")

                                        val msg = Message()
                                        msg.arg1 = i
                                        msg.arg2 = currentPosition.toInt()
                                        msg.what = 1
                                        handler!!.sendMessage(msg)
                                    }
                                }
                                // 1초 마다 1초 만큼의 댓글들을 불러와 화면 밖에 배치한다.
                                try {
                                    Log.d(TAG, "PlayerActivity > thread: Thread / sleep")
                                    Thread.sleep(1000)
                                } catch (e: InterruptedException) {
                                    Log.d(TAG, "PlayerActivity > thread: Thread / 종료")
                                }
                            }
                        }
                    }
                }
            })
        }
        if (handler == null) {
            Log.d(TAG, "PlayerActivity > handler: Handler")

            handler = object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)

                    if (msg.what == 1) {

                        val index = msg.arg1

                        val v = initCommentView()
                        v.text = commentList!![index].text
                        v.background = null
                        binding.animationLayout.addView(v)

                        // 영상 위에서 애니메이션 중인 댓글들을 리스트에 넣고 관리한다. 영상이 재생 및 일시 정시
                        // 상태일 때 애니메이션 상태를 제어한다.
                        val comment = AnimatingComment(v)
                        // 뷰들이 서로 겹치는지를 판단해야 하는데, 아직 준비가 끝나지 않은 뷰의 너비를 재려고 하면
                        // 0으로 나와서 따로 내용과 재생 시점 외에 너비도 같이 저장해서 가져다 쓴다. 뷰를
                        // 생성하면서 width를 하드코딩하면 되지 않을까 했지만 여전히 0으로 나온다. 그래서 댓글을
                        // 작성할 때 post()로 너비와 높이를 구해서 따로 저장해 사용한다.
                        comment.viewWidth  = commentList!![index].width!!
                        comment.viewHeight = commentList!![index].height!!
                        animatingComments[index] = comment

                        val params = initLayoutParams()
                        // 처음에 왼쪽 마진만 먼저 적용해놓고 또 윗마진과 왼마진을 한 번 더 적용하는 이유는,
                        // 윗마진을 얻어올 때 왼마진이 사용되기 때문이다. 이때 기본 마진으로 설정된 초기 상태일
                        // 기존의 뷰들과 겹친다는 오판정이 난다.
                        params.leftMargin =
                            getMarginLeft(msg.arg2.toLong(), commentList!![index].position!!)
                        v.layoutParams = params

                        params.topMargin = getMarginTop(index)
                        params.leftMargin =
                            getMarginLeft(msg.arg2.toLong(), commentList!![index].position!!)
                        v.layoutParams = params

                        Log.d(TAG, "PlayerActivity > handler: Handler / [$index] ${msg.arg2.toLong()} - ${commentList!![index].position!!}")
                        Log.d(TAG, "                                    [$index] text:       ${commentList!![index].text}")
                        Log.d(TAG, "                                    [$index] width:      ${commentList!![index].width!!}")
                        Log.d(TAG, "                                    [$index] marginLeft: ${params.leftMargin}")

                        applyAnimation(index)
                    }
                }
            }
        }
    }

    // 영상 위에 기존의 구름자막들을 배치하고 애니메이션을 초기화한다.
    private fun addViews() {
        // 8초 동안 1080 만큼 이동하고(화면의 끝에서 끝으로 가고), 초당 135 만큼 이동한다. A 시점 구름자막 마진이
        // 600이면, 그 뒤의 미치 배치해야 하는 댓글들의 마진은 초당 600+135n이다. 따라서 포지션 0일 때에는 0초에서
        // 3초 사이에 작성된 댓글들이 화면에 보이고, 4초 부터는 아직 안 보이므로 스레드를 사용해서 배치한다.
        // 처음에는 0초 부터 4초 미만 사이의 댓글을 배치하고, 이후 장면을 건너뛰어 재생할 때에는 -8초 부터
        // +4초 이상 까지 댓글을 미리 배치한다.
        val currentPosition = player!!.currentPosition

        if (commentList != null) {
            Log.d(TAG, "PlayerActivity > addViews() / currentPosition:  $currentPosition")
            Log.d(TAG, "                              commentList.size: ${commentList!!.size}")

            for (i in 0 until commentList!!.size) {

                if (commentList!![i].position!! > currentPosition - 8000 &&
                    commentList!![i].position!! < currentPosition + 4000) {

                    Log.d(TAG, "PlayerActivity > addViews() / [$i] text:     ${commentList!![i].text}")
                    Log.d(TAG, "                              [$i] width:    ${commentList!![i].width}")
                    Log.d(TAG, "                              [$i] height:   ${commentList!![i].height}")
                    Log.d(TAG, "                              [$i] position: ${commentList!![i].position}")

                    val v = initCommentView()
                    v.text = commentList!![i].text
                    // 이전 댓글을 불러올 때에는 노란 테두리를 없앤다.
                    v.background = null

                    // 영상 위에서 애니메이션 중인 댓글들을 리스트에 넣고 관리한다. 영상이 재생 및 일시 정시 상태일
                    // 때 애니메이션 상태를 제어한다.
                    val comment = AnimatingComment(v)
                    // 뷰들이 서로 겹치는지를 판단해야 하는데, 아직 준비가 끝나지 않은 뷰의 너비를 재려고 하면
                    // 0으로 나와서 따로 내용과 재생 시점 외에 너비도 같이 저장해서 가져다 쓴다. 뷰를 생성하면서
                    // width를 하드코딩하면 되지 않을까 했지만 여전히 0으로 나온다. 그래서 댓글을 작성할 때
                    // post()로 너비와 높이를 구해서 따로 저장해 사용한다.
                    comment.viewWidth = commentList!![i].width!!
                    comment.viewHeight = commentList!![i].height!!
                    animatingComments[i] = comment

                    val params = initLayoutParams()
                    params.topMargin = getMarginTop(i)
                    params.leftMargin = getMarginLeft(currentPosition, commentList!![i].position!!)
                    v.layoutParams = params

                    binding.animationLayout.addView(v)

                    applyAnimation(i)
                }
            }
        }
        Log.d(TAG, "PlayerActivity > addViews() / animation_layout.childCount: ${binding.animationLayout.childCount}")
        Log.d(TAG, " ")
    }










    private fun go() {
        // prepare()는 말 그대로 플레이어를 준비하는(영상을 가져오는) 작업이고, playWhenReady가 true가 되면
        // 영상을 재생한다.
        playWhenReady = true
        player?.playWhenReady = playWhenReady

        // 곧 화면에 보여질 댓글을 미리 화면 밖에 배치하는 스레드를 시작한다.
        thread?.start()

        // 안 보이게 해놨던 댓글들을 보이게 한다.
        binding.animationLayout.visibility = View.VISIBLE

        Log.d(TAG, "PlayerActivity > go()")
        Log.d(TAG, " ")
    }










    // 댓글 작성 버튼을 누르면새 텍스트 뷰를 생성하고, 댓글 텍스트를 넣어, 여유 공간이 있는지 확인하고, 배치한다.
    fun comment(view: View) {

        if (isAbleToComment) {

            val text = binding.commentEditText.text.toString()

            Log.d(TAG, "PlayerActivity > comment() / 새 댓글:    $text")
            Log.d(TAG, "                             isPlaying: ${player!!.isPlaying}")

            // 30개의 텍스트 뷰 중에서 비어있는 자리를 찾아 댓글을 삽입한다.
            for (i in 0..29) {

                if (animatingComments[i] == null) {

                    val v = initCommentView()
                    // 기존의 뷰와 겹치거나, 너무 길거나, 텍스트 뷰를 연속으로 추가할 수 있는 최대 라인(10)을
                    // 초과했는지 검사해야 하기 때문에, 일단 텍스트를 넣은 뒤 더 이상 작성할 수 없다고 판단되면
                    // 뷰를 삭제한다.
                    v.text = text
                    binding.animationLayout.addView(v)

                    // 영상 위에서 애니메이션 중인 댓글들을 리스트에 넣고 관리한다. 영상이 재생 및 일시 정시 상태일
                    // 때 애니메이션 상태를 제어한다.
                    animatingComments[i] = AnimatingComment(v)

                    val marginTop = getMarginTop(i)
                    when {
                        marginTop > -1 -> {
                            // 여유 공간을 찾았다면 0 이상의 정상적인 수치의 마진이 얻어진다. 새 뷰의 최종 위치를
                            // 확정하고 배치한다.
                            val params = initLayoutParams()
                            params.topMargin = marginTop
                            params.leftMargin = VIEW_DEF_MARGIN_LEFT.toInt()

                            v.layoutParams = params

                            // 댓글 뷰를 영상 위에 성공적으로 배치했으면 애니메이션을 시작한다.
                            applyAnimation(i)

                            // 댓글 리스트에 방금 작성한 것을 추가한다.
                            v.post(Runnable() {
                                run() {
                                    Log.d(TAG, "PlayerActivity > comment() / post() / marginTop: $marginTop")
                                    Log.d(TAG, "                                      width:     ${v.width}")
                                    Log.d(TAG, "                                      height:    ${v.height}")
                                    addComment(text, player!!.currentPosition, v.width, v.height)
                                }
                            })
                            binding.commentEditText.setText("")
                        }
                        marginTop == -1 -> {
                            // setCommentView()로부터 -1을 반환받았다는 것은 연속으로 작성 가능한 댓글의 수가
                            // 한계에 다다랐다는 것이므로, 뷰를 삭제하고 댓글도 작성하지 않는다.
                            Toast.makeText(this, "연속으로 작성할 수 있는 댓글의 수를 초과하였습니다", Toast.LENGTH_SHORT).show()
                            removeView(i)
                        }
                        else -> {
                            // setCommentView()로부터 -2을 반환받았다는 것은 텍스트 뷰가 너무 길다는 것이므로,
                            // 이 역시 뷰를 삭제하고 댓글도 작성하지 않는다.
                            Toast.makeText(this, "댓글이 너무 깁니다", Toast.LENGTH_SHORT).show()
                            removeView(i)
                        }
                    }
                    break

                } else {
                    if (i == 29)
                        Toast.makeText(this, "영상을 좀 더 시청한 뒤에 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                }
            }
        }
        // 정상적으로 잘 들어가 있는지 확인하기 위해 텍스트를 모두 출력해 본다.
        for (i in 0..29) {
            Log.d(TAG, "                             댓글 ${i + 1}: " + animatingComments[i]?.view?.text)
        }
        Log.d(TAG, " ")
    }

    // 댓글을 작성하면 영상 위에 자막이 나타났다가 왼쪽으로 이동해 사라지고, 영상이 일시 정지 상태일 때에는
    // 애니메이션이 작동하지 않지만, 영상이 다시 재생되면 정상적으로 움직인다.
    private fun applyAnimation(index: Int) {
        /*
         * xml 파일에 작성된 뷰의 아이디를 얻어오는 코드
         *
         * 패키지명(com.example.exoplayercodelab:id/)이 같이 딸려오기 때문에 이 부분을 잘라내는 별도의 처리를
         * 해준다.
         */
//        val viewId: String = views!!.view!!.resources.getResourceName(views.view!!.id).substringAfter("com.example.exoplayercodelab:id/")
        val comments = animatingComments[index]

        // 현채 재생 위치를 기준으로, 현재 위치에 작성된 댓글은 기본 마진과 애니메이션 값, 지속시간을 적용하면 되지만,
        // 이전과 이후에 작성된 댓글들은 화면에 배치할 때 조정이 필요하다. 애니메이터를 설정할 때 이동값과 지속시간에
        // 직접 숫자를 넣지 않은 이유는 모든 뷰가 같은 속력을 내야하는데 뷰들의 위치가 다 다를 수 있기 때문이다.
        val marginLeft = comments!!.view!!.marginLeft
        val value = getAnimationValue(marginLeft).toFloat()

        Log.d(TAG, "PlayerActivity > applyAnimation() / [$index] marginLeft: $marginLeft")
        Log.d(TAG, "                                    [$index] value:      $value")

        val animator = ObjectAnimator.ofFloat(comments.view!!, View.TRANSLATION_X, -value)
            .apply {
                duration = getAnimationDuration(value).roundToLong()
            }
        Log.d(TAG, "                                    [$index] duration:   ${animator.duration}")

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationPause(animation: Animator?) {
                super.onAnimationPause(animation)

                Log.d(TAG, "PlayerActivity / AnimatorListenerAdapter() > 텍스트 뷰 $index 애니메이션 일시 정지")
            }
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)

                Log.d(TAG, "PlayerActivity / AnimatorListenerAdapter() > 텍스트 뷰 $index 애니메이션 종료")
                Log.d(TAG, " ")

                // 애니메이션이 종료되면 뷰를 제거한다.
                removeView(index)
            }
        })
        // 영상이 재생 중이면 애니메이션을 시작한다.
        if (player!!.isPlaying) {
            comments.animationState = AnimatingComment.RUNNING
            animator.start()
        } else {
            comments.animationState = AnimatingComment.READY
        }
        // 애니메이션 중인 텍스트 뷰들을 배열에 넣고 관리한다. 영상을 재생하거나 멈출 때 함께 제어한다. 애니메이터나
        // 텍스트가 있는지의 여부로 뷰가 비었는지 안 비었는지 판단한다.
        comments.animator = animator
    }

    // 영상 위에서 애니메이션 중인 댓글들을 리스트에 넣고 관리한다. 영상이 재생 및 일시 정시 상태일 때 애니메이션
    // 상태를 제어한다.
    private fun addComment(text: String, position: Long, width: Int, height: Int) {
        Log.d(TAG, "PlayerActivity > addComment() / lastIndex: $lastIndex")

        val index = lastIndex.plus(1)
        val comment = Comment(index, text, position, width, height)
        comment.isNew = true
        commentList?.add(comment)
        newCommentList?.add(comment)

        lastIndex ++
    }










    private fun removeView(index: Int) {
        Log.d(TAG, "PlayerActivity > removeView() / ${animatingComments[index]!!.view}")
        Log.d(TAG, "                                ${animatingComments[index]}")

        binding.animationLayout.removeView(animatingComments[index]!!.view)
        animatingComments[index] = null

        Log.d(TAG, "                                텍스트 뷰 ${index + 1}를(을) 삭제하였습니다.")
        Log.d(TAG, " ")
    }










    /*
     * 댓글 텍스트를 감지해 작성 버튼이 활성화 또는 비활성화되게 한다. 공백만으로 이루어져 있거나 텍스트를 아무 것도
     * 입력하지 않으면 댓글을 작성할 수 없다.
     */
    private fun addTextChangedListener() {
        binding.commentEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {

                if (binding.commentEditText.text.toString().trim().isNotEmpty()) {
                    isAbleToComment = true

                    binding.sendButtonAble.visibility = View.VISIBLE
                    binding.sendButtonDisable.visibility = View.INVISIBLE
                } else {
                    isAbleToComment = false

                    binding.sendButtonAble.visibility = View.INVISIBLE
                    binding.sendButtonDisable.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun addScrubListener()  {

        binding.exoProgress.isEnabled = true

        binding.exoProgress.addListener(object : TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                Log.d(TAG, "PlayerActivity > TimeBar.OnScrubListener / onScrubStart() / position: $position")
                Log.d(TAG, " ")
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                Log.d(TAG, "PlayerActivity > TimeBar.OnScrubListener / onScrubMove() / position: $position")
                Log.d(TAG, " ")
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                Log.d(TAG, "PlayerActivity > TimeBar.OnScrubListener / onScrubStop() / canceled: $canceled")
                Log.d(TAG, "PlayerActivity > TimeBar.OnScrubListener / onScrubStop() / position: $position")
                Log.d(TAG, " ")

                for (i in animatingComments.indices) {
                    if (animatingComments[i] != null) {
                        if (animatingComments[i]!!.animator != null) {
                            animatingComments[i]!!.animator!!.end()
                        }
                    }
                }
                animatingComments = arrayOfNulls<AnimatingComment>(30)
                binding.animationLayout.removeAllViews()

                // 타임바를 움직이면 기존의 재생 중이던 구름자막을 모두 없애고 새 뷰들을 배치한다.
                addViews()
            }
        })
    }

    // 영상의 재생 상태를 파악하고 그에 따라 댓글 뷰의 애니메이션을 제어한다.
    private inner class PlayerEventListener : Player.EventListener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE ->      "ExoPlayer.STATE_IDLE      - 재생 실패"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING - 재생 준비 중"
                ExoPlayer.STATE_READY ->     "ExoPlayer.STATE_READY     - 재생 준비 완료"
                ExoPlayer.STATE_ENDED ->     "ExoPlayer.STATE_ENDED     - 재생 종료"
                else ->                      "STATE_UNKNOWN             - 상태 불명"
            }
            Log.d(TAG, "PlayerActivity / PlaybackStateListener > onPlaybackStateChanged() / playbackState: $playbackState")
            Log.d(TAG, "                                                                    stateString:   $stateString")
            Log.d(TAG, "                                                                    playerEventListener: $playerEventListener")
            Log.d(TAG, " ")
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            super.onTimelineChanged(timeline, reason)

            Log.d(TAG, "PlayerActivity / PlaybackStateListener > onTimelineChanged() / timeline: $timeline")
            Log.d(TAG, "                                                               reason:   $reason")
        }

        @RequiresApi(Build.VERSION_CODES.KITKAT)
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            /*
             * 댓글 작성 시 영상 재생 여부에 따라 댓글의 애니메이션 상태 정보를 갖는다. 영상이 일시 정지일 때
             * 작성되어 'isReady'일 경우에는 나중에 영상이 재생될 때 start()로 애니메이션을 시작하고, 재생 중일
             * 때 작성되어 'isPlaying'일 경우에는 영상이 정지되면 'paused'가 되는데, 'paused'인 댓글은 영상이
             * 다시 재생될 때 resume()으로 애니메이션을 재시작한다.
             */
            if (isPlaying) {
                Log.d(TAG, "PlayerActivity / PlaybackStateListener > onIsPlayingChanged() / 재생 중")

                for (i in 0..29) {

                    val ac = animatingComments[i]
                    if (ac != null) {

                        if (ac.animationState == AnimatingComment.READY) {

                            ac.animationState = AnimatingComment.RUNNING
                            ac.animator!!.start()

                        } else if (ac.animationState == AnimatingComment.PAUSED) {

                            ac.animationState = AnimatingComment.RUNNING
                            ac.animator!!.resume()
                        }
                    }
                }
            } else {
                Log.d(TAG, "PlayerActivity / PlaybackStateListener > onIsPlayingChanged() / 재생 중이 아님")

                for (i in 0..29) {

                    val ac = animatingComments[i]
                    if (ac != null) {

                        if (ac.animationState == AnimatingComment.RUNNING) {

                            ac.animationState = AnimatingComment.PAUSED
                            ac.animator!!.pause()
                        }
                    }
                }
            }
            Log.d(TAG, " ")
        }
    }










    public override fun onPause() {
        super.onPause()

        Log.d(TAG, "PlayerActivity > onPause() / player: $player")
        Log.d(TAG, " ")

//        if (Util.SDK_INT <= 23) {

        releasePlayer()
        if (thread != null) {
            thread!!.interrupt()
            thread = null
        }
//        } else {
//            Log.d(TAG, "PlayerActivity > onPause() / SDK 버전이 23 이상입니다.")
//            Log.d(TAG, " ")
//        }
    }

    public override fun onStop() {
        super.onStop()

        Log.d(TAG, "PlayerActivity > onStop() / player: $player")
        Log.d(TAG, " ")

//        if (Util.SDK_INT > 23) {

        releasePlayer()
        saveData()
        if (thread != null) {
            thread!!.interrupt()
            thread = null
        }
//        } else {
//            Log.d(TAG, "PlayerActivity > onStop() / SDK 버전이 23 보다 낮습니다.")
//            Log.d(TAG, " ")
//        }
    }

    // 사용자가 홈 버튼을 누르는 등 화면을 나가면 플레이어를 종료한다.
    private fun releasePlayer() {

        Log.d(TAG, "PlayerActivity > releasePlayer() / player: $player")
        Log.d(TAG, " ")

        if (player != null) {

            playWhenReady = player!!.playWhenReady
            currentWindow = player!!.currentWindowIndex
            playbackPosition = player!!.currentPosition

            Log.d(TAG, "PlayerActivity > releasePlayer() / player!!.playWhenReady: ${player!!.playWhenReady}")
            Log.d(TAG, "                                   playbackPosition: $playbackPosition")
            Log.d(TAG, "                                   currentWindow:    $currentWindow")
            Log.d(TAG, "                                   playWhenReady:    $playWhenReady")

            player!!.removeListener(playerEventListener!!)
            player!!.release()
            player = null
        }
    }

    private fun saveData() {
        Log.d(TAG, "PlayerActivity > saveComments()")

        val token: TypeToken<Comment> = object : TypeToken<Comment>() {}
        val gson = GsonBuilder().create()

        val editor = prefs!!.edit()

        if (newCommentList!!.size != 0) {
            for (i in 0 until newCommentList!!.size) {
                val value = gson.toJson(newCommentList!![i], token.type)
                val key = newCommentList!![i].index
                editor?.putString("$key", value)

                Log.d(TAG, "                                  $key: $value")
            }
            editor?.apply()

            Log.d(TAG, " ")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d(TAG, "PlayerActivity > onDestroy()")
        Log.d(TAG, " ")
    }










    companion object {
        //        private val TAG = PlayerActivity::class.java.name
        const val TAG = "exo_2"

        // (막 작성된 댓글 뷰가 갖는) 기본 왼쪽 마진
        const val VIEW_DEF_MARGIN_LEFT = 600f
        // 최대 뷰 길이: 재생 바를 이동했을 때 등 유동적으로 왼쪽 마진을 구할 때 사용한다.
        const val VIEW_MAX_WIDTH = 615f
        // 기본 애니메이션 시간: 유동적으로 왼쪽 마진을 구할 때 사용한다. (단위: ms)
        const val ANIM_DEF_DURATION = 8000f
    }

    /**
     * 현채 재생 위치를 기준으로, 현재 위치에 작성된 댓글은 기본 마진과 애니메이션 값, 지속시간을 적용하면 되지만,
     * 이전과 이후에 작성된 댓글들은 화면에 배치할 때 조정이 필요하다. 애니메이션은 오른쪽에서 왼쪽으로 이동하기
     * 때문에, 먼저 작성된 댓글은 더 왼쪽에 위치해있는 만큼 이동해야 하는 거리가 더 짧고, 늦게 작성된 댓글은 더
     * 오른쪽에 위치한 만큼 더 많은 거리를 이동해야 한다.
     */
    private fun getAnimationValue(marginLeft: Int): Int {
        Log.d(TAG, "PlayerActivity > getAnimationValue() / marginLeft: $marginLeft")

        return if (marginLeft < 0)
            VIEW_MAX_WIDTH.toInt() - marginLeft
        else
            VIEW_MAX_WIDTH.toInt() + marginLeft
    }

    /**
     * @param value 애니메이션 진행값
     */
    private fun getAnimationDuration(value: Float): Float {
        Log.d(TAG, "PlayerActivity > getAnimationDuration() / value:    $value")
        Log.d(TAG, "                                          duration: ${(value / getAnimationSpeed()).toLong()}")

        return value / getAnimationSpeed()
    }

    /**
     * 설정된 기본 수치들을 바탕으로 애니메이션 고정 속력을 구한다. 댓글 뷰가 어느 위치에 있더라도, 모든 댓글 뷰의
     * 애니메이션 속력은 같다. (단위: px/ms)
     */
    private fun getAnimationSpeed(): Float {
        val speed = (VIEW_DEF_MARGIN_LEFT + VIEW_MAX_WIDTH) / ANIM_DEF_DURATION

//        Log.d(TAG, "PlayerActivity > getAnimationSpeed() / speed: " +
//                "($VIEW_DEF_MARGIN_LEFT + $VIEW_MAX_WIDTH) ÷ $ANIM_DEF_DURATION = $speed")
        return speed
    }

    /**
     * 현채 재생 위치를 기준으로, 현재 위치에 작성된 댓글은 기본 마진과 애니메이션 값, 지속시간을 적용하면 되지만,
     * 이전과 이후에 작성된 댓글들은 화면에 배치할 때 조정이 필요하다.
     * @param currentPosition 영상의 현재 재생위치
     * @param position 댓글이 작성되었던 영상의 재생위치
     */
    private fun getMarginLeft(currentPosition: Long, position: Long): Int {

        var diff = currentPosition - position
        if (diff < currentPosition)
            diff = -diff
        val marginLeft = VIEW_DEF_MARGIN_LEFT + (getAnimationSpeed() * diff)

        Log.d(TAG, "PlayerActivity > getMarginLeft() / diff:       $currentPosition ± $position = $diff")
        Log.d(TAG, "                                   marginLeft: " +
                "$VIEW_DEF_MARGIN_LEFT + (${getAnimationSpeed()} × $diff) = $marginLeft")

        return marginLeft.toInt()
    }

    /**
     * 뷰의 세로 위치를 얻어온다. 텍스트 뷰를 배치할 여유 공간을 찾아 기존의 뷰와 겹치면 윗마진을 조절해 아래로 계속
     * 밀어낸다.
     */
    private fun getMarginTop(index: Int): Int {
        val v = animatingComments[index]!!.view!!
        // 8초 동안 1080 만큼 이동하고(화면의 끝에서 끝으로 가고), 초당 135 만큼 이동한다. 포지션 0일 때에는
        // 0초에서 3초 사이에 작성된 댓글들이 보인다.
        // A 시점 구름자막 마진이 600이면, 그 뒤의 미치 배치해야 하는 댓글들의 마진은 초당 600+135n이다. A 시점에
        // 달린 구름자막의 최대 길이는 화면 안에 들 수 있는 최대 길이+화면 밖으로 초과될 수 있는 길이(430+135=615)
        // 텍스트 뷰의 최대 길이는 615이므로 이를 넘을 경우 -2를 리턴해버린다.
        if (v.length() > VIEW_MAX_WIDTH) {
            return -2
        }
        // 새 텍스트 뷰의 왼변 x 좌표
        val newXLeft = v.marginLeft
        // 이전 텍스트 뷰들과 겹치는지 체크한다. 이전 뷰의 오른쪽 변의 x 좌표 이번 뷰의 왼쪽 변의 x 좌표 보다 값이
        // 크면 겹치는 것으로 판단한다. RelativeLayout 안에 있는 뷰는 마진으로 위치를 조정하며, 애니메이션을
        // 적용하더라도 실제 좌표에는 변화가 없다.
        Log.d(TAG, "PlayerActivity > getMarginTop() / index:    $index")
        Log.d(TAG, "                                  text:     ${v.text}")
        Log.d(TAG, "                                  newXLeft: $newXLeft")

        // 새 텍스트 뷰가 최종적으로 갖게 될 윗마진
        var newMarginTop = 0

        if (commentList!!.size != 0) {
            // 현재 검사 중인 줄의 위쪽 y 좌표
            var lineMarginTop = 0
            // 검사 중에 marginTop이 증가했다는 것은 기존의 텍스트 뷰와 겹쳐서 아래로 밀려났다는 뜻이고, 이는 현재의
            // 라인(lineYTop)과 비교해서 알 수 있다.
            // 각 줄(=텍스트 뷰)의 높이
            var height = 0
            // 새 텍스트 뷰를 배치하려는 곳에 이미 다른 텍스트 뷰가 있다면, 그 뷰의 높이 만큼 아래로 밀어내고 밀어내면서
            // 빈 곳을 찾는다. 하지만 무한하게 내려갈 수 있는 것은 아니고 최대 10줄로 제한해 놓았다.
            for (i in 1..10) {

                for (j in 0..29) {
                    // 자기 뷰와 현재 검사 중인 줄의 빈 텍스트 뷰는 검사하지 않아도 된다.
                    if (j != index &&
                        animatingComments[j] != null && animatingComments[j]!!.animator != null) {

                        val old = animatingComments[j]!!
                        // 기존 텍스트 뷰의 윗변 y 좌표
                        val oldMarginTop = old.view!!.marginTop

                        // 애니메이션을 적용해도 위치에 변화는 없고 사용자의 눈에만 움직이는 것 처럼 보일 뿐이다. 따라서
                        // 뷰가 서로 겹치는지를 판단하기 위해 좌표가 필요할 경우 별도로 계산해야 한다. 처음 이전 뷰의 x
                        // 좌표에서 이동한 만큼의 거리(애니메이션 경과 값)를 빼면 된다.
                        val oldWidth =
                            if (old.viewWidth == 0 || old.viewWidth == null)
                                old.view!!.width
                            else
                                old.viewWidth!!
                        val oldXRightRaw = old.view!!.marginLeft + oldWidth
                        val animatedValue = (old.animator!!.animatedValue as Float).roundToInt()
                        // 왼쪽으로 애니메이션이 적용되면 음수가 나오기 때문에 여기서는 더하기를 해야 원하는 값이
                        // 나온다.
                        val oldXRight = oldXRightRaw + animatedValue

                        Log.d(TAG, "                                  Line $i / View $j / oldXRightRaw: ${old.view!!.marginLeft}＋${oldWidth}＝$oldXRightRaw")
                        Log.d(TAG, "                                  Line $i / View $j / oldXRight:    $oldXRightRaw－${-animatedValue}＝$oldXRight")
                        Log.d(TAG, "                                  Line $i / View $j / oldMarginTop($oldMarginTop) =? newMarginTop($newMarginTop)")
                        Log.d(TAG, "                                  Line $i / View $j / oldMarginRight($oldXRight) >? newXLeft($newXLeft)")

                        // 새 댓글을 넣은 텍스트 뷰를 현재 검사 중인 줄에 배치한 뒤, 자신을 제외하고 윗마진이 같은
                        // 이전의 뷰가 있고 그 뷰와 x 좌표가 겹치면 윗마진의 값을 증가시켜 다음 줄로 밀어낸다.
                        if (oldMarginTop == newMarginTop && oldXRight > newXLeft) {
                            Log.d(TAG, "                                  Line $i / View $j / old.view.height: ${old.view!!.height}")

                            val oldHeight =
                                if (old.viewHeight == 0 || old.viewHeight == null) {
                                    old.view!!.height
                                } else {
                                    old.viewHeight!!
                                }
                            height = oldHeight
                            // 검사 중에 marginTop이 증가했다는 것은 기존의 텍스트 뷰와 겹쳐서 아래로 밀려났다는
                            // 뜻이고, 이는 현재의 라인과 비교해서 알 수 있다.
                            newMarginTop += height
                        }
                    }
                }
                Log.d(TAG, "                                  Line $i / lineMarginTop($lineMarginTop) =? newMarginTop($newMarginTop)")
                Log.d(TAG, "                                  Line $i / 뷰 높이: $height")

                if (lineMarginTop == newMarginTop) {
                    // newMarginTop 값이 증가하지 않았다는 것은 해당 줄에서 다른 뷰들과 겹치지 않았다는 뜻이고,
                    // 현재 검사 중인 줄의 윗마진(lineMarginTop)과 값이 같다. 따라서 새 텍스트 뷰는 이대로
                    // 최종적으로 배치한다.
                    Log.d(TAG, "                                  Line $i / 기존의 뷰들과 새 뷰 ${index}가 서로 겹치지 않으므로 해당 줄에 배치합니다.")

                    break
                } else {
                    // 다른 뷰들과 겹쳐 newMarginTop 증가해 아랫줄로 밀려나면, 다음 줄에서도 두 값이 같은지
                    // 다른지 비교해야 하기 때문에, 해당 줄의 윗마진(lineMarginTop)도 값을 증가시켜서 같게
                    // 만들어 준다.
                    lineMarginTop += height

                    Log.d(TAG, "                                  Line $i / 기존의 뷰들과 새 뷰 ${index}가 서로 겹치므로 다음 줄에서 다시 검사합니다.")

                    // 10 줄을 검사할 동안 텍스트 뷰를 배치할 곳을 찾지 못하면 marginTop을 -1으로 해서 반환한다.
                    if (i == 10) {
                        newMarginTop = -1

                        Log.d(TAG, "                                  더 이상 연속으로 뷰를 배치할 곳이 없습니다.")
                    }
                }
            }
            Log.d(TAG, " ")
        }
        Log.d(TAG, "PlayerActivity > getMarginTop() / newXTop: $newMarginTop")
        Log.d(TAG, " ")

        return newMarginTop
    }










    // 전체 화면 모드
    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        playerView!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    /**
     * 텍스트 뷰를 생성한 뒤 외형을 꾸며서 가져온다.
     */
    private fun initCommentView(): TextView {
        val view = TextView(this)

        val params = initLayoutParams()
        params.topMargin = 0
        params.leftMargin = VIEW_DEF_MARGIN_LEFT.toInt()
        view.layoutParams = params
        view.background = ContextCompat.getDrawable(this, R.drawable.stroke_comment)
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        view.setTextColor(Color.WHITE)
        view.setTypeface(view.typeface, Typeface.BOLD)
        view.setShadowLayer(4f, 2f, 2f, Color.parseColor("#808080"))
        view.inputType = InputType.TYPE_CLASS_TEXT

        return view
    }

    class AnimatingComment(view: TextView) {
        var view: TextView? = view
        var animator: ObjectAnimator? = null
        /*
         * 댓글 작성 시 영상 재생 여부에 따라 댓글의 애니메이션 상태 정보를 갖는다. 영상이 일시 정지일 때 작성되어
         * 'READY'일 경우에는 나중에 영상이 재생될 때 start()로 애니메이션을 시작하고, 재생 중일 때 작성되어
         * 'PLAYING'일 경우에는 영상이 정지되면 'PAUSED'가 되는데, 'PAUSED'인 댓글은 영상이 다시 재생될
         * 때 resume()으로 애니메이션을 재시작한다.
         */
        var animationState: Int? = null
        var viewWidth: Int? = null
        var viewHeight: Int? = null

        companion object {
            var RUNNING = 1
            var READY = 2
            var PAUSED = 3
        }
    }

    // 변수와 로컬에 저장하는 전체 댓글
    class Comment(index: Int, text: String, position: Long, width: Int, height: Int) {
        var index: Int? = index
        var text: String? = text
        var position: Long? = position
        var width: Int? = width
        var height: Int? = height
        var isNew = false
    }

    private fun initLayoutParams() : RelativeLayout.LayoutParams {
        return RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // 화면 크기에 맞게 플레이어 뷰 크기를 조절한다.
    private fun resizePlayerView() {
        val display = windowManager.defaultDisplay // in case of Activity
        /* val display = activity!!.windowManaver.defaultDisplay */ // in case of Fragment
        val size = Point()
        display.getSize(size) // or getSize(size)
        val width = size.x
        val height = size.y

        Log.d(TAG, "PlayerActivity > onCreate() / resizePlayerView() / 너비: $width")
        Log.d(TAG, "                                                   높이: $height")
        Log.d(TAG, "                                                   변경된 높이: ${width * 9 / 16}")
        Log.d(TAG, " ")

        // 가로, 세로 비율은 16:9 (1920:1080)
        playerView!!.setHeight(width * 9 / 16)
    }

    private fun View.setHeight(value: Int) {
        val lp = layoutParams
        lp?.let {
            lp.height = value
            layoutParams = lp
        }
    }

    // 공식 사이트에 있는 기본 예제는 내부 파일 재생용이라 인터넷상의 외부 파일을 재생하려면 별도의 처리가 필요하다.
//    private fun buildMediaSource(uri: Uri): MediaSource {
//        val userAgent = Util.getUserAgent(this, "구름자막")
//
//        return if (uri.lastPathSegment!!.contains("mp3") || uri.lastPathSegment!!.contains("mp4")) {
//            ExtractorMediaSource.Factory(DefaultHttpDataSourceFactory(userAgent))
//                    .createMediaSource(uri)
//        } else if (uri.lastPathSegment!!.contains("m3u8")) {
//            // com.google.android.exoplayer:exoplayer-hls 확장 라이브러리를 빌드 해야 합니다.
//            HlsMediaSource.Factory(DefaultHttpDataSourceFactory(userAgent))
//                    .createMediaSource(uri)
//        } else {
//            ExtractorMediaSource.Factory(DefaultDataSourceFactory(this, userAgent))
//                    .createMediaSource(uri)
//        }
//    }

    private fun buildMediaSource2(uri: Uri): MediaSource {
        val factory: DataSource.Factory = DefaultDataSourceFactory(this, "ExoPlayer")
        return ProgressiveMediaSource.Factory(factory).createMediaSource(uri)
    }
}