# Task List - Reminders UI/UX & Intelligence Enhancement

- `[x]` **Phase 1: Data & Recurring Logic**
    - `[x]` Add `isDone` to `ReminderEntity` and update Room database.
    - `[x]` Implement Occurrence Engine in `RemindersFragment` to show recurring tasks.
- `[x]` **Phase 2: Timeline Modernization**
    - `[x]` Update `item_timeline_hour.xml` (Remove horizontal scroll).
    - `[x]` Refactor `TimelineAdapter` for adaptive stacking/sizing.
    - `[x]` Implement Auto-Scroll to current hour in `RemindersFragment`.
- `[x]` **Phase 3: Interaction & Management**
    - `[x]` Create `ReminderOptionsBottomSheet` (Edit, Delete, Snooze, Done).
    - `[x]` Integrate Bottom Sheet into `RemindersFragment`.
- `[x]` **Phase 4: Verification & Polish**
    - `[x]` Verify recurring visibility.
    - `[x]` Test "Snooze" and "Mark as Done" logic.
    - `[x]` Final UI polish for "Done" state (grayed out).
