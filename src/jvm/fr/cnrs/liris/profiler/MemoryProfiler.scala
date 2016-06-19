/*
 * Copyright LIRIS-CNRS (2016)
 * Contributors: Vincent Primault <vincent.primault@liris.cnrs.fr>
 *
 * This software is a computer program whose purpose is to study location privacy.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.cnrs.liris.profiler

import java.io.{OutputStream, PrintStream}
import java.lang.management.ManagementFactory

/**
 * Memory profiler.
 *
 * At each call to [[MemoryProfiler.markPhase]] performs garbage collection and stores
 * heap and non-heap memory usage in an external file.
 *
 * <em>Heap memory</em> is the runtime data area from which memory for all
 * class instances and arrays is allocated. <em>Non-heap memory</em> includes
 * the method area and memory required for the internal processing or
 * optimization of the JVM. It stores per-class structures such as a runtime
 * constant pool, field and method data, and the code for methods and
 * constructors. The Java Native Interface (JNI) code or the native library of
 * an application and the JVM implementation allocate memory from the
 * <em>native heap</em>.
 *
 * The script in /devtools/blaze/scripts/blaze-memchart.sh can be used for post processing.
 */
object MemoryProfiler {
  private[this] var memoryProfile: Option[PrintStream] = None
  private[this] var currentPhase: ProfilePhase = ProfilePhase.Init

  def start(out: OutputStream): Unit = synchronized {
    this.memoryProfile = Some(new PrintStream(out))
  }

  def stop(): Unit = synchronized {
    memoryProfile.foreach(_.close())
    memoryProfile = None
  }

  def markPhase(nextPhase: ProfilePhase): Unit = synchronized {
    memoryProfile.foreach { memoryProfile =>
      val name = currentPhase.description
      ManagementFactory.getMemoryMXBean.gc()
      var memoryUsage = ManagementFactory.getMemoryMXBean.getHeapMemoryUsage
      memoryProfile.println(name + ":heap:init:" + memoryUsage.getInit)
      memoryProfile.println(name + ":heap:used:" + memoryUsage.getUsed)
      memoryProfile.println(name + ":heap:commited:" + memoryUsage.getCommitted)
      memoryProfile.println(name + ":heap:max:" + memoryUsage.getMax)

      memoryUsage = ManagementFactory.getMemoryMXBean.getNonHeapMemoryUsage
      memoryProfile.println(name + ":non-heap:init:" + memoryUsage.getInit)
      memoryProfile.println(name + ":non-heap:used:" + memoryUsage.getUsed)
      memoryProfile.println(name + ":non-heap:commited:" + memoryUsage.getCommitted)
      memoryProfile.println(name + ":non-heap:max:" + memoryUsage.getMax)
      currentPhase = nextPhase
    }
  }
}