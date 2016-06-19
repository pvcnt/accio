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

package fr.cnrs.liris.profiler.chart

import fr.cnrs.liris.profiler.ProfileInfo


/**
 * Implementation of [[ChartCreator]] that creates Gantt Charts that contain bars for all tasks in the profile.
 *
 * @param info The data of the profiled build
 */
class DetailedChartCreator(info: ProfileInfo) extends CommonChartCreator {
  override def create: Chart = {
    val chart = new Chart
    createCommonChartItems(chart, info)

    // calculate the critical path
    //EnumSet<ProfilerTask> typeFilter = EnumSet.noneOf(ProfilerTask.class);
    //CriticalPathEntry criticalPath = info.getCriticalPath(typeFilter);
    //info.analyzeCriticalPath(typeFilter, criticalPath);

    info.tasks.foreach { task =>
      val label = task.typ.description + ": " + task.description
      val typ = chart.lookUpType(task.typ.description)
      val stop = task.startTime + task.duration
      //CriticalPathEntry entry = null;
      // for top level tasks, check if they are on the critical path
      /*if (task.parentId == 0 && criticalPath != null) {
      entry = info.getNextCriticalPathEntryForTask(criticalPath, task);
      // find next top-level entry
      if (entry != null) {
      CriticalPathEntry nextEntry = entry.next;
      while (nextEntry != null && nextEntry.task.parentId != 0) {
      nextEntry = nextEntry.next;
    }
      if (nextEntry != null) {
      // time is start and not stop as we traverse the critical back backwards
      chart.addVerticalLine(task.threadId, nextEntry.task.threadId, task.startTime);
    }
    }
    }*/
      chart.addBar(task.threadId, task.startTime.inNanoseconds, stop.inNanoseconds, typ, label, highlight = false /*(entry != null)*/)
    }
    chart
  }
}