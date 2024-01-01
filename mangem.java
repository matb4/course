import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String TAG = "MainActivity";

    private Button recordButton;
    private Button sendButton;
    private Button receiveButton;
    private Button playButton;

    private File outputFile;
    private File encryptedFile;
    private File receivedFile;
    private File decryptedFile;

    private int bufferSize;
    private boolean manualFrequency = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordButton = findViewById(R.id.record_button);
        sendButton = findViewById(R.id.send_button);
        receiveButton = findViewById(R.id.receive_button);
        playButton = findViewById(R.id.play_button);

        // Request RECORD_AUDIO permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        // Initialize buffer size
        bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        // Initialize output file
        outputFile = new File(Environment.getExternalStorageDirectory(), "recorded_audio.mp4");

        // Initialize encrypted file
        encryptedFile = new File(Environment.getExternalStorageDirectory(), "encrypted_audio.mp4");

        // Initialize received file
        receivedFile = new File(Environment.getExternalStorageDirectory(), "received_audio.mp4");

        // Initialize decrypted file
        decryptedFile = new File(Environment.getExternalStorageDirectory(), "decrypted_audio.mp4");

        // Add Bouncy Castle provider
        java.security.Security.addProvider(new BouncyCastleProvider());

        // Set up record button click listener
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordAudio();
            }
        });

        // Set up send button click listener
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptAudio();
                sendAudio();
            }
        });

        // Set up receive button click listener
        receiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receiveAudio();
            }
        });

        // Set up play button click listener
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptAudio();
                playAudio();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied to record audio", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void recordAudio() {
        // Get the user's choice of frequency
        CheckBox manualFrequencyCheckbox = findViewById(R.id.manual_frequency_checkbox);
        manualFrequency = manualFrequencyCheckbox.isChecked();
        int frequency = 44100; // Default frequency

        // If the user wants to choose the frequency manually, get the input
        if (manualFrequency) {
            EditText frequencyEditText = findViewById(R.id.frequency_edit_text);
            frequency = Integer.parseInt(frequencyEditText.getText().toString());
        }

        // Create a new audio recorder
        MediaRecorder recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioSamplingRate(frequency);
        recorder.setOutputFile(outputFile.getAbsolutePath());

        // Start recording
        try {
            recorder.prepare();
            recorder.start();

            // Record for 5 seconds
            Thread.sleep(5000);

            // Stop recording
            recorder.stop();
            recorder.release();
        } catch (Exception e) {
            Log.e(TAG, "Error recording audio", e);
        }
    }

    private void encryptAudio() {
        try {
            // Generate a random AES key
            byte[] key = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(key);

            // Create a cipher object
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));

            // Encrypt the audio file
            FileInputStream fis = new FileInputStream(outputFile);
            FileOutputStream fos = new FileOutputStream(encryptedFile);
            CipherOutputStream cos = new CipherOutputStream(fos, cipher);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
            fis.close();
            fos.close();
            cos.close();
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting audio", e);
        }
    }

    private void sendAudio() {
        // Create a new audio track
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

        // Start playing the audio track
        audioTrack.play();

        // Read the encrypted audio file and write it to the audio track
        try {
            FileInputStream fis = new FileInputStream(encryptedFile);
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                audioTrack.write(buffer, 0, bytesRead);
            }
            fis.close();
        } catch (Exception e) {
            Log.e(TAG, "Error sending audio", e);
        }

        // Stop playing the audio track
        audioTrack.stop();
        audioTrack.release();
    }

    private void receiveAudio() {
        // Create a new audio track
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

        // Start playing the audio track
        audioTrack.play();

        // Read the encrypted audio file from the aux cable and write it to the audio track
        try {
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            audioRecord.startRecording();
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = audioRecord.read(buffer, 0, bufferSize)) != -1) {
                audioTrack.write(buffer, 0, bytesRead);
            }
            audioRecord.stop();
            audioRecord.release();
        } catch (Exception e) {
            Log.e(TAG, "Error receiving audio", e);
        }

        // Stop playing the audio track
        audioTrack.stop();
        audioTrack.release();
    }

    private void decryptAudio() {
        try {
            // Generate the same AES key as used for encryption
            byte[] key = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(key);

            // Create a cipher object
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));

            // Decrypt the audio file
            FileInputStream fis = new FileInputStream(receivedFile);
            FileOutputStream fos = new FileOutputStream(decryptedFile);
            CipherOutputStream cos = new CipherOutputStream(fos, cipher);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
            fis.close();
            fos.close();
            cos.close();
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting audio", e);
        }
    }

    private void playAudio() {
        // Create a new media player
        MediaPlayer mediaPlayer = new MediaPlayer();

        try {
            // Set the data source to the decrypted audio file
            mediaPlayer.setDataSource(decryptedFile.getAbsolutePath());

            // Prepare the media player
            mediaPlayer.prepare();

            // Start playing the audio
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Error playing audio", e);
        }
    }
}
