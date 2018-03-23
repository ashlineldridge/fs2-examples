package com

import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import fs2.Pipe

package object example extends LazyLogging {

  def logged[A](f: A => String): Pipe[IO, A, A] =
    _.evalMap(a => IO(logger.info(f(a))).map(_ => a))
}
