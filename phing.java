import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioProcessor {
   private static final int SAMPLE_RATE = 44100;
   private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
   private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
   private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

   private AudioRecord audioRecord;
   private Thread recordingThread;
   private boolean isRecording = false;

   public AudioProcessor() {
       audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
   }

   public void startRecording() {
       if (isRecording) return;
       isRecording = true;
       recordingThread = new Thread(new Runnable() {
           @Override
           public void run() {
               audioRecord.startRecording();
               while (isRecording) {
                  short[] buffer = new short[BUFFER_SIZE];
                  int read = audioRecord.read(buffer, 0, BUFFER_SIZE);
                  if (read > 0) {
                      processAudioData(buffer);
                  }
               }
               audioRecord.stop();
           }
       });
       recordingThread.start();
   }

   public void stopRecording() {
       if (!isRecording) return;
       isRecording = false;
       try {
           recordingThread.join();
       } catch (InterruptedException e) {
           e.printStackTrace();
       }
   }

   private void processAudioData(short[] buffer) {
       // Process the audio data here...
   }

   public class FFT {
       // The FFT class code goes here...
   }

   private int getClosestIndex(int[] array, int target) {
       int closestIndex = 0;
       int closestValue = Math.abs(array[0] - target);
       for (int i = 1; i < array.length; i++) {
           int value = Math.abs(array[i] - target);
           if (value < closestValue) {
               closestValue = value;
               closestIndex = i;
           }
       }
       return closestIndex;
   }
}

