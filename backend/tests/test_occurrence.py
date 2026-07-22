from datetime import date, datetime, timedelta

from app.services.occurrence import compute_next_occurrence, is_valid_schedule


def test_one_time_future_event_returns_its_own_window():
    start = datetime(2100, 1, 1, 10, 0)
    result = compute_next_occurrence(start, 60, None, None, now=datetime(2026, 1, 1))
    assert result == (start, datetime(2100, 1, 1, 11, 0))


def test_one_time_past_event_returns_none():
    start = datetime(2000, 1, 1, 10, 0)
    result = compute_next_occurrence(start, 60, None, None, now=datetime(2026, 1, 1))
    assert result is None


def test_one_time_event_currently_in_progress_counts():
    start = datetime(2026, 1, 1, 10, 0)
    now = datetime(2026, 1, 1, 10, 30)
    result = compute_next_occurrence(start, 60, None, None, now=now)
    assert result == (start, datetime(2026, 1, 1, 11, 0))


def test_weekly_recurrence_anchored_in_past_finds_future_occurrence():
    start = datetime(2000, 1, 3, 9, 0)  # a Monday
    schedule = "0 9 * * 1"  # every Monday at 9:00
    now = datetime(2026, 7, 22, 12, 0)  # a Wednesday
    result = compute_next_occurrence(start, 30, schedule, None, now=now)
    assert result is not None
    occurrence_start, occurrence_end = result
    assert occurrence_start.weekday() == 0  # Monday
    assert occurrence_start >= now
    assert occurrence_end == occurrence_start + timedelta(minutes=30)


def test_weekly_recurrence_stops_after_frequency_end():
    start = datetime(2026, 1, 5, 9, 0)  # a Monday
    schedule = "0 9 * * 1"
    frequency_end = date(2026, 1, 12)  # only Jan 5 and Jan 12 are allowed
    now = datetime(2026, 1, 20)  # after both allowed occurrences have ended
    result = compute_next_occurrence(start, 30, schedule, frequency_end, now=now)
    assert result is None


def test_frequency_end_is_inclusive_of_start_date():
    start = datetime(2026, 1, 5, 9, 0)  # a Monday
    schedule = "0 9 * * 1"
    frequency_end = date(2026, 1, 5)  # same day as the first occurrence
    now = datetime(2026, 1, 1)  # before the first occurrence has even happened
    result = compute_next_occurrence(start, 30, schedule, frequency_end, now=now)
    assert result == (start, datetime(2026, 1, 5, 9, 30))


def test_invalid_cron_is_rejected():
    assert is_valid_schedule("not a cron string") is False


def test_valid_cron_is_accepted():
    assert is_valid_schedule("0 9 * * 1") is True
