package io.radicalbit.ks22.config

import pureconfig.generic.auto._
import pureconfig.generic.semiauto.deriveEnumerationReader
import pureconfig.{ConfigReader, ConfigSource}

object RunModes {
  sealed trait RunMode

  case object Kafka extends RunMode
  case object Rest  extends RunMode
  case object Job   extends RunMode
}

final case class Config(
    restConfig: RestConfig,
    kafkaConfig: KafkaConfig,
    streamConfig: StreamConfig,
    mode: RunModes.RunMode
)

object Config {

  implicit val modeConverter: ConfigReader[RunModes.RunMode] = deriveEnumerationReader[RunModes.RunMode]

  def load(): Config = configResolver(ConfigSource.default.load[Config])

  private def configResolver: ConfigReader.Result[Config] => Config = {
    case Right(config) => config
    case Left(error)   => throw new RuntimeException(s"Cannot read config file, errors:\n ${error.toList.mkString("\n")}")
  }
}

final case class KafkaConfig(bootstrapServers: String, topic: String)
final case class StreamConfig(applicationId: String, inputTopic: String, outputTopic: String)

final case class RestConfig(hostname: String, namespace: String, modelName: String, isIstio: Boolean) {

  def buildURI(driver: String): String = if (isIstio)
    s"$hostname/$driver/$namespace/$modelName/api/v1.0/predictions"
  else
    s"$hostname/api/v1.0/predictions"
}
