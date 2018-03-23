package com.example.concurrency

import java.util.concurrent.Executors

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext

// Not FS2 specific but an example of shifting threads
object ThreadShifting extends App with LazyLogging {

  val DefaultThreadPool = ExecutionContext.fromExecutor(null)
  val BlockingIoThreadPool = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  (for {
    _ <- IO(logger.info("Main thread"))
    _ <- IO.shift(BlockingIoThreadPool)
    _ <- IO(logger.info("Some long running IO operation..."))
    _ <- IO(Thread.sleep(2000))
    _ <- IO.shift(DefaultThreadPool)
    _ <- IO(logger.info("Some CPU bound operation"))
  } yield ()).unsafeRunSync
}
