package com.example.concurrency

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import fs2._

import scala.concurrent.ExecutionContext

// Idea taken from here: https://typelevel.org/blog/2017/05/02/io-monad-for-cats.html
object ThreadShifting extends App with LazyLogging {

  val defaultThreadPool = Executors.newFixedThreadPool(2, new NamedThreadFactory("default-thread-pool-"))
  val blockingIoThreadPool = Executors.newCachedThreadPool(new NamedThreadFactory("blocking-io-thread-pool-"))
  implicit val defaultExecutionContext = ExecutionContext.fromExecutor(defaultThreadPool)
  val blockingIoExecutionContext = ExecutionContext.fromExecutor(blockingIoThreadPool)

  class Worker(id: Int) {
    def run: Stream[IO, Unit] =
      Stream.eval(
        for {
          _ <- IO(logger.info(s"#$id: Starting..."))
          _ <- IO.shift(blockingIoExecutionContext)
          _ <- IO(logger.info(s"#$id: Some long running IO operation..."))
          _ <- IO(Thread.sleep(2000))
          _ <- IO.shift(defaultExecutionContext)
          _ <- IO(logger.info(s"#$id: Some CPU bound operation"))
          _ <- IO(Thread.sleep(500))
        } yield ())
  }

  Stream.range(1, 10)
    .map(new Worker(_))
    .map(_.run)
    .join(3)
    .onFinalize(shutdown)
    .compile
    .drain
    .flatMap(_ => IO(logger.info("Done")))
    .unsafeRunSync()

  private def shutdown: IO[Unit] =
    IO {
      defaultThreadPool.shutdown()
      blockingIoThreadPool.shutdown()
    }
}

// Adapted from Executors.DefaultThreadFactory so that we can control
// thread naming to more easily distinguish the thread pool from logging
class NamedThreadFactory(namePrefix: String) extends ThreadFactory {

  private val securityManager = System.getSecurityManager
  private val threadNumber = new AtomicInteger(1)
  private val threadGroup: ThreadGroup =
    if (securityManager != null) securityManager.getThreadGroup
    else Thread.currentThread.getThreadGroup

  override def newThread(r: Runnable): Thread = {
    val t = new Thread(threadGroup, r, namePrefix + threadNumber.getAndIncrement, 0)
    if (t.isDaemon) t.setDaemon(false)
    if (t.getPriority != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY)
    t
  }
}
