import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class WalkieTalkieMessengerActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String TAG = "WalkieTalkieMessenger";

    private Button btnStart, btnStop;
    private EditText etFrequency;

    private AudioRecord recorder;
    private AudioTrack player;
    private DatagramSocket socket;
    private InetAddress address;
    private int port;

    private boolean isRecording = false;
    private boolean isPlaying = false;

    private byte[] key;
    private byte[] iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walkie_talkie_messenger);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        etFrequency = findViewById(R.id.etFrequency);

        // Request permission to record audio
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);

        // Initialize the audio recorder
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize);

        // Initialize the audio player
        player = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioFormat, bufferSize, AudioTrack.MODE_STREAM);

        // Initialize the socket
        try {
            socket = new DatagramSocket();
            address = InetAddress.getByName("192.168.1.100");
            port = 5000;
        } catch (SocketException e) {
            Log.e(TAG, "Error creating socket", e);
        } catch (UnknownHostException e) {
            Log.e(TAG, "Error getting IP address", e);
        }

        // Generate a random key and IV
        key = new byte[16];
        iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(key);
        random.nextBytes(iv);

        // Set up the start button
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start recording
                recorder.startRecording();
                isRecording = true;

                // Start playing
                player.play();
                isPlaying = true;

                // Start sending data
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isRecording) {
                            // Read data from the recorder
                            byte[] buffer = new byte[bufferSize];
                            int bytesRead = recorder.read(buffer, 0, bufferSize);

                            // Encrypt the data
                            byte[] encryptedData = encrypt(buffer);

                            // Send the data to the other walkie-talkie
                            DatagramPacket packet = new DatagramPacket(encryptedData, encryptedData.length, address, port);
                            try {
                                socket.send(packet);
                            } catch (IOException e) {
                                Log.e(TAG, "Error sending data", e);
                            }
                        }
                    }
                }).start();

                // Start receiving data
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isPlaying) {
                            // Receive data from the other walkie-talkie
                            byte[] buffer = new byte[bufferSize];
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                            try {
                                socket.receive(packet);
                            } catch (IOException e) {
                                Log.e(TAG, "Error receiving data", e);
                            }

                            // Decrypt the data
                            byte[] decryptedData = decrypt(buffer);

                            // Play the data
                            player.write(decryptedData, 0, decryptedData.length);
                        }
                    }
                }).start();
            }
        });

        // Set up the stop button
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop recording
                recorder.stop();
                isRecording = false;

                // Stop playing
                player.stop();
                isPlaying = false;

                // Close the socket
                socket.close();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                } else {
                    // Permission denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private byte[] encrypt(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            Log.e(TAG, "Error encrypting data", e);
            return null;
        }
    }

    private byte[] decrypt(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            Log.e(TAG, "Error decrypting data", e);
            return null;
        }
    }
}
