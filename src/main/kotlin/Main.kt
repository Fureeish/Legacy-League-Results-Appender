import io.github.cdimascio.dotenv.dotenv
import models.LeagueStandingsPlayerRecord
import models.SeasonStandingsPlayerRecord
import java.io.File

const val MATH_MODE = "$$"
const val COLUMN_SEPARATOR = "|"

fun main() {
    val env = dotenv()

    val playerSeasonStandingsPath = env.get("PLAYER_SEASON_STANDINGS_PATH")
    val newestLeagueStandingsPath = env.get("NEWEST_LEAGUE_STANDINGS_PATH")
    val seasonNumber = env.get("SEASON_NUMBER").toInt()

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