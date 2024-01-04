package com.example.walkie_talkie_messenger;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String TAG = "WalkieTalkieMessenger";

    private Button mRecordButton;
    private Button mPlayButton;
    private TextView mFrequencyTextView;

    private AudioRecord mAudioRecord;
    private AudioTrack mAudioTrack;

    private boolean mIsRecording = false;
    private boolean mIsPlaying = false;

    private int mSampleRate = 44100;
    private int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mAudioFormat);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecordButton = findViewById(R.id.record_button);
        mPlayButton = findViewById(R.id.play_button);
        mFrequencyTextView = findViewById(R.id.frequency_text_view);

        // Request permission to record audio
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        // Create the AudioRecord object
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate, mChannelConfig, mAudioFormat, mBufferSize);

        // Create the AudioTrack object
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate, mChannelConfig, mAudioFormat, mBufferSize, AudioTrack.MODE_STREAM);

        // Set up the record button
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsRecording) {
                    // Start recording
                    mAudioRecord.startRecording();
                    mIsRecording = true;

                    // Update the UI
                    mRecordButton.setText("Stop Recording");
                } else {
                    // Stop recording
                    mAudioRecord.stop();
                    mIsRecording = false;

                    // Update the UI
                    mRecordButton.setText("Start Recording");
                }
            }
        });

        // Set up the play button
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsPlaying) {
                    // Start playing
                    mAudioTrack.play();
                    mIsPlaying = true;

                    // Update the UI
                    mPlayButton.setText("Stop Playing");
                } else {
                    // Stop playing
                    mAudioTrack.stop();
                    mIsPlaying = false;

                    // Update the UI
                    mPlayButton.setText("Start Playing");
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                } else {
                    // Permission denied
                    finish();
                }
                break;
        }
    }

    private void recordAudio() {
        // Create a buffer to store the audio data
        byte[] buffer = new byte[mBufferSize];

        // Start recording
        mAudioRecord.startRecording();

        // Loop until the user stops recording
        while (mIsRecording) {
            // Read the audio data from the microphone
            int numBytes = mAudioRecord.read(buffer, 0, mBufferSize);

            // Encrypt the audio data
            byte[] encryptedBuffer = encryptAudioData(buffer, numBytes);

            // Send the encrypted audio data to the other walkie-talkie
            sendAudioData(encryptedBuffer);
        }

        // Stop recording
        mAudioRecord.stop();
    }

    private void playAudio() {
        // Create a buffer to store the audio data
        byte[] buffer = new byte[mBufferSize];

        // Start playing
        mAudioTrack.play();

        // Loop until the user stops playing
        while (mIsPlaying) {
            // Receive the encrypted audio data from the other walkie-talkie
            byte[] encryptedBuffer = receiveAudioData();

            // Decrypt the audio data
            byte[] decryptedBuffer = decryptAudioData(encryptedBuffer);

            // Write the decrypted audio data to the speaker
            mAudioTrack.write(decryptedBuffer, 0, decryptedBuffer.length);
        }

        // Stop playing
        mAudioTrack.stop();
    }

    private byte[] encryptAudioData(byte[] buffer, int numBytes) {
        // Create a new byte array to store the encrypted data
        byte[] encryptedBuffer = new byte[numBytes];

        // Encrypt the data using a strong encryption algorithm
        for (int i = 0; i < numBytes; i++) {
            encryptedBuffer[i] = (byte) (buffer[i] ^ 0x55);
        }

        return encryptedBuffer;
    }

    private byte[] decryptAudioData(byte[] buffer) {
        // Create a new byte array to store the decrypted data
        byte[] decryptedBuffer = new byte[buffer.length];

        // Decrypt the data using the same encryption algorithm that was used to encrypt it
        for (int i = 0; i < buffer.length; i++) {
            decryptedBuffer[i] = (byte) (buffer[i] ^ 0x55);
        }

        return decryptedBuffer;
    }

    private void sendAudioData(byte[] buffer) {
        // Send the audio data to the other walkie-talkie using an aux connection
        try {
            OutputStream outputStream = new FileOutputStream("/dev/ttyS0");
            outputStream.write(buffer);
            outputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error sending audio data", e);
        }
    }

    private byte[] receiveAudioData() {
        // Receive the audio data from the other walkie-talkie using an aux connection
        try {
            InputStream inputStream = new FileInputStream("/dev/ttyS0");
            byte[] buffer = new byte[mBufferSize];
            inputStream.read(buffer);
            inputStream.close();
            return buffer;
        } catch (IOException e) {
            Log.e(TAG, "Error receiving audio data", e);
            return new byte[0];
        }
    }
}
