package ca.uwaterloo.ece452.discoveruwaterloo.data.routing

import org.osmdroid.util.GeoPoint
import kotlin.math.sqrt

data class Milestone(
    val id: String,
    val name: String,
    val location: GeoPoint
)

object MilestoneRouter {
    val milestones = listOf(
        Milestone("SLC", "Student Life Centre", GeoPoint(43.4718, -80.5456)),
        Milestone("PAC", "Physical Activities Complex", GeoPoint(43.4722, -80.5469)),
        Milestone("MC", "Mathematics & Computer", GeoPoint(43.4723, -80.5448)),
        Milestone("DC", "Davis Centre", GeoPoint(43.4729, -80.5439)),
        Milestone("QNC", "Quantum Nano Centre", GeoPoint(43.4714, -80.5442)),
        Milestone("E7", "Engineering 7", GeoPoint(43.4729, -80.5401)),
        Milestone("SCH", "South Campus Hall", GeoPoint(43.4697, -80.5423)),
        Milestone("CPH", "Carl A. Pollock Hall", GeoPoint(43.4716, -80.5414)),
        Milestone("AL", "Arts Lecture Hall", GeoPoint(43.4705, -80.5449)),
        Milestone("DP", "Dana Porter Library", GeoPoint(43.4699, -80.5445))
    )

    private val edges = mapOf(
        "SLC" to listOf("PAC", "MC", "QNC", "AL"),
        "PAC" to listOf("SLC"),
        "MC" to listOf("SLC", "DC", "QNC"),
        "DC" to listOf("MC", "E7"),
        "QNC" to listOf("SLC", "MC", "E7", "SCH", "AL"),
        "E7" to listOf("DC", "QNC", "CPH"),
        "CPH" to listOf("E7", "SCH"),
        "SCH" to listOf("QNC", "CPH", "DP"),
        "AL" to listOf("SLC", "QNC", "DP"),
        "DP" to listOf("AL", "SCH")
    )

    private fun distance(p1: GeoPoint, p2: GeoPoint): Double {
        val dLat = p1.latitude - p2.latitude
        val dLng = p1.longitude - p2.longitude
        return sqrt(dLat * dLat + dLng * dLng)
    }

    fun findClosestMilestone(point: GeoPoint): Milestone {
        return milestones.minByOrNull { distance(it.location, point) } ?: milestones[0]
    }

    fun findRoute(start: GeoPoint, end: GeoPoint): List<Milestone> {
        val startMilestone = findClosestMilestone(start)
        val endMilestone = findClosestMilestone(end)

        if (startMilestone.id == endMilestone.id) {
            return listOf(startMilestone)
        }

        // Dijkstra's Algorithm
        val dist = mutableMapOf<String, Double>()
        val prev = mutableMapOf<String, String>()
        val unvisited = milestones.map { it.id }.toMutableSet()

        milestones.forEach {
            dist[it.id] = Double.MAX_VALUE
        }
        dist[startMilestone.id] = 0.0

        while (unvisited.isNotEmpty()) {
            val u = unvisited.minByOrNull { dist[it] ?: Double.MAX_VALUE } ?: break
            if (dist[u] == Double.MAX_VALUE) break
            if (u == endMilestone.id) break

            unvisited.remove(u)

            val neighbors = edges[u] ?: emptyList()
            for (v in neighbors) {
                if (v in unvisited) {
                    val uMilestone = milestones.first { it.id == u }
                    val vMilestone = milestones.first { it.id == v }
                    val alt = dist[u]!! + distance(uMilestone.location, vMilestone.location)
                    if (alt < (dist[v] ?: Double.MAX_VALUE)) {
                        dist[v] = alt
                        prev[v] = u
                    }
                }
            }
        }

        // Reconstruct path
        val path = mutableListOf<Milestone>()
        var currId: String? = endMilestone.id
        while (currId != null) {
            val milestone = milestones.firstOrNull { it.id == currId }
            if (milestone != null) {
                path.add(0, milestone)
            }
            currId = prev[currId]
        }

        return if (path.firstOrNull()?.id == startMilestone.id) path else emptyList()
    }
}
