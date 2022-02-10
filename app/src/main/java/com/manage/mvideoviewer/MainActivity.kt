package com.manage.mvideoviewer

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible


class MainActivity : AppCompatActivity() {

    // Your Video URL
    var videoUrl = "https://www.youtube.com/embed/21X5lGlDOfg?autoplay=1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent = intent
        if (intent.hasExtra("url")) {
            videoUrl = intent.getStringExtra("url").toString()
        }

        findViewById<TextView>(R.id.popup).isVisible = false

        if (videoUrl.contains("youtube", ignoreCase = true)) {
            startYtVideo(videoUrl)
        } else {
            findViewById<TextView>(R.id.webView).isVisible = false
            startVideo(videoUrl)
        }

    }


    private fun startVideo(videoUrl: String) {
        // finding videoview by its id
        val videoView = findViewById<VideoView>(R.id.videoView)

        // Uri object to refer the
        // resource from the videoUrl
        val uri: Uri = Uri.parse(videoUrl)

        // sets the resource from the
        // videoUrl to the videoView
        videoView.setVideoURI(uri)

        // creating object of
        // media controller class
        val mediaController = MediaController(this)

        // sets the anchor view
        // anchor view for the videoView
        mediaController.setAnchorView(videoView)

        // sets the media player to the videoView
        mediaController.setMediaPlayer(videoView)

        // sets the media controller to the videoView
        videoView.setMediaController(mediaController)
        videoView.setOnErrorListener(MediaPlayer.OnErrorListener { mp, what, extra ->
            val popup = findViewById<TextView>(R.id.popup)
            popup.isVisible = true
            findViewById<TextView>(R.id.videoView).isVisible = false
            popup.text = "Error code: $what; Extra: $extra"
            false
        })
        // starts the video
        videoView.start()
    }


    private fun startYtVideo(url: String) {

        val displayYoutubeVideo = findViewById<WebView>(R.id.webView)
        displayYoutubeVideo.setWebViewClient(AutoPlayVideoWebViewClient())
        //displayYoutubeVideo.webChromeClient = WebChromeClient()
        displayYoutubeVideo.settings.mediaPlaybackRequiresUserGesture = false
        displayYoutubeVideo.settings.javaScriptEnabled = true
        displayYoutubeVideo.settings.useWideViewPort = true

        displayYoutubeVideo.loadUrl(url)
    }

}


private class AutoPlayVideoWebViewClient : WebViewClient() {
    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        // mimic onClick() event on the center of the WebView
        val delta: Long = 100
        val downTime = SystemClock.uptimeMillis()
        val x = (view.left + view.width / 2).toFloat()
        val y = (view.top + view.height / 2).toFloat()
        val tapDownEvent = MotionEvent.obtain(downTime, downTime + delta, MotionEvent.ACTION_DOWN, x, y, 0)
        tapDownEvent.source = InputDevice.SOURCE_CLASS_POINTER
        val tapUpEvent = MotionEvent.obtain(downTime, downTime + delta + 2, MotionEvent.ACTION_UP, x, y, 0)
        tapUpEvent.source = InputDevice.SOURCE_CLASS_POINTER
        view.dispatchTouchEvent(tapDownEvent)
        view.dispatchTouchEvent(tapUpEvent)
    }
}

