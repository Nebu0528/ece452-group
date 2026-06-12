package ca.uwaterloo.ece452.discoveruwaterloo.data

object MockData {
    val tags = listOf(
        Tag(1, "Engineering"),
        Tag(2, "Social"),
        Tag(3, "Academic"),
        Tag(4, "Sports"),
    )

    val events = listOf(
        Event(
            id = 1,
            name = "UW Hackathon 2026",
            description = "24-hour hackathon open to all UWaterloo students.",
            location = "E7 Building",
            date = null,
            userId = 2,
            reviewerId = 1,
            status = EventStatus.APPROVED,
            tags = listOf(tags[0], tags[1])
        ),
        Event(
            id = 2,
            name = "Engineering Research Fair",
            description = "Showcase of undergraduate and graduate research projects.",
            location = "DC Atrium",
            date = null,
            userId = 2,
            reviewerId = 1,
            status = EventStatus.APPROVED,
            tags = listOf(tags[0], tags[2])
        ),
        Event(
            id = 3,
            name = "Intramural Soccer Signup",
            description = "Sign up for the fall intramural soccer league.",
            location = "CIF Field",
            date = null,
            userId = 3,
            reviewerId = null,
            status = EventStatus.PENDING,
            tags = listOf(tags[3], tags[1])
        ),
    )
}
