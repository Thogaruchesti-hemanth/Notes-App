# Implementation Plan - Elegant Full-Screen Reminder UI

I will redesign the full-screen reminder alarm interface to match the provided elegant design and update the reminder color palette to premium gradients.

## Proposed Changes

### [UI Components]

#### [MODIFY] [activity_reminder_alarm.xml](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/res/layout/activity_reminder_alarm.xml)
- Completely overhaul the layout to match the provided image:
    - Add "REMINDER" header text.
    - Add a pill-shaped badge for the reminder type.
    - Style the Title and Message with appropriate typography.
    - Add a horizontal divider.
    - Implement a 3-column layout for Date, Time, and Repeat Frequency details with icons.
    - Update the "Dismiss" button to be a large, dark, elegant button at the bottom.

#### [MODIFY] [ReminderAlarmActivity.java](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/java/com/example/NotesNest/activity/ReminderAlarmActivity.java)
- Update data binding to populate the new UI elements:
    - Format and display the Date and Day of the week.
    - Format and display the Time.
    - Display the Repeat Frequency.
    - Dynamically apply the gradient background using `gradientStartColor` and `gradientEndColor` passed from the reminder data.

### [Data & Logic]

#### [MODIFY] [Constants.java](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/java/com/example/NotesNest/utils/Constants.java)
- Replace the existing `professionalGradients` with a new set of "light and elegant premium colors" as requested. These will be used for both the note selection and the alarm background.

#### [MODIFY] [NotificationHelper.java](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/java/com/example/NotesNest/notifications/helper/NotificationHelper.java)
- Pass all required reminder details to the `ReminderAlarmActivity` via the full-screen intent:
    - `type`
    - `notificationTime`
    - `repeatType`
    - `gradientStartColor`
    - `gradientEndColor`

#### [MODIFY] [NotificationWorker.java](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/java/com/example/NotesNest/notifications/workers/NotificationWorker.java)
- Extract these extra fields from the `ReminderEntity` and pass them to the `NotificationHelper.postNotification` call.

## Verification Plan

### Manual Verification
1. **Visual Check**: Trigger a reminder and verify the screen looks exactly like the requested design.
2. **Dynamic Data**: Verify that the correct date, time, and repeat frequency are shown.
3. **Background**: Verify that the screen uses the elegant gradient background assigned to the reminder.
4. **Color Selection**: Open the "Add Reminder" screen and verify that the new premium gradients are available for selection.
