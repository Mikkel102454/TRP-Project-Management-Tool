# Project Management Tool

## Optional Feature API integration

Projects can be linked to a PM release and then show live, read-only features from the Feature API. Configure the provider with environment variables; credentials are intentionally not stored in the repository:

```text
FEATURE_SYSTEM_API_SERVER=https://example.invalid/feature-api/
FEATURE_SYSTEM_AUTH_ID=...
FEATURE_SYSTEM_AUTH_KEY=...
```

A PM release can only be selected when its project is created. It is immutable afterward, and the project only exposes live, read-only PM tasks. Administrators can validate or remove a user's PM account ID from User Control. Release values are trimmed but retain their case.

Completed PM time registrations can be adjusted from the timetable and are synchronized back to the Feature API. Remote time deletion remains unavailable.
