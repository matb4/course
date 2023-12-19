import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class MainActivity : AppCompatActivity() {

  private val TAG = "MainActivity"

  private val SAMPLE_RATE = 8000
  private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
  private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
  private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
  private val SYNC_TONE_LENGTH = 3 * SAMPLE_RATE
  private val DENOMINATOR = 8
  private val FREQUENCIES = intArrayOf(262, 294, 330, 350, 392, 440, 494, 523, 587, 659, 698, 784, 880, 988, 1047, 1175)

  private lateinit var mAudioRecord: AudioRecord
  private lateinit var mAudioTrack: AudioTrack
  private var mIsRecording = false
  private var mIsPlaying = false

  override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_main)

      mAudioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE)
      mAudioTrack = AudioTrack(
          AudioManager.STREAM_MUSIC,
          SAMPLE_RATE,
          CHANNEL_CONFIG,
          AUDIO_FORMAT,
          BUFFER_SIZE,
          AudioTrack.MODE_STREAM
      )

      mAudioRecord.startRecording()
      mIsRecording = true

      DecodeAudioTask().execute()
  }

  override fun onDestroy() {
      super.onDestroy()

      mIsRecording = false
      mAudioRecord.stop()
      mAudioRecord.release()

      mIsPlaying = false
      mAudioTrack.stop()
      mAudioTrack.release()
  }

  private inner class DecodeAudioTask : AsyncTask<Void, Void, IntArray>() {

      override fun doInBackground(vararg params: Void?): IntArray {
          val syncSamples = ShortArray(SYNC_TONE_LENGTH)
          val samples = ShortArray(SAMPLE_RATE / DENOMINATOR)
          var numBits = 0

          for (i in 0 until SYNC_TONE_LENGTH) {
              syncSamples[i] = readSample()
          }

          numBits = (mAudioRecord.bufferSizeInFrames / DENOMINATOR) / (SAMPLE_RATE / DENOMINATOR)
          val decoded = IntArray(numBits)
          val norm = SAMPLE_RATE.toDouble() / SYNC_TONE_LENGTH
          for (i in 0 until numBits) {
              for (j in 0 until SAMPLE_RATE / DENOMINATOR) {
                samples[j] = readSample()
              }
              val freq = getMaxFrequency(samples, norm, FREQUENCIES[FREQUENCIES.size - 1] + 50)
              val closestIndex = getClosestIndex(FREQUENCIES, freq)
              decoded[i] = closestIndex
          }

          return decoded
      }

      override fun onPostExecute(decoded: IntArray) {
          super.onPostExecute(decoded)

          Log.d(TAG, "Decoded message: ${Arrays.toString(decoded)}")

          encodeAndPlay(decoded)
      }

      private fun readSample(): Short {
          val buffer = ByteArray(2)
          mAudioRecord.read(buffer, 0, 2)
          return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).short
      }

      private fun getMaxFrequency(samples: ShortArray, norm: Double, maxAcceptable: Int): Int {
          var maxI = 0
          var maxFreq = 0.0
          val transformedSamples = DoubleArray(samples.size)
          for (i in samples.indices) {
              transformedSamples[i] = samples[i].toDouble()
          }
          val fft = FFT(transformedSamples.size)
          fft.forward(transformedSamples)
          for (i in 1 until transformedSamples.size) {
              val j = (norm * i).toInt()
              val a = Math.sqrt(Math.pow(transformedSamples[i], 2.0) + Math.pow(transformedSamples[transformedSamples.size - i], 2.0))
              if (a > maxFreq && j < maxAcceptable) {
                maxI = i
                maxFreq = a
              }
          }
          return maxI
      }

      private fun getClosestIndex(frequencies: IntArray, freq: Int): Int {
          var closestIndex = 0
          var closestDiff = Integer.MAX_VALUE
          for (i in frequencies.indices) {
              val diff = Math.abs(frequencies[i] - freq)
              if (diff < closestDiff) {
                closestIndex = i
                closestDiff = diff
              }
          }
          return closestIndex
      }
  }

  private class FFT(size: Int) {
      private val data: DoubleArray
      private val w: DoubleArray

      init {
         ```

