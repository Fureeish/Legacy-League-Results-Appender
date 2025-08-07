package models

import COLUMN_SEPARATOR
import FactoryFor
import MATH_MODE

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
