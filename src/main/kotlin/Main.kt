import java.io.File
import kotlin.collections.drop
import kotlin.collections.dropLast
import kotlin.collections.filterNotNull
import kotlin.collections.find
import kotlin.collections.first
import kotlin.collections.forEach
import kotlin.collections.joinToString
import kotlin.collections.lastIndex
import kotlin.collections.map
import kotlin.collections.plusAssign
import kotlin.collections.sort
import kotlin.collections.sum
import kotlin.collections.toList
import kotlin.collections.toMutableList
import kotlin.collections.withIndex
import kotlin.io.useLines
import kotlin.let
import kotlin.sequences.drop
import kotlin.sequences.filter
import kotlin.sequences.map
import kotlin.sequences.toMutableList
import kotlin.text.contains
import kotlin.text.orEmpty
import kotlin.text.replace
import kotlin.text.split
import kotlin.text.startsWith
import kotlin.text.toInt
import kotlin.text.toIntOrNull
import kotlin.text.trim

const val MATH_MODE = "$$"
const val COLUMN_SEPARATOR = "|"

interface FactoryFor<T> {
    fun fromLine(line: String): T
}

class SeasonStandingsPlayerRecord(
    val nameAndSurname: String,
    val scores: MutableList<Int?> = mutableListOf(),
) : Comparable<SeasonStandingsPlayerRecord> {
    val totalPoints: Int
        get() = scores.filterNotNull().sum()

    override fun compareTo(other: SeasonStandingsPlayerRecord): Int {
        return other.totalPoints.compareTo(totalPoints)
    }

    override fun toString(): String {
        val formattedPoints = scores.joinToString(separator = " $COLUMN_SEPARATOR ") { it?.toString().orEmpty() }

        return listOf(nameAndSurname, "$MATH_MODE $totalPoints $MATH_MODE", formattedPoints)
            .joinToString(
                prefix = "$COLUMN_SEPARATOR ",
                postfix = " $COLUMN_SEPARATOR",
                separator = " $COLUMN_SEPARATOR "
            )
    }

    companion object : FactoryFor<SeasonStandingsPlayerRecord> {
        override fun fromLine(line: String): SeasonStandingsPlayerRecord {
            val rowCells = line.split(COLUMN_SEPARATOR)
            val rawPoints = rowCells.drop(4).dropLast(1)

            val nameAndSurname = rowCells[2].trim()
            val points = rawPoints.map { it.trim().toIntOrNull() }

            val result = SeasonStandingsPlayerRecord(nameAndSurname, points.toMutableList())
            return result
        }
    }
}

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

fun main() {
    val playerSeasonStandingsPath =
        "\\\\wsl.localhost\\Ubuntu-22.04\\home\\filip\\LordsOfLegacy\\_posts\\Season 1\\2025-06-25-Legacy-League-Standings-Season-1.md"
    val newestLeagueStandingsPath =
        "\\\\wsl.localhost\\Ubuntu-22.04\\home\\filip\\LordsOfLegacy\\_posts\\Season 1\\2025-08-06-Legacy-League-1-6.md"
    val seasonNumber = 1

    val playerSeasonStandings = getPlayerStandings(playerSeasonStandingsPath, SeasonStandingsPlayerRecord)
    val playerLeagueStandings = getPlayerStandings(newestLeagueStandingsPath, LeagueStandingsPlayerRecord)

    addNewScoreColumnToAllPlayers(playerSeasonStandings)
    val numberOfLeaguesSoFar = playerSeasonStandings.first().scores.size

    for (newLeagueResult in playerLeagueStandings) {
        /*
         * I'm using `contains` rather than `==` for comparing names
         * because sometimes the file with standings contains some extra HTML
         * for meme purposes.
         */
        val seasonRecord = playerSeasonStandings.find {
            it.nameAndSurname.contains(newLeagueResult.nameAndSurname)
        } ?: run {
            val emptyScoresForAllLeaguesPlayedSoFar = MutableList<Int?>(numberOfLeaguesSoFar) { null }
            val standingsForNewPlayer = SeasonStandingsPlayerRecord(
                newLeagueResult.nameAndSurname, emptyScoresForAllLeaguesPlayedSoFar
            )

            playerSeasonStandings.add(standingsForNewPlayer)

            standingsForNewPlayer
        }

        seasonRecord.scores.let { it[it.lastIndex] = newLeagueResult.pointsScored }
    }

    playerSeasonStandings.sort()

    val firstRow = getColumnNames(numberOfLeaguesSoFar, seasonNumber)
    val secondRow = getColumnAlignments(numberOfLeaguesSoFar)

    println(firstRow)
    println(secondRow)

    for ((index, player) in playerSeasonStandings.withIndex()) {
        println("$COLUMN_SEPARATOR $MATH_MODE ${index + 1}. $MATH_MODE $player")
    }
}

private fun getColumnNames(numberOfLeaguesSoFar: Int, seasonNumber: Int): String {
    val columns = mutableListOf("Place", "Name and surname", "Total points")
    columns += (1..numberOfLeaguesSoFar)
        .map { "[$it][league-$seasonNumber-$it]" }
        .toList()

    return columns.joinToString(
        prefix = COLUMN_SEPARATOR,
        postfix = COLUMN_SEPARATOR,
        separator = COLUMN_SEPARATOR
    )
}

fun getColumnAlignments(numberOfLeaguesSoFar: Int): String {
    val alignments = mutableListOf(":-:", "-")
    alignments += (0..numberOfLeaguesSoFar)
        .map { ":-:" }
        .toList()

    return alignments.joinToString(
        prefix = COLUMN_SEPARATOR,
        postfix = COLUMN_SEPARATOR,
        separator = COLUMN_SEPARATOR
    )
}

private fun addNewScoreColumnToAllPlayers(playerSeasonStandings: List<SeasonStandingsPlayerRecord>) {
    playerSeasonStandings.forEach { it.scores += null }
}

private fun <T> getPlayerStandings(path: String, factory: FactoryFor<T>): MutableList<T> {
    return File(path).useLines { lines ->
        lines
            .filter { it.startsWith(COLUMN_SEPARATOR) }
            .drop(2)
            .map(factory::fromLine)
            .toMutableList()
    }
}