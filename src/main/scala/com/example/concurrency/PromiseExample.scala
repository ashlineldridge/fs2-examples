package com.example.concurrency

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import fs2.StreamApp.ExitCode
import fs2._
import fs2.async.Promise

import scala.concurrent.ExecutionContext.Implicits.global

class ConcurrentCompletion(p: Promise[IO, Int]) extends LazyLogging {

  private def attemptPromiseCompletion(n: Int): Stream[IO, Unit] =
    Stream.eval(p.complete(n)).attempt.drain

  def start: Stream[IO, ExitCode] =
    (Stream(
      attemptPromiseCompletion(1),
      attemptPromiseCompletion(2),
      Stream.eval(p.get).evalMap(n => IO(logger.info(s"Result: $n")))
    ).join(3).drain ++ Stream.emit(ExitCode.Success)).covaryOutput[ExitCode]

}

object Once extends StreamApp[IO] {

  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] =
    for {
      p <- Stream.eval(async.promise[IO, Int])
      e <- new ConcurrentCompletion(p).start
    } yield e
}