import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

  private static final int SAMPLE_RATE = 44100;
  private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
  private static final String SECRET_KEY = "YourSecretKey";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      new Thread(new Runnable() {
          @Override
          public void run() {
              try {
                // Audio Capture
                AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
                audioRecord.startRecording();
                byte[] buffer = new byte[BUFFER_SIZE];
                int read = audioRecord.read(buffer, 0, BUFFER_SIZE);

                // Voice Encoding
                Cipher cipher = Cipher.getInstance("AES");
                SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                byte[] encryptedData = cipher.doFinal(buffer);

                // Transmitting the Encoded Data
                FileOutputStream auxFile = new FileOutputStream("/dev/aux");
                auxFile.write(encryptedData);
                auxFile.close();

                // Data Decoding
                FileInputStream auxFile = new FileInputStream("/dev/aux");
                byte[] receivedData = new byte[BUFFER_SIZE];
                auxFile.read(receivedData);
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
                byte[] decryptedData = cipher.doFinal(receivedData);

                // Audio Reproduction
                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE, AudioTrack.MODE_STREAM);
                audioTrack.write(decryptedData, 0, decryptedData.length);

              } catch (Exception e) {
                Log.e("MainActivity", "Error", e);
              }
          }
      }).start();
  }
}




<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

