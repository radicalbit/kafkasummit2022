package io.radicalbit.ks22.utils

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse
import org.slf4j.Logger

object Streams {

  def safeRun(stream: KafkaStreams)(implicit log: Logger): Unit = {
    stream.setUncaughtExceptionHandler(new ExceptionHandler())
    stream.start()
  }

  private class ExceptionHandler()(implicit log: Logger) extends StreamsUncaughtExceptionHandler {

    override def handle(exception: Throwable): StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse = {
      log.error(s"Caught exception from.", exception)
      StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }
  }

}
