/*
 * Accio is a program whose purpose is to study location privacy.
 * Copyright (C) 2016-2017 Vincent Primault <vincent.primault@liris.cnrs.fr>
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

package fr.cnrs.liris.accio.core.infra.scheduler.gridengine

import java.io.{ByteArrayInputStream, IOException, InputStream}
import java.nio.file.Path

import com.typesafe.scalalogging.StrictLogging
import fr.cnrs.liris.accio.core.service.{Downloader, Job, Scheduler}
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.IOUtils
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.userauth.method.{AuthMethod, AuthPublickey}
import net.schmizz.sshj.xfer.{FileSystemFile, InMemorySourceFile}

import scala.collection.mutable

class GridEngineScheduler(
  workDir: Path,
  downloader: Downloader,
  prefix: String,
  sshOpts: SSHOptions,
  executorUri: String,
  javaHome: Option[String],
  executorArgs: Seq[String])
  extends Scheduler with StrictLogging {

  private[this] lazy val localExecutorPath = {
    val targetPath = workDir.resolve("executor.jar")
    if (targetPath.toFile.exists()) {
      targetPath.toFile.delete()
    }
    logger.info(s"Downloading executor JAR to ${targetPath.toAbsolutePath}")
    downloader.download(executorUri, targetPath)
    targetPath.toAbsolutePath
  }
  private[this] lazy val ssh = {
    val ssh = new SSHClient
    ssh.connect(sshOpts.host)
    ssh.auth(sshOpts.user, createAuthPublicKey(ssh, sshOpts.publicKey): _*)
    ssh.useCompression()
    ssh
  }
  private[this] val executorUploadLock = new Object
  private[this] var executorUploaded = false

  override def submit(job: Job): String = {
    val session = ssh.startSession()
    try {
      val remoteWorkDir = remoteHomePath(session)
      maybeUploadExecutor(remoteWorkDir)
      uploadScript(job, remoteWorkDir)
      runQsub(session, job)
    } finally {
      session.close()
    }
  }

  /*private def createAuthPassword(password: String): AuthMethod = {
    new AuthPassword(new PasswordFinder {
      def reqPassword(resource: Resource[_]): Array[Char] = password.toCharArray.clone

      def shouldRetry(resource: Resource[_]): Boolean = false
    })
  }*/

  private def createAuthPublicKey(ssh: SSHClient, publicKey: Option[String]): Seq[AuthMethod] = {
    val base = sys.props("user.home") + "/.ssh/"
    val paths = Seq(base + "id_rsa", base + "id_dsa", base + "id_ed25519", base + "id_ecdsa") ++ publicKey.toSeq
    paths.flatMap { path =>
      try {
        logger.debug(s"Attempting to load key from: $path")
        Some(ssh.loadKeys(path))
      } catch {
        case e: IOException =>
          logger.info(s"Could not load keys from $path due to: ${e.getMessage}")
          None
      }
    }.map(keyProvider => new AuthPublickey(keyProvider))
  }

  override def kill(key: String): Unit = {

  }

  override def stop(): Unit = {
    ssh.disconnect()
  }

  private def remoteHomePath(session: Session): String = {
    val cmd = session.exec("pwd")
    val out = IOUtils.readFully(cmd.getInputStream).toString()
    cmd.join()
    if (cmd.getExitStatus != 0) {
      throw new RuntimeException(s"Error running pwd: $out")
    }
    out
  }

  private def maybeUploadExecutor(remoteWorkDir: String) = {
    executorUploadLock synchronized {
      if (!executorUploaded) {
        ssh.newSCPFileTransfer().upload(new FileSystemFile(localExecutorPath.toString), s"$remoteWorkDir/executor.jar")
        executorUploaded = true
      }
    }
  }

  private def isExecutorPresent(session: Session, remoteWorkDir: String) = {
    val cmd = session.exec(s"stat $remoteWorkDir/executor.jar")
    IOUtils.readFully(cmd.getInputStream).toString()
    cmd.join()
    cmd.getExitStatus == 0
  }

  private def uploadScript(job: Job, remoteWorkDir: String) = {
    val executorCmd = createCommandLine(job, localExecutorPath.toString, executorArgs, javaHome)
    val wrapperScript = "#!/bin/bash\n" + executorCmd.mkString(" ")
    ssh.newSCPFileTransfer().upload(new WrapperScript(s"$prefix${job.taskId.value}.sh", wrapperScript.getBytes), remoteWorkDir)
  }

  private[this] val QsubOutputRegex = "^Your job ([0-9]+) ".r

  private def runQsub(session: Session, job: Job) = {
    val cmd = session.exec(buildCommandLine(job).mkString(" "))
    val out = IOUtils.readFully(cmd.getInputStream).toString()
    cmd.join()
    if (cmd.getExitStatus != 0) {
      throw new RuntimeException(s"Error running qsub: $out")
    }
    out match {
      case QsubOutputRegex(key) => key
      case _ => throw new RuntimeException(s"Unexpected qsub output: $out")
    }
  }

  private def buildCommandLine(job: Job) = {
    val cores = job.resource.cpu.ceil.toInt
    val cmd = mutable.ListBuffer.empty[String]
    cmd += "qsub"
    cmd ++= Seq("-l", s"fsize=${job.resource.diskMb}M")
    cmd ++= Seq("-l", s"h_rss=${job.resource.ramMb}M")
    cmd ++= Seq("-N", s"accio_${job.taskId.value}")
    if (cores > 1) {
      cmd ++= Seq("-pe", "multicores", cores.toString)
      cmd ++= Seq("-q", "mc_long")
    } else {
      cmd ++= Seq("-q", "long")
    }
    cmd += s"$prefix${job.taskId.value}.sh"
    logger.debug(s"Command-line: ${cmd.mkString(" ")}")
    cmd
  }
}

private class WrapperScript(filename: String, bytes: Array[Byte]) extends InMemorySourceFile {
  override def getLength: Long = bytes.length

  override def getName: String = filename

  override def getInputStream: InputStream = new ByteArrayInputStream(bytes)
}