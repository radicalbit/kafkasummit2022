package io.radicalbit.ks22

import io.radicalbit.ks22.config.Config
import io.radicalbit.ks22.models.AvgLatency
import io.radicalbit.ks22.utils.Streams
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.streams.kstream.Suppressed.BufferConfig
import org.apache.kafka.streams.kstream.{Suppressed, TimeWindows, Windowed}
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.{KGroupedStream, KStream, TimeWindowedKStream}
import org.apache.kafka.streams.scala.serialization.Serdes
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.slf4j.{Logger, LoggerFactory}

import java.time.{Clock, Duration}
import java.util.Properties

object StreamJob {

  import serde._

  def start(): Unit = {

    implicit val log: Logger    = LoggerFactory.getLogger(StreamJob.getClass)
    implicit val clock: Clock   = Clock.systemUTC()
    implicit val config: Config = Config.load()
    val streamConfig            = config.streamConfig
    val kafkaConfig             = config.kafkaConfig

    log.debug("Bootstrapping kafka streams pipeline.")

    val streamConfiguration: Properties = {
      val properties = new Properties()
      properties.put(StreamsConfig.APPLICATION_ID_CONFIG, streamConfig.applicationId)
      properties.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, "4")
      properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.bootstrapServers)
      properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.bytesSerde.getClass.getName)
      properties.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.AT_LEAST_ONCE)
      properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      properties.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "0")
      properties
    }

    log.warn(s"Stream configuration is: \t$streamConfiguration")

    lazy val streams: KafkaStreams = {
      val builder = new StreamsBuilder()

      val scores: KStream[Long, String]       = builder.stream[Long, String](streamConfig.inputTopic)
      val latencyMS: KStream[Long, Long]      = scores.map((timestamp, _) => 0L -> (clock.millis() - timestamp))
      val grouped: KGroupedStream[Long, Long] = latencyMS.groupByKey

      val windowed: TimeWindowedKStream[Long, Long] =
        grouped.windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(1)))

      val aggregated: KStream[Windowed[Long], AvgLatency] = windowed
        .aggregate(AvgLatency.empty) { (_, latencyMS, state) =>
          val sum   = state.sum + latencyMS
          val count = state.count + 1
          AvgLatency(sum, count, sum / count.toDouble)
        }
        .suppress(Suppressed.untilWindowCloses(BufferConfig.unbounded()))
        .toStream

      aggregated.foreach { case (key, AvgLatency(_, cnt, avg)) =>
        log.info(s"For this ${key.key()}:${key.window().end()} window $cnt events. Average latency:\t $avg")
      }
      new KafkaStreams(builder.build(), streamConfiguration)
    }

    Streams.safeRun(streams)
  }
}
