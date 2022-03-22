package io.radicalbit.ks22.models

object AvgLatency {
  def empty: AvgLatency = AvgLatency(0L, 0L, 0.0)
}

final case class AvgLatency(sum: Long, count: Long, avg: Double)
