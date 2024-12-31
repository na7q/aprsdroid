package org.na7q.app

import _root_.android.util.Log

object Benchmark {
	def apply[T](tag: String)(block: => T) : T = {
		val start = System.currentTimeMillis
		try {
			block
		} finally {
			val exectime = System.currentTimeMillis - start
			Log.d(tag, "exectuion time: %.3f s".formatLocal(null, exectime / 1000.0))
		}
	}
}


