package com.example
package concurrency

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import fs2.StreamApp.ExitCode
import fs2.async.Ref
import fs2._

import scala.concurrent.ExecutionContext.Implicits.global

class Worker(number: Int, ref: Ref[IO, Int]) extends LazyLogging {

  private val sink: Sink[IO, Int] =
    _.evalMap(n => IO(logger.info(s"#$number >> $n")))

  def start: Stream[IO, Unit] =
    for {
      _ <- Stream.eval(ref.get).to(sink)
      _ <- Stream.eval(ref.modify(_ + 1))
      _ <- Stream.eval(ref.get).to(sink)
    } yield ()
}

object Counter extends StreamApp[IO] with LazyLogging {

  logger.info("Starting counter application")

  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] =
    for {
      ref <- Stream.eval(async.refOf[IO, Int](0))
      w1 = new Worker(1, ref)
      w2 = new Worker(2, ref)
      w3 = new Worker(3, ref)
      ws = Stream(w1.start, w2.start, w3.start)
      ec <- (ws.join(3).drain ++ Stream.emit(ExitCode.Success)).covaryOutput[ExitCode]
    } yield ec
}

