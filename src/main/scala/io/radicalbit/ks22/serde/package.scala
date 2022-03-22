package io.radicalbit.ks22

import io.circe.Json
import io.radicalbit.ks22.models.AvgLatency
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.scala.ByteArrayWindowStore
import org.apache.kafka.streams.scala.kstream.{Consumed, Grouped, Materialized}
import org.apache.kafka.streams.scala.serialization.Serdes
import org.apache.kafka.streams.scala.serialization.Serdes._

import java.nio.charset.Charset
import scala.util.{Failure, Success}

package object serde {
  import io.circe.generic.auto._
  import io.circe.syntax._
  import io.circe.parser._

  implicit val avgLatencyserde: Serde[AvgLatency] = Serdes.fromFn[AvgLatency](
    { item: AvgLatency => item.asJson.noSpaces.getBytes("UTF-8") },
    { bytes: Array[Byte] =>
      val str = new String(bytes, Charset.forName("UTF-8"))
      val ret = parse(str).map(_.as[AvgLatency])
      ret match {
        case Right(value)    => value.toOption
        case Left(exception) => throw exception
      }
    }
  )

  implicit val inputserde: Consumed[Long, String] = Consumed.`with`[Long, String]
  implicit val groupedserde: Grouped[Long, Long]  = Grouped.`with`[Long, Long]

  implicit val materialized: Materialized[Long, AvgLatency, ByteArrayWindowStore] =
    Materialized.`with`[Long, AvgLatency, ByteArrayWindowStore]
}
