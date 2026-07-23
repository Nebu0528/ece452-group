from datetime import date, datetime, timedelta
from typing import Optional

from croniter import croniter

MAX_ITERATIONS = 10_000


def is_valid_schedule(schedule: str) -> bool:
    return croniter.is_valid(schedule)


def compute_next_occurrence(
    start_time: datetime,
    duration_minutes: int,
    schedule: Optional[str],
    frequency_end: Optional[date],
    now: datetime,
) -> Optional[tuple[datetime, datetime]]:
    """Return the (start, end) of the earliest occurrence of this event whose
    end has not yet passed relative to `now`, honoring `frequency_end`
    (inclusive last date an occurrence may start on). Returns None if the
    event/series has permanently concluded.

    `start_time` is always a valid occurrence in its own right, regardless of
    whether it satisfies `schedule` — the series is defined to begin there.
    """
    if frequency_end is not None and start_time.date() > frequency_end:
        return None

    first_end = start_time + timedelta(minutes=duration_minutes)
    if first_end >= now:
        return (start_time, first_end)

    if schedule is None:
        return None

    cron = croniter(schedule, start_time)
    for _ in range(MAX_ITERATIONS):
        occurrence_start = cron.get_next(datetime)
        if frequency_end is not None and occurrence_start.date() > frequency_end:
            return None
        occurrence_end = occurrence_start + timedelta(minutes=duration_minutes)
        if occurrence_end >= now:
            return (occurrence_start, occurrence_end)
    return None
