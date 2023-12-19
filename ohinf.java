import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

 private static final String TAG = "MainActivity";

 private static final int SAMPLE_RATE = 8000;
 private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
 private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
 private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
 private static final int SYNC_TONE_LENGTH = 3 * SAMPLE_RATE;
 private static final int DENOMINATOR = 8;
 private static final int[] FREQUENCIES = {262, 294, 330, 350, 392, 440, 494, 523, 587, 659, 698, 784, 880, 988, 1047, 1175};

 private AudioRecord mAudioRecord;
 private AudioTrack mAudioTrack;
 private boolean mIsRecording = false;
 private boolean mIsPlaying = false;

 @Override
 protected void onCreate(Bundle savedInstanceState) {
     super.onCreate(savedInstanceState);
     setContentView(R.layout.activity_main);

     // Create an AudioRecord object to capture audio data from the microphone
     mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER```

