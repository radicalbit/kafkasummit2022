package io.radicalbit.ks22.models

final case class MatchUpdate(
    team_home: String,
    team_away: String,
    score_a: Int,
    score_b: Int,
    possession_ab: String,
    shots_a: Int,
    shots_b: Int,
    match_status: String,
    elapsed_minutes: Int,
    timestamp_ms: Long
)

object MatchUpdate {

  def fromEpoch(millis: Long): MatchUpdate =
    MatchUpdate(
      team_home = "Milan",
      team_away = "Inter",
      score_a = 1,
      score_b = 0,
      possession_ab = "60,40",
      shots_a = 2,
      shots_b = 2,
      match_status = "PLAYING",
      elapsed_minutes = 14,
      timestamp_ms = millis
    )
}
