package io.radicalbit.ks22.serde

import io.circe.generic.auto._
import io.circe.syntax._
import io.radicalbit.ks22.models.SeldonPayload
import org.apache.kafka.common.serialization.Serializer

class MatchSerDe extends Serializer[SeldonPayload] {

  override def serialize(topic: String, data: SeldonPayload): Array[Byte] = {
    val payload = data.asJson.noSpaces
    payload.getBytes
  }
}
