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

import dagger.Component;
import dagger.Module;
import dagger.Provides;

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

        // Dagger 2 component injection
        ((AudioComponent) DaggerAudioComponent.create()).inject(this);

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
            try {
                frequency = Integer.parseInt(frequencyEditText.getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid frequency", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Validate the frequency
        if (frequency < 44100 || frequency > 48000) {
            Toast.makeText(this, "Frequency must be between 44100 and 48000 Hz", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new audio recorder
        audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        audioRecorder.setAudioSamplingRate(frequency);
        audioRecorder.setOutputFile(outputFile.getAbsolutePath());

        // Start recording
        try {
            audioRecorder.prepare();
            audioRecorder.start();

            // Record for 5 seconds
            Thread.sleep(5000);

            // Stop recording
            audioRecorder.stop();
            audioRecorder.release();
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
        audioTrack.setStreamType(AudioManager.STREAM_MUSIC);
        audioTrack.setAudioFormat(new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(44100)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build());
        audioTrack.play();

        // Read the encrypted audio file and write it to the audio track
        try {
            FileInputStream fis = new FileInputStream(encryptedFile);
            byte[] buffer = new byte[1024];
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
        audioTrack.setStreamType(AudioManager.STREAM_MUSIC);
        audioTrack.setAudioFormat(new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(44100)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build());
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

    @Module
    public static class AudioModule {

        @Provides
        public AudioRecorder provideAudioRecorder() {
            return new AudioRecorder();
        }

        @Provides
        public AudioTrack provideAudioTrack() {
            return new AudioTrack();
        }

        @Provides
        public Cipher provideCipher() {
            try {
                return Cipher.getInstance("AES/CBC/PKCS5Padding");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Component(modules = {AudioModule.class})
    public interface AudioComponent {

        void inject(MainActivity activity);
    }
}
