// Copied from LinkedIn's Ambry.
/**
 * Copyright 2016 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package fr.cnrs.liris.common.util

import java.io.{File, IOException, RandomAccessFile}
import java.nio.channels.{FileLock => NioFileLock}
import java.nio.channels.OverlappingFileLockException

/**
 * File lock helper.
 */
class FileLock(file: File) {
  file.createNewFile()
  private[this] val channel = new RandomAccessFile(file, "rw").getChannel
  private[this] var flock: Option[NioFileLock] = None

  /**
   * Lock the file or throw an exception if the lock is already held.
   */
  @throws[IOException]
  def lock(): Unit = synchronized {
    //trace("Acquiring lock on " + file.getAbsolutePath)
    flock = Some(channel.lock())
  }

  /**
   * Try to lock the file and return true if the locking succeeds.
   */
  @throws[IOException]
  def tryLock(): Boolean = synchronized {
    // trace("Acquiring lock on " + file.getAbsolutePath)
    try {
      // Weirdly this method will return null if the lock is held by another
      // process, but will throw an exception if the lock is held by this process
      // so we have to handle both cases.
      flock = Option(channel.tryLock())
      flock.isDefined;
    } catch {
      case _: OverlappingFileLockException => false
    }
  }

  /**
   * Unlock the lock if it is held.
   */
  @throws[IOException]
  def unlock(): Unit = synchronized {
    //trace("Releasing lock on " + file.getAbsolutePath)
    flock.foreach(_.release())
    flock = None
  }

  /**
   * Destroy this lock, closing the associated [[java.nio.channels.FileChannel]].
   */
  @throws[IOException]
  def destroy(): Unit = synchronized {
    unlock()
    channel.close()
  }
}
