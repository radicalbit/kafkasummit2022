package io.radicalbit.ks22

import io.radicalbit.ks22.config.{Config, RunModes}
import org.slf4j.{Logger, LoggerFactory}

import java.time.Clock

object Main extends App {

  implicit val log: Logger    = LoggerFactory.getLogger(StreamJob.getClass)
  implicit val config: Config = Config.load()
  implicit val clock: Clock   = Clock.systemUTC()

  val mode = config.mode
  mode match {
    case RunModes.Kafka => KafkaGenerator.generate()
    case RunModes.Rest  => RestGenerator.generate()
    case RunModes.Job   => StreamJob.start()
    case _ =>
      log.error(
        s"Unknown run mode method. Can't proceed. exiting",
        new IllegalArgumentException(s"Unknown run mode method $mode. Can't proceed. exiting")
      )
      System.exit(-1)
  }
}
