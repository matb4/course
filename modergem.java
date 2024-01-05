import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class WalkieTalkieMessengerActivity extends AppCompatActivity {

    private static final String TAG = "WalkieTalkieMessenger";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private EditText frequencyEditText;
    private Button startButton;
    private Button stopButton;
    private TextView statusTextView;

    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private DatagramSocket datagramSocket;
    private InetAddress broadcastAddress;
    private int port = 5005;

    private boolean isRecording = false;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walkie_talkie_messenger);

        frequencyEditText = findViewById(R.id.frequencyEditText);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        statusTextView = findViewById(R.id.statusTextView);

        // Request permission to record audio
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        // Initialize the audio record and track
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioFormat, bufferSize, AudioTrack.MODE_STREAM);

        // Initialize the datagram socket
        try {
            datagramSocket = new DatagramSocket();
            broadcastAddress = InetAddress.getByName("255.255.255.255");
        } catch (SocketException e) {
            Log.e(TAG, "Error initializing datagram socket", e);
            Toast.makeText(this, "Error initializing datagram socket", Toast.LENGTH_SHORT).show();
            return;
        } catch (UnknownHostException e) {
            Log.e(TAG, "Error getting broadcast address", e);
            Toast.makeText(this, "Error getting broadcast address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Set up the start button
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start recording and playing audio
                isRecording = true;
                isPlaying = true;
                audioRecord.startRecording();
                audioTrack.play();

                // Start sending audio data over the network
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isRecording) {
                            byte[] buffer = new byte[bufferSize];
                            audioRecord.read(buffer, 0, bufferSize);
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, port);
                            try {
                                datagramSocket.send(packet);
                            } catch (IOException e) {
                                Log.e(TAG, "Error sending audio data over the network", e);
                            }
                        }
                    }
                }).start();

                // Start receiving audio data over the network
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isPlaying) {
                            byte[] buffer = new byte[bufferSize];
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                            try {
                                datagramSocket.receive(packet);
                            } catch (IOException e) {
                                Log.e(TAG, "Error receiving audio data over the network", e);
                            }
                            audioTrack.write(buffer, 0, buffer.length);
                        }
                    }
                }).start();

                // Update the status text view
                statusTextView.setText("Recording and playing audio");
            }
        });

        // Set up the stop button
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop recording and playing audio
                isRecording = false;
                isPlaying = false;
                audioRecord.stop();
                audioTrack.stop();

                // Update the status text view
                statusTextView.setText("Stopped recording and playing audio");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}

