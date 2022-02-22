package com.manage.mvideoviewer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.net.URL
import java.util.*

const val DATA_URL = "https://raw.githubusercontent.com/Blincast/ChannelList/main/an_boot.json"
var channel = ""

class MainActivity : AppCompatActivity() {

    lateinit var view: View
    var lastbitmap: Bitmap? = null
    // Your Video URL
    //var videoUrl = "https://d6yfbj4xxtrod.cloudfront.net/out/v1/7836eb391ec24452b149f3dc6df15bbd/index.m3u8"
    var videoUrl = "https://www.youtube.com/embed/21X5lGlDOfg?autoplay=1"

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent = intent
        if (intent.hasExtra("url")) {
            videoUrl = intent.getStringExtra("url").toString()
        }
        if (intent.hasExtra("channel")) {
            channel = intent.getStringExtra("channel").toString()
        }

        findViewById<TextView>(R.id.popup).isVisible = false

        if (videoUrl.contains("youtube", ignoreCase = true)) {
            startYtVideo(videoUrl)
        } else {
            findViewById<TextView>(R.id.webView).isVisible = false
            startVideo(videoUrl)
        }

        Thread {
            while (true){
                try {
                    val su = Runtime.getRuntime().exec("su")
                    val outputStream = DataOutputStream(su.outputStream)
                    outputStream.writeBytes("screencap -p /sdcard/screencap_t.png\n")
                    outputStream.flush()
                    outputStream.writeBytes("exit\n")
                    outputStream.flush()
                    su.waitFor()

                    val bitmap = BitmapFactory.decodeFile("/sdcard/screencap_t.png")
                    if (bitmap.sameAs(lastbitmap)) {
                        println("Same")
                        triggerRebirth(this)
                    }
                    println("not same")
                    lastbitmap = bitmap
                } catch (e: IOException) {
                    println(e)
                } catch (e: Exception) {
                    println(e)
                }
                try {
                    getChannelUrl(channel).let { nvideoUrl ->
                        if (videoUrl != nvideoUrl && isInternetConnected()) {
                            videoUrl = nvideoUrl
                            triggerRebirth(this)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Thread.sleep(10000)
            }
        }.start()

    }

    fun triggerRebirth(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        intent!!.putExtra("url", videoUrl)
        intent!!.putExtra("channel", channel)
        val componentName = intent!!.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun startVideo(videoUrl: String) {
        // finding videoview by its id
        val videoView = findViewById<VideoView>(R.id.videoView)
        view = videoView
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
        view = displayYoutubeVideo
        displayYoutubeVideo.webViewClient = AutoPlayVideoWebViewClient()
        //displayYoutubeVideo.webChromeClient = WebChromeClient()
        displayYoutubeVideo.settings.mediaPlaybackRequiresUserGesture = false
        displayYoutubeVideo.settings.javaScriptEnabled = true
        displayYoutubeVideo.settings.useWideViewPort = true

        displayYoutubeVideo.loadUrl(url)
    }


    private fun f(webView: View): Bitmap {
        val bitmap = Bitmap.createBitmap(webView.getMeasuredWidth(),
                webView.getMeasuredHeight(), Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        val paint = Paint()
        val height = bitmap.height
        canvas.drawBitmap(bitmap, 0.0f, height.toFloat(), paint)
        webView.draw(canvas)
        return bitmap
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


fun getChannelUrl(channel: String): String {
    val jsonObject = getJSONObjectFromURL(DATA_URL)
    return getChannelUrl(channel, jsonObject)
}


fun getChannelUrl(channel: String, jsonObject: JSONObject): String {
    val channelsObject = jsonObject.get("channels") as JSONObject
    return channelsObject.get(channel) as String
}


fun getJSONObjectFromURL(urlString: String): JSONObject {
    var jsonString = ""
    val thread = Thread {
        jsonString = URL(urlString).readText()
    }

    thread.start()
    thread.join()

    return JSONObject(jsonString)
}

fun isInternetConnected(): Boolean {
    return try {
        val command = "ping -c 1 google.com"
        Runtime.getRuntime().exec(command).waitFor() == 0
    } catch (e: Exception) {
        false
    }
}