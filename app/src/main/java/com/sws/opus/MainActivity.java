package com.sws.opus;

import android.Manifest.permission;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    /**
     * 原始音频文件路径
     */
    private static final String AUDIO_FILE_PATH = Environment.getExternalStorageDirectory().getPath() +
            "/recorded_audio.pcm";

    /**
     * 处理过的音频文件路径
     */
    private static final String AUDIO_PROCESS_FILE_PATH = Environment.getExternalStorageDirectory().getPath() +
            "/opus_process.pcm";

    private long mEncoder;
    private long mDecoder;

    private AudioTrack mAudioTrack;
    private int mMinBufferSize;
    private File mFile;
    private File mProcessFile;
    private boolean isInitialized;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initAudioRecord();
        if (VERSION.SDK_INT >= VERSION_CODES.M && PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(getApplicationContext(), permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermissions(new String[]{permission.WRITE_EXTERNAL_STORAGE}, 1000);
        } else {
            initAudio();
        }
        setup();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 1000) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initAudio();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setup() {
        Button playingBtn = findViewById(R.id.playing);
        Button playingAgcNsBtn = findViewById(R.id.playing_process);
        Button agcNsProcessBtn = findViewById(R.id.opus_btn);

        playingBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isInitialized || !mFile.exists()) {
                    Toast.makeText(MainActivity.this, "文件读写失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                playing(false);
            }
        });

        playingAgcNsBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isInitialized && !mProcessFile.exists() || mProcessFile.length() <= 0) {
                    Toast.makeText(MainActivity.this, "文件读写失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                playing(true);
            }
        });

        agcNsProcessBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isInitialized || !mFile.exists()) {
                    Toast.makeText(MainActivity.this, "文件读写失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                process();
            }
        });
    }

    private void initAudio() {
        mProcessFile = new File(AUDIO_PROCESS_FILE_PATH);

        mFile = new File(AUDIO_FILE_PATH);

        if (!mFile.exists() || mFile.length() <= 0) {
            Log.e("sws", " init file-----------");
            AssetManager assets = getAssets();
            try {
                InputStream inputStream = assets.open("record/recorded_audio.pcm");
                FileOutputStream fileOutputStream = new FileOutputStream(mFile);
                byte[] buf = new byte[1024 * 1024];
                int len;
                while ((len = inputStream.read(buf)) != -1) {
                    fileOutputStream.write(buf, 0, len);
                }
                inputStream.close();
                fileOutputStream.close();
                isInitialized = true;
                Log.e("sws", " init file end-----------");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("sws", "-----------");
            isInitialized = true;
        }
    }

    private void initAudioRecord() {
        if (mAudioTrack == null) {
            mMinBufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, mMinBufferSize, AudioTrack.MODE_STREAM);
        }
    }

    private void process() {
        if (mEncoder == 0) {
            //压缩质量 complexity
            //1 单声道
            mEncoder = OpusUtil.createEncoder(8000, 1, 8);
        }

        Log.e("sws", "encode ===" + mEncoder);
        Log.e("sws", "decoder ===" + mDecoder);
        try {
            FileInputStream ins = new FileInputStream(mFile);
            File outFile = new File(AUDIO_PROCESS_FILE_PATH);
            FileOutputStream out = new FileOutputStream(outFile);
//            码率 V = 8K * 16 * 1 = 128Kbps = 16KBps
//            音频帧时间为20ms，每帧音频数据大小为 size = 16KBps * 0.02s = 320KB，即160 Short，
            byte[] buf = new byte[320];

            while (ins.read(buf) != -1) {
                short[] shortData = new short[buf.length >> 1];

                byte[] processData = new byte[20];

                ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortData);
                Log.e("sws", "压缩 shortData======" + shortData.length);

                int encode = OpusUtil.encode(mEncoder, shortData, 0, processData);
                Log.e("sws", "压缩后的大小======" + encode);
                if (encode > 0) {
                    out.write(processData, 0, encode);
                }
            }
            Toast.makeText(getApplicationContext(), "完成", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.e("sws", "opus end======");
    }

    private boolean isPlaying;

    private void playing(final boolean isPlayingProcess) {
        if (isPlaying) {
            isPlaying = false;
            mAudioTrack.stop();
        }
        if (mDecoder == 0) {
            mDecoder = OpusUtil.createDecoder(8000, 1);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream ins = null;
                try {
                    File file = mFile;
                    if (isPlayingProcess) {
                        file = mProcessFile;
                    }

                    isPlaying = true;

                    ins = new FileInputStream(file);
                    mAudioTrack.play();
                    byte[] buf;
                    short[] decodeBuf = null;
                    if(isPlayingProcess) {
                        buf = new byte[20];
                        decodeBuf = new short[160];
                    } else {
                        buf = new byte[mMinBufferSize];
                    }
                    int len;

                    while ((len = ins.read(buf)) != -1 && mAudioTrack != null && isPlaying) {
                        if (isPlayingProcess) {
                            int decode = OpusUtil.decode(mDecoder, buf, decodeBuf);
                            Log.e("sws", "解压后的大小==" + decode);
                            mAudioTrack.write(decodeBuf, 0, decode);
                        } else {
                            mAudioTrack.write(buf, 0, len);
                        }
                    }
                    if (mAudioTrack == null) {
                        return;
                    }
                    mAudioTrack.stop();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (ins != null) {
                        try {
                            ins.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private byte[] shortsToBytes(short[] data) {
        byte[] buffer = new byte[data.length * 2];
        int shortIndex, byteIndex;
        shortIndex = byteIndex = 0;
        for (; shortIndex != data.length; ) {
            buffer[byteIndex] = (byte) (data[shortIndex] & 0x00FF);
            buffer[byteIndex + 1] = (byte) ((data[shortIndex] & 0xFF00) >> 8);
            ++shortIndex;
            byteIndex += 2;
        }
        return buffer;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isPlaying = false;
        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }
        if (mEncoder > 0) {
            OpusUtil.destroyEncoder(mEncoder);
        }
        if (mDecoder > 0) {
            OpusUtil.destroyDecoder(mDecoder);
        }
    }
}
