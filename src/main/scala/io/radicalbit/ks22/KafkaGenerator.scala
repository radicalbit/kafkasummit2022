package io.radicalbit.ks22

import io.radicalbit.ks22.config.Config
import io.radicalbit.ks22.models.{MatchUpdate, SeldonPayload}
import io.radicalbit.ks22.serde.MatchSerDe
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.LongSerializer
import org.slf4j.Logger

import java.time.Clock
import java.util.Properties
import scala.concurrent.Promise
import scala.util.Try

object KafkaGenerator {

  /**
    * first task. write prediction to seldon. Then, write back result to output
    */
  def generate()(implicit log: Logger, config: Config, clock: Clock): Unit = {
    val kafkaConfig = config.kafkaConfig

    log.debug("Bootstrapping kafka producer.")

    val producerConfig = {
      val properties = new Properties()
      properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.bootstrapServers)
      properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[LongSerializer].getName)
      properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[MatchSerDe].getName)
      properties
    }

    log.warn(s"Producer configuration is: \t$producerConfig")

    val producer = new KProducer(kafkaConfig.topic, producerConfig)

    val thread = new Thread(producer)

    thread.start()
  }
}

final class KProducer(topic: String, properties: Properties)(implicit log: Logger, clock: Clock) extends Runnable {
  private val producer = new KafkaProducer[Long, SeldonPayload](properties)

  override def run(): Unit =
    while (true) {
      val now    = clock.millis()
      val event  = SeldonPayload(MatchUpdate.fromEpoch(now))
      val record = new ProducerRecord[Long, SeldonPayload](topic, now, event)

      val metadata = producer.send(record)
      val promise  = Promise[RecordMetadata]()

      promise.complete(Try {
        val result = metadata.get
        log.debug(s"Message $result sent to Kafka successfully.")
        result
      }.recover { case e =>
        val msg = s"Cannot send message to Kafka due to issue with Kafka Producer."
        log.error(msg, e)
        throw new RuntimeException(msg, e)
      })
      promise.future
    }
}
