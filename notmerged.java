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
                                                                                                                                                            private static final int SAMPLE_RATE = 8000;                                                                                                            private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;                                                                                  private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;                                                                                 private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    private static final int SYNC_TONE_LENGTH = 3 * SAMPLE_RATE;
    private static final int DENOMINATOR = 8;
    private static final int[] FREQUENCIES = {262, 294, 330, 350, 392, 440, 494, 523, 587, 659, 698, 784, 880, 988, 1047, 1175};
                                                                                                                                                            private AudioRecord mAudioRecord;
    private boolean mIsRecording = false;                                                                                                                                                                                                                                                                           @Override
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
                                                                                                                                                                // Stop capturing audio data                                                                                                                            mIsRecording = false;
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
                for (int j = 0; j < SAMPLE_RATE / DENOMINATOR; j++) {                                                                                                       samples[j] = readSample();                                                                                                                          }                                                                                                                                                       int freq = getMaxFrequency(samples, norm, FREQUENCIES[FREQUENCIES.length - 1] + 50);
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


import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;                                                                                                                             import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

public class AudioDecoder {

    private static final String TAG = "AudioDecoder";

    public static void decodeAudio(String filename, int sourceSampleFreq, int denominator, int[] frequencies) {
        AssetManager assetManager = getAssets();
        try {
            AssetFileDescriptor afd = assetManager.openFd(filename);
            InputStream fh = afd.createInputStream();
            byte[] header = new byte[44];
            fh.read(header);
            int id = readIntLittleEndian(header, 0);
            int size = readIntLittleEndian(header, 4);
            int type = readIntLittleEndian(header, 8);
            int formatTag = readShortLittleEndian(header, 14);
            int channels = readShortLittleEndian(header, 16);
            int samplesPerSec = readIntLittleEndian(header, 18);
            int bytesPerSec = readIntLittleEndian(header, 22);
            int blockAlign = readShortLittleEndian(header, 32);
            int bitsPerSample = readShortLittleEndian(header, 34);
            Log.d(TAG, "channels: " + channels);
            Log.d(TAG, "samplesPerSec: " + samplesPerSec);                                                                                                          Log.d(TAG, "bytesPerSec: " + bytesPerSec);
            Log.d(TAG, "bitsPerSample: " + bitsPerSample);

            int length = readIntLittleEndian(header, 40);
            Log.d(TAG, "length: " + length);

            int syncToneLength = 3 * samplesPerSec;
            Log.d(TAG, "sync length: " + syncToneLength);
            int[] syncSamples = new int[syncToneLength];
            for (int i = 0; i < syncToneLength; i++) {
                syncSamples[i] = readShortLittleEndian(fh);
            }

            int syncFreq = getMaxFrequency(syncSamples, 1.0 / 3.0);
            if (syncFreq == 440) {
                Log.d(TAG, "Found valid 3-second syncbeep of 440Hz");
            } else {
                Log.d(TAG, "Invalid sync beep: " + syncFreq);
                System.exit(0);
            }

            int halfByteSampleLength = samplesPerSec / denominator;
Log.d(TAG, "half byte sample length: " + halfByteSampleLength);

            StringBuilder buffer = new StringBuilder();
            int j = 0;                                                                                                                                              while (true) {
                try {                                                                                                                                                       int[] halfByte = new int[2];                                                                                                                            for (int i = 0; i < 2; i++) {
                        int[] samples = new int[halfByteSampleLength];
                        for (int k = 0; k < halfByteSampleLength; k++) {
                            samples[k] = readShortLittleEndian(fh);
                        }
                        int freq = getMaxFrequency(samples, (double) samplesPerSec / halfByteSampleLength);
                        int v = getClosestIndex(frequencies, freq);
                        halfByte[i] = v;
                    }

                    int fullByte = halfByte[0] | (halfByte[1] << 4);
                    buffer.append((char) fullByte);
                    j++;
                } catch (Exception e) {
                    break;
                }
            }

            Log.d(TAG, "");
            Log.d(TAG, "decoded data:");                                                                                                                            Log.d(TAG, buffer.toString());                                                                                                                      } catch (IOException e) {                                                                                                                                   e.printStackTrace();
        }
    }

    public static int readIntLittleEndian(byte[] bytes, int offset) {
        return ((bytes[offset + 3] & 0xff) << 24)
                | ((bytes[offset + 2] & 0xff) << 16)
                | ((bytes[offset + 1] & 0xff) << 8)
                | ((bytes[offset] & 0xff));
    }

    public static int readShortLittleEndian(byte[] bytes, int offset) {
        return ((bytes[offset + 1] & 0xff) << 8) | (bytes[offset] & 0xff);
    }

    public static int readShortLittleEndian(InputStream stream) throws IOException {
        byte[] buffer = new byte[2];
        int bytesRead = stream.read(buffer);
        if (bytesRead < 2) throw new IOException("Unexpected end of stream");
        return ((buffer[1] & 0xff) << 8) | (buffer[0] & 0xff);
    }

    public static int getMaxFrequency(int[] samples, double sampleRate) {
        int N = samples.length;
        double[] window = hannWindow(N);
        double[] fftReal = new double[N];
        double[] fftImg = new double[N];
        for (int i = 0; i < N; i++) {
            fftReal[i] = samples[i] * window[i];
        }
        FFT.fft(fftReal, fftImg);
        double[] powerSpectrum = new double[N/2];
        for (int i = 0; i < N/2; i++) {
            powerSpectrum[i] = (fftReal[i]*fftReal[i] + fftImg[i]*fftImg[i]) / (N*N);
        }
        int maxIndex = getMaxIndex(powerSpectrum);
        double freq = maxIndex * sampleRate / N;
        return (int) Math.round(freq);
    }

    public static int getMaxIndex(double[] array) {
        int maxIndex = 0;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];                                                                                                                                         maxIndex = i;
            }
        }
        return maxIndex;
    }

    public static double[] hannWindow(int N) {
        double[] window = new double[N];
        for (int i = 0; i < N; i++) {
            window[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (N - 1)));
        }
        return window;
    }

    public static int getClosestIndex(int[] array, int value) {
        int index = Arrays.binarySearch(array, value);
        if (index < 0) {
            index = -index - 1;
            if (index >= array.length) {
                index = array.length - 1;
            }
            if (index > 0 && Math.abs(array[index] - value) > Math.abs(array[index - 1] - value)) {
                index--;
            }
        }
        return index;                                                                                                                                       }
~
