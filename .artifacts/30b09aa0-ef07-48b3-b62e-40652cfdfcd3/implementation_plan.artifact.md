# Implementation Plan - Premium Category UI Refinement

This plan focuses on making the Category Management UI look more "premium" and modern by adopting Material 3 design principles, specifically regarding separators and whitespace.

## User Review Required

> [!TIP]
> **To achieve a premium look, less is often more.**
> I recommend **deleting the separator lines** (`View`) and instead using **generous whitespace** and **interactive feedback (ripples)**. Hard lines can make a UI feel "busy" or "dated," especially in modern Android designs.

## Proposed Changes

### [UI Layouts]

#### [MODIFY] [item_category_manage.xml](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/res/layout/item_category_manage.xml)
- **Delete the separator `View`**: This will clean up the visual clutter.
- **Add Interactive Feedback**: Change the background to use `?attr/selectableItemBackground` to provide a nice ripple effect when touched.
- **Optimize Spacing**: Slightly increase the vertical padding of the item to ensure clear separation between categories without needing a physical line.
- **Align Icons**: Ensure the drag handle and delete icons are perfectly centered and have enough "breathable" room.

#### [MODIFY] [bottom_sheet_manage_categories.xml](file:///C:/Users/saihe/AndroidStudioProjects/NotesNest/app/src/main/res/layout/bottom_sheet_manage_categories.xml)
- Add a small `View` as a top divider if needed, or just rely on the existing layout. (Current layout already has a `dragHandleView`).

## Verification Plan

### Manual Verification
1.  **Visual Check**: Open Category Management. Verify the list looks "cleaner" and more open without the grey lines.
2.  **Interaction Check**: Tap on a category. Verify that a ripple effect fills the item background, providing professional feedback.
3.  **Density Check**: Ensure that with multiple categories, the list doesn't feel "squeezed" or "loose."
