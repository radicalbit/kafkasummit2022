package io.radicalbit.ks22

import io.radicalbit.ks22.config.{Config, RestConfig}
import io.radicalbit.ks22.models.{MatchUpdate, SeldonPayload, SeldonResponse}
import org.slf4j.Logger

import java.time.Clock
import scala.collection.mutable.ArrayBuffer

object RestGenerator {

  /**
    * first task. write prediction to seldon. Then, write back result to output
    */
  def generate()(implicit log: Logger, config: Config, clock: Clock): Unit = {
    val restConf = config.restConfig
    log.debug("Bootstrapping rest producer to endpoint.")
    val producer = new RestProducer(restConf)

    val thread = new Thread(producer)
    thread.start()
  }
}

final class RestProducer(restConfig: RestConfig)(implicit log: Logger, clock: Clock) extends Runnable {
  import io.circe.generic.auto._
  import sttp.client3._
  import sttp.client3.circe._

  private lazy val connection = HttpURLConnectionBackend()
  private val uri             = uri"${restConfig.buildURI("seldon")}"

  override def run(): Unit = {
    val state = new ArrayBuffer[Long]()
    var time  = clock.millis()
    while (true) {
      val now   = clock.millis()
      val event = SeldonPayload(MatchUpdate.fromEpoch(now))

      val response: Identity[Response[Either[ResponseException[String, io.circe.Error], SeldonResponse]]] = basicRequest
        .post(uri)
        .body(event)
        .response(asJson[SeldonResponse])
        .send(connection)

      if (response.code.code != 200) {
        val msg = s"Cannot send rest prediction due to issue return code ${response.code.code}."
        val e   = new RuntimeException(msg)
        log.error(msg, e)
        throw e
      } else {
        response.body match {
          case Left(error) =>
            val msg = s"Cannot send rest prediction due to unknown error $error."
            val e   = new RuntimeException(msg, error.getCause)
            log.error(msg, error.getCause)
            throw e
          case Right(_) =>
            val newNow    = clock.millis()
            val latencyMS = newNow - now
            state.addOne(latencyMS)
            if (newNow - time > 1000) {
              val cnt = state.length
              val sum = state.sum
              log.info(s"For this window ${state.length} events. Average latency:\t ${sum / cnt.toDouble}")
              state.clear()
              time = clock.millis()
            }
        }
      }
    }
  }
}
