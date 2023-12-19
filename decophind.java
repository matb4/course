public class MainActivity extends AppCompatActivity {
   private AudioRecord audioRecord;
   private AudioTrack audioTrack;
   private boolean isRecording = false;
   private boolean isPlaying = false;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       setContentView(R.layout.activity_main);

       int sampleRate = 44100; // Sample rate in Hz
       int channelConfig = AudioFormat.CHANNEL_OUT_MONO; // Mono audio
       int audioFormat = AudioFormat.ENCODING_PCM_16BIT; // 16-bit PCM audio
       int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat); // Minimum buffer size

       audioTrack = new AudioTrack(
               AudioManager.STREAM_MUSIC,
               sampleRate,
               channelConfig,
               audioFormat,
               bufferSize,
               AudioTrack.MODE_STREAM);

       audioRecord = new AudioRecord(
               MediaRecorder.AudioSource.MIC,
               sampleRate,
               channelConfig,
               audioFormat,
               bufferSize);

       audioRecord.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
           @Override
           public void onMarkerReached(AudioRecord recorder) {
               // Handle end of recording
               isRecording = false;
               recorder.stop();
               recorder.release();
           }

           @Override
           public void onPeriodicNotification(AudioRecord recorder) {
               // Handle periodic notification
           }
       });

       audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
           @Override
           public void onMarkerReached(AudioTrack track) {
               // Handle end of playback
               isPlaying = false;
               track.stop();
               track.release();
           }

           @Override
           public void onPeriodicNotification(AudioTrack track) {
               // Handle periodic notification
           }
       });
   }

   @Override
   protected void onDestroy() {
       super.onDestroy();
       if (isRecording) {
           audioRecord.stop();
           audioRecord.release();
       }
       if (isPlaying) {
           audioTrack.stop();
           audioTrack.release();
       }
   }

   public void startRecording() {
       if (!isRecording) {
           audioRecord.startRecording();
           isRecording = true;
       }
   }

   public void stopRecording() {
       if (isRecording) {
           audioRecord.stop();
           isRecording = false;
       }
   }

   public void startPlaying() {
       if (!isPlaying) {
           audioTrack.play();
           isPlaying = true;
       }
   }

   public void stopPlaying() {
       if (isPlaying) {
           audioTrack.stop();
           isPlaying = false;
       }
   }

   public void decodeAndPlay() {
       // Decode the audio data and play it
       // This is a simplified example and might not work perfectly for all cases
       short[] buffer = new short[bufferSize];
       while (isRecording) {
           int read = audioRecord.read(buffer, 0, buffer.length);
           if (read > 0) {
               for (int i = 0; i < read; i++) {
                  double frequency = FREQUENCIES[buffer[i]];
                  double angle = 2 * Math.PI * frequency * (i / (double) sampleRate);
                  buffer[i] = (short) (Math.sin(angle) * Short.MAX_VALUE);
               }
               audioTrack.write(buffer, 0, read);
           }
       }
   }
}

