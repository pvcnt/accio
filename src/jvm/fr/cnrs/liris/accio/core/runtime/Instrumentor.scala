package fr.cnrs.liris.accio.core.runtime

import java.io.{ByteArrayOutputStream, PrintStream}
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicLong

import com.twitter.util.{Duration, StorageUnit}

import scala.collection.JavaConverters._

class Instrumentor {
  private[this] val _wallTime = new AtomicLong(0)
  private[this] val _cpuTime = new AtomicLong(0)
  private[this] val _userTime = new AtomicLong(0)
  private[this] val _systemTime = new AtomicLong(0)
  private[this] val _memoryUsed = new AtomicLong(0)
  private[this] val _memoryReserved = new AtomicLong(0)

  def apply[T](f: => T): T = {
    val bean = ManagementFactory.getThreadMXBean
    val startWallTime = System.nanoTime
    val startCpuTime = if (bean.isCurrentThreadCpuTimeSupported) Some(bean.getCurrentThreadCpuTime) else None
    val startUserTime = if (bean.isCurrentThreadCpuTimeSupported) Some(bean.getCurrentThreadUserTime) else None
    val startSystemTime = if (bean.isCurrentThreadCpuTimeSupported) Some(startCpuTime.get - startUserTime.get) else None

    try {
      f
    } finally {
      _wallTime.addAndGet(math.max(0, System.nanoTime - startWallTime))
      val cpuTime = startCpuTime.map(start => bean.getCurrentThreadCpuTime - start)
      cpuTime.foreach(time => this._cpuTime.addAndGet(math.max(0, time)))
      val userTime = startUserTime.map(start => bean.getCurrentThreadUserTime - start)
      userTime.foreach(time => this._userTime.addAndGet(math.max(0, time)))
      val systemTime = startSystemTime.map(start => cpuTime.get - userTime.get - start)
      systemTime.foreach(time => this._systemTime.addAndGet(math.max(0, time)))

      val peaks = ManagementFactory.getMemoryPoolMXBeans.asScala.map(_.getPeakUsage)
      _memoryUsed.addAndGet(peaks.map(_.getUsed).sum)
      _memoryReserved.addAndGet(peaks.map(_.getCommitted).sum)
    }
  }

  def wallTime: Duration = Duration.fromNanoseconds(_wallTime.get)

  def cpuTime: Duration = Duration.fromNanoseconds(_cpuTime.get)

  def userTime: Duration = Duration.fromNanoseconds(_userTime.get)

  def memoryUsed: StorageUnit = StorageUnit.fromBytes(_memoryUsed.get)

  def memoryReserved: StorageUnit = StorageUnit.fromBytes(_memoryReserved.get)
}

object Instrumentor {
  private[this] val outBytes = new ByteArrayOutputStream
  private[this] val errBytes = new ByteArrayOutputStream
  private[this] var savedOut: Option[PrintStream] = None
  private[this] var savedErr: Option[PrintStream] = None
  val out = new PrintStream(outBytes)
  val err = new PrintStream(errBytes)

  def stdoutAsBytes: Array[Byte] = synchronized {
    out.flush()
    outBytes.toByteArray
  }

  def stderrAsBytes: Array[Byte] = synchronized {
    err.flush()
    errBytes.toByteArray
  }

  def stdoutAsString: String = new String(stdoutAsBytes)

  def stderrAsString: String = new String(stderrAsBytes)

  def recordOutErr(): Unit = synchronized {
    if (savedOut.isEmpty) {
      savedOut = Some(System.out)
      savedErr = Some(System.err)
      System.setErr(err)
      System.setOut(out)
    }
  }

  def restoreOutErr(): Unit = synchronized {
    if (savedOut.isDefined) {
      System.setErr(savedOut.get)
      System.setOut(savedErr.get)
      savedOut = None
      savedErr = None
    }
  }
}