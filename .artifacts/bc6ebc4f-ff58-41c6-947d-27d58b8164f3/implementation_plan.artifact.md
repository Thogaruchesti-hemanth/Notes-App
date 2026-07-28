# Implementation Plan - Enhancing Reminders UI/UX, Stability & Intelligence

This plan aims to transform the current Reminders feature into a more engaging, professional, and efficient experience. It addresses performance bottlenecks, modernizes the UI, and introduces "Smart Recurring Visibility."

## User Review Required

> [!IMPORTANT]
> - **Recurring Reminders Visibility**: Currently, repeating reminders only appear on their *next* trigger date. I will implement logic to show "Daily/Weekly" reminders on every applicable day in the calendar, even before they trigger.
> - **Timeline Layout**: I recommend **keeping** the timeline but making it "Flexible." This means:
>     - **Auto-Scroll**: The timeline will automatically scroll to the current hour or the first upcoming task.
>     - **Dynamic Density**: Hours with no tasks will be more compact, while active hours expand.
> - **Bottom Sheet Migration**: Replacing the "Edit/Delete" dialog with a modern Modal Bottom Sheet for a native feel.

## Proposed Changes

### 1. Data Layer & Recurring Logic [Efficiency & Visibility]

#### [MODIFY] [ReminderDao.java](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/java/com/example/NotesNest/databases/daos/ReminderDao.java)
- Keep fetching all active reminders for the ViewModel (since we need to calculate occurrences in memory for the calendar view).

#### [MODIFY] [RemindersFragment.java](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/java/com/example/NotesNest/fragments/RemindersFragment.java)
- **Occurrence Engine**: Update `filterRemindersForSelectedDate` to include reminders whose `notificationTime` is in the past but whose `repeatType` (Daily, Weekly, etc.) matches the `selectedDate`.
- **Auto-Scroll**: Implement `scrollToActiveHour()` after the timeline is loaded.

---

### 2. UI Components & Interaction [Engagement]

#### [NEW] [ReminderOptionsBottomSheet.java](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/java/com/example/NotesNest/fragments/ReminderOptionsBottomSheet.java)
- Modern `BottomSheetDialogFragment`.
- **Actions**: Edit, Delete, Snooze (Quick chips for 15m, 1h), and "Mark as Done."

#### [MODIFY] [item_timeline_hour.xml](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/res/layout/item_timeline_hour.xml)
- **Remove HorizontalScrollView**: Replace the inner scrolling with a simpler layout that relies on vertical density. This eliminates the "per-hour" scrolling frustration.

#### [MODIFY] [TimelineAdapter.java](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/java/com/example/NotesNest/adapter/TimelineAdapter.java)
- **Flexible Stacking**: Improve the `translationX` logic so that 3+ overlapping tasks remain readable (using a slight diagonal stagger).
- **Adaptive Sizing**: Tasks will dynamically resize their width based on how many are "conflicting" at that minute, ensuring they always fit within the screen.
- **Visual Feedback**: Add a "Done" state (grayed out with strike-through).

---

### 3. Stability & Scheduling [Reliability]

#### [MODIFY] [NotificationScheduler.java](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/java/com/example/NotesNest/notifications/schedulers/NotificationScheduler.java)
- Optimize `WorkManager` settings to ensure high priority for reminder triggers.

## Verification Plan

### Manual Verification
1. **Recurring Check**: Create a "Daily" reminder starting yesterday. Navigate to "Today" and "Tomorrow" in the calendar; verify it appears in the timeline on both days.
2. **Timeline Flexibility**: Add tasks at 8:00 AM and 10:00 PM. Verify that the view handles the distance gracefully and scrolls to the 8:00 AM task on open.
3. **Overlap Test**: Add 4 tasks at exactly 2:00 PM. Verify they stack neatly without going off-screen.
4. **Bottom Sheet**: Verify "Snooze" updates the next trigger time correctly.
