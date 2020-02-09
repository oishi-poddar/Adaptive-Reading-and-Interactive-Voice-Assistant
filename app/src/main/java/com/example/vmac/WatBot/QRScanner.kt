package com.example.vmac.WatBot

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
//import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import kotlinx.android.synthetic.main.activity_qrscanner.*
import android.os.StrictMode
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import com.example.vmac.WatBot.singleton.SpeakerManager
import com.mapzen.speakerbox.Speakerbox
import java.util.*


class QRScanner : AppCompatActivity() {
    private lateinit var codeScanner: CodeScanner
    private var speakerManager: SpeakerManager = SpeakerManager.getInstance()
    private lateinit var speakerBox: Speakerbox
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrscanner)
        val scannerView = scanner_view
        speakerBox = Speakerbox(application)
        speakerBox.unmute()
        speakerBox.play("Scan the document")
        speakerBox.mute()
        codeScanner = CodeScanner(this, scannerView)
        // Parameters (default values)
        codeScanner.camera = CodeScanner.CAMERA_BACK // or CAMERA_FRONT or specific camera id
        codeScanner.formats = CodeScanner.ALL_FORMATS // list of type BarcodeFormat,
        // ex. listOf(BarcodeFormat.QR_CODE)
        codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
        codeScanner.scanMode = ScanMode.SINGLE // or CONTINUOUS or PREVIEW
        codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
        codeScanner.isFlashEnabled = false // Whether to enable flash or not

        // Callbacks
        codeScanner.decodeCallback = DecodeCallback {
            runOnUiThread {
//                downloadManager = applicationContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//                val uri = Uri.parse(it.text)
//                val request = DownloadManager.Request(uri)
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                    request.allowScanningByMediaScanner()
//                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//                }
                //val reference = downloadManager.enqueue(request)
                Toast.makeText(this, "Scan result: ${it.text}", Toast.LENGTH_LONG).show()
                var intent = Intent(this,FileViewer1::class.java)
                intent.putExtra("url",it.text);
                vibrator(500);
                startActivity(intent)
            }
        }
        codeScanner.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
            runOnUiThread {
                Toast.makeText(this, "Camera initialization error: ${it.message}",
                        Toast.LENGTH_LONG).show()
                vibrator(2000);
            }
        }

        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    fun vibrator(time: Long){
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(time)
        }
    }
}
