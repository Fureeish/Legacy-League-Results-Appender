package models

import COLUMN_SEPARATOR
import FactoryFor
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.collections.component4

class LeagueStandingsPlayerRecord(val nameAndSurname: String, val score: Score) {
    val pointsScored: Int
        get() = score.wins * 3 + score.draws

    companion object : FactoryFor<LeagueStandingsPlayerRecord> {
        override fun fromLine(line: String): LeagueStandingsPlayerRecord {
            val (_, _, nameAndSurname, score) = line
                .split(COLUMN_SEPARATOR)
                .map { it.replace("$", "") }
                .map(String::trim)
            return LeagueStandingsPlayerRecord(nameAndSurname, Score.from(score))
        }
    }

    data class Score(val wins: Int, val losses: Int, val draws: Int) {
        companion object {
            fun from(text: String) = text
                .split('-')
                .map(String::toInt)
                .let { (wins, losses, draws) ->
                    Score(wins, losses, draws)
                }
        }
    }
}
