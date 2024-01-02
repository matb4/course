import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class MainActivity : AppCompatActivity() {

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private val TAG = "MainActivity"

    private lateinit var recordButton: Button
    private lateinit var sendButton: Button
    private lateinit var receiveButton: Button
    private lateinit var playButton: Button

    private lateinit var outputFile: File
    private lateinit var encryptedFile: File
    private lateinit var receivedFile: File
    private lateinit var decryptedFile: File

    private var manualFrequency = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recordButton = findViewById(R.id.record_button)
        sendButton = findViewById(R.id.send_button)
        receiveButton = findViewById(R.id.receive_button)
        playButton = findViewById(R.id.play_button)

        // Request RECORD_AUDIO permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
            return
        }

        // Initialize output file
        outputFile = File(Environment.getExternalStorageDirectory(), "recorded_audio.mp4")

        // Initialize encrypted file
        encryptedFile = File(Environment.getExternalStorageDirectory(), "encrypted_audio.mp4")

        // Initialize received file
        receivedFile = File(Environment.getExternalStorageDirectory(), "received_audio.mp4")

        // Initialize decrypted file
        decryptedFile = File(Environment.getExternalStorageDirectory(), "decrypted_audio.mp4")

        // Add Bouncy Castle provider
        java.security.Security.addProvider(BouncyCastleProvider())

        // Set up record button click listener
        recordButton.setOnClickListener {
            recordAudio()
        }

        // Set up send button click listener
        sendButton.setOnClickListener {
            encryptAudio()
            sendAudio()
        }

        // Set up receive button click listener
        receiveButton.setOnClickListener {
            receiveAudio()
        }

        // Set up play button click listener
        playButton.setOnClickListener {
            decryptAudio()
            playAudio()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied to record audio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun recordAudio() {
        // Get the user's choice of frequency
        val manualFrequencyCheckbox = findViewById<CheckBox>(R.id.manual_frequency_checkbox)
        manualFrequency = manualFrequencyCheckbox.isChecked
        var frequency = 44100 // Default frequency

        // If the user wants to choose the frequency manually, get the input
        if (manualFrequency) {
            val frequencyEditText = findViewById<EditText>(R.id.frequency_edit_text)
            try {
                frequency = frequencyEditText.text.toString().toInt()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Invalid frequency", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Validate the frequency
        if (frequency < 44100 || frequency > 48000) {
            Toast.makeText(this, "Frequency must be between 44100 and 48000 Hz", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a new audio recorder
        val recorder = MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setAudioSamplingRate(frequency)
        recorder.setOutputFile(outputFile.absolutePath)

        // Start recording
        try {
            recorder.prepare()
            recorder.start()

            // Record for 5 seconds
            Thread.sleep(5000)

            // Stop recording
            recorder.stop()
            recorder.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error recording audio", e)
        }
    }

    private fun encryptAudio() {
        try {
            // Generate a random AES key
            val key = ByteArray(16)
            val random = SecureRandom()
            random.nextBytes(key)

            // Create a cipher object
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))

            // Encrypt the audio file
            val fis = FileInputStream(outputFile)
            val fos = FileOutputStream(encryptedFile)
            val cos = CipherOutputStream(fos, cipher)
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                cos.write(buffer, 0, bytesRead)
            }
            fis.close()
            fos.close()
            cos.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error encrypting audio", e)
        }
    }

    private fun sendAudio() {
        // Create a new audio track
        val audioTrack = AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM)

        // Start playing the audio track
        audioTrack.play()

        // Read the encrypted audio file and write it to the audio track
        try {
            val fis = FileInputStream(encryptedFile)
            val buffer = ByteArray(AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT))
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                audioTrack.write(buffer, 0, bytesRead)
            }
            fis.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending audio", e)
        }

        // Stop playing the audio track
        audioTrack.stop()
        audioTrack.release()
    }

    private fun receiveAudio() {
        // Create a new audio track
        val audioTrack = AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM)

        // Start playing the audio track
        audioTrack.play()

        // Read the encrypted audio file from the aux cable and write it to the audio track
        try {
            val audioRecord = AudioRecord(MediaRecorder.AudioSource.DEFAULT, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT))
            audioRecord.startRecording()
            val buffer = ByteArray(AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT))
            var bytesRead: Int
            while (audioRecord.read(buffer, 0, AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)).also { bytesRead = it } != -1) {
                audioTrack.write(buffer, 0, bytesRead)
            }
            audioRecord.stop()
            audioRecord.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error receiving audio", e)
        }

        // Stop playing the audio track
        audioTrack.stop()
        audioTrack.release()
    }

    private fun decryptAudio() {
        try {
            // Generate the same AES key as used for encryption
            val key = ByteArray(16)
            val random = SecureRandom()
            random.nextBytes(key)

            // Create a cipher object
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))

            // Decrypt the audio file
            val fis = FileInputStream(receivedFile)
            val fos = FileOutputStream(decryptedFile)
            val cos = CipherOutputStream(fos, cipher)
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                cos.write(buffer, 0, bytesRead)
            }
            fis.close()
            fos.close()
            cos.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting audio", e)
        }
    }

    private fun playAudio() {
        // Create a new media player
        val mediaPlayer = MediaPlayer()

        try {
            // Set the data source to the decrypted audio file
            mediaPlayer.setDataSource(decryptedFile.absolutePath)

            // Prepare the media player
            mediaPlayer.prepare()

            // Start playing the audio
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
        }
    }
}
