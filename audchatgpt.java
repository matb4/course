Sure, I can help you modify the code to work as an Android app. Here's a modified version of the code that you can use as a starting point for your Android app:

```java
import android.media.AudioFormat;
import android.media.AudioRecord;
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
    private boolean mIsRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create an AudioRecord object to capture audio data from the microphone
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);

        // Start capturing audio data
        mAudioRecord.startRecording();
        mIsRecording = true;

        // Start decoding the audio data in a separate thread
        new DecodeAudioTask().execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop capturing audio data
        mIsRecording = false;
        mAudioRecord.stop();
        mAudioRecord.release();
    }

    private class DecodeAudioTask extends AsyncTask<Void, Void, int[]> {

        @Override
        protected int[] doInBackground(Void... voids) {
            short[] syncSamples = new short[SYNC_TONE_LENGTH];
            short[] samples = new short[SAMPLE_RATE / DENOMINATOR];
            int numBits = 0;

            // Read the sync tone samples from the audio stream
            for (int i = 0; i < SYNC_TONE_LENGTH; i++) {
                syncSamples[i] = readSample();
            }

            // Read the audio stream and decode the frequency of each sample
            numBits = (mAudioRecord.getBufferSizeInFrames() / DENOMINATOR) / (SAMPLE_RATE / DENOMINATOR);
            int[] decoded = new int[numBits];
            double norm = (double) SAMPLE_RATE / (double) SYNC_TONE_LENGTH;
            for (int i = 0; i < numBits; i++) {
                for (int j = 0; j < SAMPLE_RATE / DENOMINATOR; j++) {
                    samples[j] = readSample();
                }
                int freq = getMaxFrequency(samples, norm, FREQUENCIES[FREQUENCIES.length - 1] + 50);
                int closestIndex = getClosestIndex(FREQUENCIES, freq);
                decoded[i] = closestIndex;
            }

            return decoded;
        }

        @Override
        protected void onPostExecute(int[] decoded) {
            super.onPostExecute(decoded);

            // Print the decoded message to the logcat
            Log.d(TAG, "Decoded message: " + Arrays.toString(decoded));
        }

        private short readSample() {
            byte[] buffer = new byte[2];
            mAudioRecord.read(buffer, 0, 2);
            return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getShort();
        }

        private int getMaxFrequency(short[] samples, double norm, int maxAcceptable) {
            int maxI = 0;
            double maxFreq = 0.0;
            double[] transformedSamples = new double[samples.length];
            for (int i = 0; i < samples.length; i++) {
                transformedSamples[i] = samples[i];
            }
            FFT fft = new FFT(transformedSamples.length);
            fft.forward(transformedSamples);
            for (int i = 1; i < transformedSamples.length; i++) {
                int j = (int) (norm * i);
                double a = Math.sqrt(Math.pow(transformedSamples[i], 2) + Math.pow(transformedSamples[transformedSamples.length - i], 2));
                if (a > maxFreq && j < maxAcceptable) {
                    maxFreq = a;
                    maxI = j;
                }
            }
            return maxI;
        }

        private int getClosestIndexSorry about that, it seems like the end of the code snippet got cut off. Here is the complete modified code:

```java
import android.media.AudioFormat;
import android.media.AudioRecord;
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
    private boolean mIsRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create an AudioRecord object to capture audio data from the microphone
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);

        // Start capturing audio data
        mAudioRecord.startRecording();
        mIsRecording = true;

        // Start decoding the audio data in a separate thread
        new DecodeAudioTask().execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop capturing audio data
        mIsRecording = false;
        mAudioRecord.stop();
        mAudioRecord.release();
    }

    private class DecodeAudioTask extends AsyncTask<Void, Void, int[]> {

        @Override
        protected int[] doInBackground(Void... voids) {
            short[] syncSamples = new short[SYNC_TONE_LENGTH];
            short[] samples = new short[SAMPLE_RATE / DENOMINATOR];
            int numBits = 0;

            // Read the sync tone samples from the audio stream
            for (int i = 0; i < SYNC_TONE_LENGTH; i++) {
                syncSamples[i] = readSample();
            }

            // Read the audio stream and decode the frequency of each sample
            numBits = (mAudioRecord.getBufferSizeInFrames() / DENOMINATOR) / (SAMPLE_RATE / DENOMINATOR);
            int[] decoded = new int[numBits];
            double norm = (double) SAMPLE_RATE / (double) SYNC_TONE_LENGTH;
            for (int i = 0; i < numBits; i++) {
                for (int j = 0; j < SAMPLE_RATE / DENOMINATOR; j++) {
                    samples[j] = readSample();
                }
                int freq = getMaxFrequency(samples, norm, FREQUENCIES[FREQUENCIES.length - 1] + 50);
                int closestIndex = getClosestIndex(FREQUENCIES, freq);
                decoded[i] = closestIndex;
            }

            return decoded;
        }

        @Override
        protected void onPostExecute(int[] decoded) {
            super.onPostExecute(decoded);

            // Print the decoded message to the logcat
            Log.d(TAG, "Decoded message: " + Arrays.toString(decoded));
        }

        private short readSample() {
            byte[] buffer = new byte[2];
            mAudioRecord.read(buffer, 0, 2);
            return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getShort();
        }

        private int getMaxFrequency(short[] samples, double norm, int maxAcceptable) {
            int maxI = 0;
            double maxFreq = 0.0;
            double[] transformedSamples = new double[samples.length];
            for (int i = 0; i < samples.length; i++) {
                transformedSamples[i] = samples[i];
            }
            FFT fft = new FFT(transformedSamples.length);
            fft.forward(transformedSamples);
            for (int i = 1; i < transformedSamples.length; i++) {
                int j = (int) (norm * i);
                double a = Math.sqrt(Math.pow(transformedSamples[i], 2) + Math.pow(transformedSamples[transformedSamples.length - i], 2));
                if (a > maxFreq && j < maxAcceptable) {
                    maxFreq = a;
                    maxI = j;
                }
            }
            return maxI;
        }

        private int getClosestIndex(int[] array, int value) {
            int closestIndex =
