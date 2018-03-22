package com.example.fs2

import java.time.ZonedDateTime

import com.typesafe.config.ConfigFactory
import pureconfig.ConfigReader._
import pureconfig.ConvertHelpers.catchReadError
import pureconfig._

case class Config(
    serviceName: String,
    serviceVersion: String,
    awsRegion: String)

object Config {

  implicit val camelCaseProductHint = ProductHint[Config](ConfigFieldMapping(CamelCase, CamelCase))
  implicit val zonedDateTimeConfigReader = fromString[ZonedDateTime](catchReadError(ZonedDateTime.parse))
  implicit val optionStringConfigReader = fromString[Option[String]](s => _ => Right(Option(s).filterNot(_.isEmpty)))

  def apply(): Config =
    loadConfigOrThrow[Config](ConfigFactory.load)
}

