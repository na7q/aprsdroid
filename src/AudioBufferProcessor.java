package com.jazzido.PacketDroid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

public class AudioBufferProcessor extends Thread {
	static final String LOG_TAG = "APRSdroid.AfskABP";

	private AudioIn audioIn = new AudioIn();
	private PacketCallback callback;
	
	private boolean inited = false;
	
	float[] fbuf = new float[16384];
	private int fbuf_cnt = 0;
	private int overlap = 18; // overlap for AFSK DEMOD (FREQSAMP / BAUDRATE)
	private boolean writeAudioBuffer = false; // for debug
	
	int _dumpCount = 1024;
	
	
	native void init();
	native void processBuffer(float[] buf, int length);
	native void processBuffer2(byte[] buf);
	
	// for debugging the caputured samples
	// sox -e signed -r 22050 -b 16 sambombo.raw output2.wav
	FileOutputStream _fos;
	File _f = new File("/sdcard/PacketDroidSamples.raw");
	
	private final LinkedBlockingQueue<short[]> queue;
	
    static {
        System.loadLibrary("multimon");
    }
	
	public AudioBufferProcessor(PacketCallback cb) {
		super("AudioBuffeProcessor");
		queue = new LinkedBlockingQueue<short[]>();

		callback = cb;

		if (writeAudioBuffer) {
			try {
				_fos = new FileOutputStream(_f);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
//	public void read() {
//		if (!inited) { inited = true; init(); } // init native demodulators
//		if (!audioIn.isAlive()) audioIn.start();
//		
//		while (true) {
//			try {
//				decode(queue.take());
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}
	
	@Override
	public void run() {
		Log.d(LOG_TAG, "thread started");
		if (!inited) { inited = true; init(); } // init native demodulators
		if (!audioIn.isAlive()) audioIn.start();
		
		while (true) {
			try {
				decode(queue.take());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void startRecording() {
		audioIn.recorder.startRecording();
	}
	
	public void stopRecording() {
		try {
			audioIn.close();
		} catch (IllegalStateException e) {
			// "IllegalStateException: stop() called on an uninitialized AudioRecord."
			// ignore this ;)
		}
		queue.clear();
	}
	
	
	void decode(short[] s) {
		// Log.d(LOG_TAG, "CALLBACK!: " + s.length);
		short max = 0;
		for (int i = 0; i < s.length; i++) {
			if (writeAudioBuffer) {
				try {
					_fos.write(s[i] & 0xFF);
					_fos.write((s[i] >> 8) & 0xFF);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			fbuf[fbuf_cnt++] = s[i] * (1.0f/32768.0f);
			if (max < s[i])
			       max = s[i];
			else if (max < -s[i])
				max = (short)-s[i];
		}
		
		callback.peak(max);
		
		if (fbuf_cnt > overlap) {
			processBuffer(fbuf, fbuf_cnt - overlap);
			System.arraycopy(fbuf, fbuf_cnt-overlap, fbuf, 0, overlap);
			fbuf_cnt = overlap;
		}
	}
	
	// taken from: http://stackoverflow.com/questions/4525206/android-audiorecord-class-process-live-mic-audio-quickly-set-up-callback-func
	public class AudioIn extends Thread {
		private AudioRecord recorder;
		
		private short[][] buffers = new short[16][8192];

		public AudioIn() {
			super("AudioIn");
			android.os.Process
					.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		}

		@Override
		public void run() {
			
			int ix = 0;
			recorder = new AudioRecord(AudioSource.MIC, 22050,
					AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT, 16384);

			try {
				recorder.startRecording();

				while (recorder.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED) {
					int nRead = 0;
					short[] buffer = buffers[ix++ % buffers.length];

					nRead = recorder.read(buffer, 0, buffer.length);

					queue.put(buffer);
					// process(buffer);
				}
			} catch (Throwable x) {
				Log.w(LOG_TAG, "Error reading audio", x);
			} 
		}

	
		private void close() {
			if (recorder != null) recorder.stop();
			Log.d(LOG_TAG, "AudioIn: close");
		}

	}

	public void callback(byte[] data) {
		Log.d(LOG_TAG, "called callback: " + new String(data));
		callback.received(data);
	}
}
