from datetime import date, datetime

import pytest
from pydantic import ValidationError

from app.schemas.event import EventCreate


def _base_payload(**overrides):
    payload = dict(
        name="Test Event",
        start_time=datetime(2026, 8, 1, 10, 0),
        duration=60,
    )
    payload.update(overrides)
    return payload


def test_valid_schedule_is_accepted():
    event = EventCreate(**_base_payload(schedule="0 9 * * 1"))
    assert event.schedule == "0 9 * * 1"


def test_invalid_cron_is_rejected():
    with pytest.raises(ValidationError):
        EventCreate(**_base_payload(schedule="not a cron string"))


def test_frequency_end_without_schedule_is_rejected():
    with pytest.raises(ValidationError):
        EventCreate(**_base_payload(frequency_end=date(2026, 12, 31)))


def test_frequency_end_before_start_date_is_rejected():
    with pytest.raises(ValidationError):
        EventCreate(**_base_payload(schedule="0 9 * * 1", frequency_end=date(2026, 7, 1)))


def test_frequency_end_on_start_date_is_accepted():
    event = EventCreate(**_base_payload(schedule="0 9 * * 1", frequency_end=date(2026, 8, 1)))
    assert event.frequency_end == date(2026, 8, 1)


def test_no_schedule_and_no_frequency_end_is_accepted():
    event = EventCreate(**_base_payload())
    assert event.schedule is None
    assert event.frequency_end is None
