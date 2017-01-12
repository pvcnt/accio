/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016 Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Accio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Accio.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnrs.liris.accio.core.application

import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicLong

import com.twitter.util.{Duration, StorageUnit}
import fr.cnrs.liris.accio.core.domain.Metric

import scala.collection.JavaConverters._

trait Profiler {
  def profile[T](f: => T): T

  def metrics: Set[Metric]
}

object NullProfiler extends Profiler {
  override def profile[T](f: => T): T = f

  override def metrics: Set[Metric] = Set.empty
}

class JvmProfiler extends Profiler {
  private[this] val _wallTime = new AtomicLong(0)
  private[this] val _cpuTime = new AtomicLong(0)
  private[this] val _userTime = new AtomicLong(0)
  private[this] val _systemTime = new AtomicLong(0)
  private[this] val _memoryUsed = new AtomicLong(0)
  private[this] val _memoryReserved = new AtomicLong(0)

  override def profile[T](f: => T): T = {
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

  override def metrics: Set[Metric] = {
    Set(
      Metric("memory_used_bytes", memoryUsed.inBytes),
      Metric("memory_reserved_bytes", memoryReserved.inBytes),
      Metric("cpu_time_nanos", cpuTime.inNanoseconds),
      Metric("user_time_nanos", userTime.inNanoseconds),
      Metric("system_time_nanos", userTime.inNanoseconds))
  }

  def wallTime: Duration = Duration.fromNanoseconds(_wallTime.get)

  def cpuTime: Duration = Duration.fromNanoseconds(_cpuTime.get)

  def userTime: Duration = Duration.fromNanoseconds(_userTime.get)

  def memoryUsed: StorageUnit = StorageUnit.fromBytes(_memoryUsed.get)

  def memoryReserved: StorageUnit = StorageUnit.fromBytes(_memoryReserved.get)
}
