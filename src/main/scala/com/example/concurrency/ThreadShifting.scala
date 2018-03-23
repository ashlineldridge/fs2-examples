package com.example.concurrency

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory}

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import fs2._

import scala.concurrent.ExecutionContext

// Idea taken from here: https://typelevel.org/blog/2017/05/02/io-monad-for-cats.html
object ThreadShifting extends App with LazyLogging {

  implicit val DefaultThreadPool = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2, new NamedThreadFactory("default-thread-pool-")))
  val BlockingIoThreadPool = ExecutionContext.fromExecutor(Executors.newCachedThreadPool(new NamedThreadFactory("blocking-io-thread-pool-")))

  class Worker(id: Int) {
    def run: Stream[IO, Unit] =
      Stream.eval(
        for {
          _ <- IO(logger.info(s"#${id}: Starting..."))
          _ <- IO.shift(BlockingIoThreadPool)
          _ <- IO(logger.info(s"#${id}: Some long running IO operation..."))
          _ <- IO(Thread.sleep(2000))
          _ <- IO.shift(DefaultThreadPool)
          _ <- IO(logger.info(s"#${id}: Some CPU bound operation"))
          _ <- IO(Thread.sleep(500))
        } yield ())
  }

  Stream.range(1, 10)
    .map(new Worker(_))
    .map(_.run)
    .join(3)
    .compile
    .drain
    .flatMap(_ => IO(logger.info("Done")))
    .unsafeRunSync
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
