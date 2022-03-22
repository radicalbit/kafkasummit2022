package io.radicalbit.ks22.models

// {"jsonData":{"quote":2.3,"winner":"Milan"},"meta":{"requestPath":{"classifier":"seldonio/mlflowserver:1.13.1"}}}

final case class MatchQuote(quote: Float, winner: String)
final case class SeldonResponse(jsonData: MatchQuote)
