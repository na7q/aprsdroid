package org.na7q.app

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

import sivantoledo.ax25.Afsk1200Demodulator

class AfskDemodulator(au : AfskUploader, in_type : Int, samplerate : Int) extends Thread("AFSK demodulator") {
	val TAG = "APRSdroid.AfskDemod"

	val BUF_SIZE = 8192
	val buffer_s = new Array[Short](BUF_SIZE)
	val buffer_f = new Array[Float](BUF_SIZE)

	val demod = new Afsk1200Demodulator(samplerate, 1, 6, au)
	var recorder : AudioRecord = null

	// we process incoming audio
	android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)


	override def run() {
		Log.d(TAG, "running...")
		try {
			var zero_reads = 0
			recorder = new AudioRecord(in_type, samplerate,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
				4*BUF_SIZE)
			recorder.startRecording();
			while (!isInterrupted() && (recorder.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED)) {
				val count = recorder.read(buffer_s, 0, BUF_SIZE)
				Log.d(TAG, "read " + count + " samples")
				if (count == 0) {
					zero_reads+=1
					if (zero_reads == 10)
						throw new RuntimeException("recorder.read() not delivering data!")
				} else if (count < 0)
					throw new RuntimeException("recorder.read() = " + count)
				else
					zero_reads=0

				for (i <- 0 to count-1)
					buffer_f(i) = buffer_s(i).asInstanceOf[Float] / 32768.0f

				demod.addSamples(buffer_f, count)
				au.notifyMicLevel(demod.peak())
			}
		} catch {
		case e : Exception =>
			Log.e(TAG, "run(): " + e)
			e.printStackTrace()
			au.postAbort(e.toString())
		}
		Log.d(TAG, "closed.")
	}

	def close() {
		try {
			this.interrupt()
			recorder.stop()
			this.join(50)
			recorder.release()
		} catch {
		case e : IllegalStateException => Log.w(TAG, "close(): " + e)
		case e : NullPointerException => // no recorder yet, ignore.
		}
	}
}
