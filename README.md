# 📝 NotesNest

<div align="center">

![NotesNest Logo](https://img.shields.io/badge/NotesNest-Productivity-blue?style=for-the-badge)
[![Android](https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android)](https://www.android.com/)
[![Java](https://img.shields.io/badge/Language-Java-orange?style=for-the-badge&logo=java)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red?style=for-the-badge)](LICENSE)

**Your All-in-One Productivity Companion**

A comprehensive Android productivity application designed to help you efficiently manage notes, tasks, reminders, and personal goals within a single, secure, and intuitive platform.

[Features](#-features) • [Screenshots](#-screenshots) • [Installation](#-installation) • [Tech Stack](#-tech-stack) • [Architecture](#-architecture) • [Changelog](#-changelog) 

</div>

---

## 🌟 Features

### 📝 **Smart Note Management**
- Create, edit, delete, and organise notes effortlessly
- Category and tag system for better organisation
- Switch between list and grid layout views
- Drag-and-drop reordering support for Categories.

### ⏰ **Intelligent Reminders**
- Set time-based reminders with local notifications
- Support for repeat reminders
- WorkManager/AlarmManager integration for reliability
- Never miss important tasks or deadlines

### 📤 **Export & Share**
- Export notes as **Plain Text (.txt)**
- Export notes as **PDF** (using PdfDocument API)
- Export notes as **Images** (Canvas rendering)
- Share via Android Intent to any supported app (Email, WhatsApp, etc.)

### 🏠 **Home Screen Widgets**
- **Note Preview Widget**: Display selected notes directly on home screen
- **Quick Add Widget**: Add new notes instantly
- Real-time widget updates

### 💾 **Backup & Restore**
- **Local Backup**: Encrypted SQLite export for maximum privacy
- **Cloud Backup**: Google Drive API integration for seamless sync
- Easy restore functionality
- Data migration support across devices

### 🎨 **Modern UI/UX**
- Material Design 3 implementation
- Light and dark theme support with auto-switching
- Smooth animations and transitions
- Adaptive layouts for phones, tablets, and landscape mode
- Dynamic greetings based on time of day
- Interactive onboarding experience

### 🔒 **Security & Privacy**
- Firebase Authentication with email/password
- Optional Google Sign-In (OAuth)
- Encrypted SharedPreferences for session management
- Strong password policies and input validation
- Secure credential storage (no plain text passwords)
- Privacy-first design philosophy

### 📊 **Additional Features**
- Dynamic Note counts 
- Profile editing (name, picture)
- Contact support and the FAQ section
- Firebase Remote Config for dynamic feature control
- Firebase Crashlytics for real-time crash monitoring
- Semantic versioning for clear release tracking

---

## 📱 Screenshots

<div align="center">

| Slpash Screen                       | Login Screen              | SignUp Screen                       | Notes Dashoard                      | Edit Note Screen                     |
| ------------------------------- | ----------------------- | ------------------------------- | ------------------------------ | --------------------------- |
| <img alt="Slpash Screen" src="https://github.com/user-attachments/assets/09241de7-718a-4c6b-83da-140640d70405" width="180" height="380" /> | <img alt="Screenshot_20260110_135633" src="https://github.com/user-attachments/assets/a751fa72-aa8c-42a5-8f8d-92d1126b9333" width="180" height="380" />| <img alt="Screenshot_20260110_141335" src="https://github.com/user-attachments/assets/de665d75-2359-4a1b-8846-a0a76a5c2815" width="180" height="380"  />| <img alt="Screenshot_20260110_135949" src="https://github.com/user-attachments/assets/39f984d1-bb50-4e46-8465-42c09cad3894"  width="180" height="380" />| <img alt="Screenshot_20260110_140016" src="https://github.com/user-attachments/assets/14738f08-09b5-4e52-a18f-bb70453eb454" width="180" height="380" />|


| Category Manager                    | Note Dialog                    | Share Options                        | Reminder Dashboard                  | Add Reminder Screen                      |
| ------------------------- | ------------------------- | --------------------------------- | --------------------- | ----------------------------- |
| <img alt="Screenshot_20260110_140002" src="https://github.com/user-attachments/assets/0c5d3576-80b7-45c6-9d7e-f618367f1a58" width="180" height="380" /> |  <img alt="Screenshot_20260110_140122" src="https://github.com/user-attachments/assets/9e0500c9-39ab-459e-bd0f-fddec933341a" width="180" height="380" />| <img alt="Screenshot_20260110_140132" src="https://github.com/user-attachments/assets/8b8bd034-a85d-4351-b174-d6da4919c8da" width="180" height="380" />| <img alt="Screenshot_20260110_140652" src="https://github.com/user-attachments/assets/d007a801-6ff7-418e-9397-3557f5229c67" width="180" height="380"/> | <img alt="Screenshot_20260110_140704" src="https://github.com/user-attachments/assets/9fb38af7-c765-47f0-a3bb-7ca11d94ea85" width="180" height="380"/> |

| Drawer                    | Settings Screen                    | Help and Support Screen                        | Manage Account Screen                  | Drive Backup Screen                      |
| ------------------------- | ------------------------- | --------------------------------- | --------------------- | ----------------------------- |
| <img alt="Screenshot_20260110_140027" src="https://github.com/user-attachments/assets/5f8c51c9-924e-4055-9b76-299c622c0a2a" width="180" height="380" />| <img alt="Screenshot_20260110_140041" src="https://github.com/user-attachments/assets/6ff67473-3d0e-4fc6-8f6d-e97c6c8fabf4"  width="180" height="380" />| <img  alt="Screenshot_20260110_140107" src="https://github.com/user-attachments/assets/d26b59de-0ed4-49f5-a53c-36f025ff6d8e" width="180" height="380" />| <img alt="Screenshot_20260110_140905" src="https://github.com/user-attachments/assets/201310ce-cc51-44af-bea0-a3766f0df5d6" width="180" height="380" />| <img alt="Screenshot_20260110_140916" src="https://github.com/user-attachments/assets/6d98c85a-b910-484f-a54b-025e93f59b97" width="180" height="380"/> |

| Edit Profile Dialog                    | Quick Note Widget                    | Config Note Widget Screen                        | Note Widget                  | Dark Mode Dashboard                      |
| ------------------------- | ------------------------- | --------------------------------- | --------------------- | ----------------------------- |
| <img alt="Screenshot_20260110_140719" src="https://github.com/user-attachments/assets/318e2efc-be5e-4d35-a91b-e3760740cfbe" width="180" height="380"/>|<img alt="Screenshot_20260110_144004" src="https://github.com/user-attachments/assets/9fea5f60-8c14-4691-b4f6-014003ae11cb" width="180" height="380"/>| <img alt="Screenshot_20260110_144021" src="https://github.com/user-attachments/assets/35f47b27-abab-4544-ae82-db6529b783af" width="180" height="380" />| <img  alt="Screenshot_20260110_144039" src="https://github.com/user-attachments/assets/df0211be-7695-48d1-8c46-72f5471b3375"  width="180" height="380" />|<img alt="Screenshot_20260110_144638" src="https://github.com/user-attachments/assets/bf5f2684-a949-452c-8309-c02888c1de65" width="180" height="380"/> |



</div>

---

## 🚀 Installation

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or higher
- Android SDK (API 29+)
- Firebase account (for authentication and analytics)
- Google Drive API credentials (for cloud backup)

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/Thogaruchesti-hemanth/NotesNest.git
   cd NotesNest
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Firebase Configuration**
   - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Add an Android app to your Firebase project
   - Download `google-services.json`
   - Place it in the `app/` directory
   - Enable Authentication (Email/Password and Google Sign-In)
   - Enable Crashlytics and Remote Config

4. **Google Drive API Setup**
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Enable Google Drive API
   - Create OAuth 2.0 credentials
   - Add credentials to your project

5. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or simply click the "Run" button in Android Studio

---

## 🛠️ Tech Stack

### Core Technologies
- **Language**: Java
- **IDE**: Android Studio
- **Build Tool**: Gradle

### Architecture & Design
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **Design System**: Material Design 3
- **UI Components**: AndroidX, Material Components

### Database & Storage
- **Local Database**: Room Database (SQLite wrapper)
- **SharedPreferences**: EncryptedSharedPreferences
- **Cloud Storage**: Google Drive API

### Backend & Services
- **Authentication**: Firebase Authentication
- **Analytics**: Firebase Analytics
- **Crash Reporting**: Firebase Crashlytics
- **Remote Config**: Firebase Remote Config
- **Background Tasks**: WorkManager

### Key Libraries
```gradle
// Room Database
implementation "androidx.room:room-runtime:2.5.0"
annotationProcessor "androidx.room:room-compiler:2.5.0"

// Firebase
implementation platform('com.google.firebase:firebase-bom:32.0.0')
implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-crashlytics'
implementation 'com.google.firebase:firebase-analytics'
implementation 'com.google.firebase:firebase-config'

// Material Design
implementation 'com.google.android.material:material:1.9.0'

// WorkManager
implementation "androidx.work:work-runtime:2.8.1"

// Google Drive API
implementation 'com.google.android.gms:play-services-drive:17.0.0'
```

---

## 🏗️ Architecture

### Project Structure
```
NotesNest/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/yourpackage/notesnest/
│   │   │   │   ├── activities/          # UI Activities
│   │   │   │   ├── adapters/            # RecyclerView Adapters
│   │   │   │   ├── database/            # Room Database
│   │   │   │   │   ├── entities/        # Data Entities
│   │   │   │   │   ├── dao/             # Data Access Objects
│   │   │   │   │   └── AppDatabase.java
│   │   │   │   ├── fragments/           # UI Fragments
│   │   │   │   ├── models/              # Data Models
│   │   │   │   ├── repositories/        # Data Repositories
│   │   │   │   ├── utils/               # Utility Classes
│   │   │   │   ├── viewmodels/          # ViewModels
│   │   │   │   └── widgets/             # Home Screen Widgets
│   │   │   ├── res/                     # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── test/                        # Unit Tests
│   └── build.gradle
├── gradle/
├── build.gradle
└── settings.gradle
```

### Database Schema

**Notes Table**
- `id` (Primary Key)
- `userId`
- `CategoryId`
- `title`
- `content`
- `colorHex`
- `createdAt`
- `updatedAt`
- `isSynced`
- `isDeleted`

**Reminders Table**
- `id` (Primary Key)
- `userId`
- `type` (TODO / TASK / BIRTHDAY) 
- `title`
- `message`
- `name` // birthday person name
- `notificationTime`  // millis
- `isRepeated`
- `repeatType`       // Daily, Weekly, Monthly, Yearly, None
- `notifyType`       // onThatDay, dayBefore, 2 daysBefore, 1Week Before
- `isRepeating`
- `gradientStartColor`
- `gradientEndColor`
- `isSynced`
- `isDeleted`
- `updatedAt`

**Category Table**
- `id`
- `name`
- `order`

---

## 📋 Changelog

### Version 3.0.0 (22/12/2025) - Latest
#### ✨ New Features
- Export & Share notes (TXT, PDF, Image)
- Home screen widgets (Preview & Quick Add)
- Backup & Restore system (Local & Cloud)
- Toggle between list and grid layouts
- Premium purchase UI groundwork
- Firebase Remote Config integration

#### 🐛 Bug Fixes
- Improved PDF export error handling
- Fixed widget update crashes
- Resolved note sharing issues on older Android versions
- Fixed backup file overwrite problems

### Version 2.0.0 (08/11/2025)
#### ✨ New Features
- Complete UI/UX overhaul with Material Design 3
- Enhanced authentication with password reset
- Google Sign-In integration
- Category and tag system
- Advanced search and filtering
- Profile editing capabilities
- Migration to Room Database

#### 🔧 Improvements
- Database optimisation with indexing
- Memory leak fixes
- App size optimisation
- Performance enhancements

### Version 1.0.0 (14/04/2025)
- Initial stable release
- Core note management features
- Basic reminders and to-do lists
- SQLite database implementation
- Light and dark theme support

[View Full Release Notes](Release_Notes_Template.docx)

---

## 🎯 Roadmap

### Upcoming Features (Version 4.0.0)
- [ ] Voice-to-text note creation
- [ ] Advanced analytics dashboard
- [ ] Wear OS companion app
- [ ] Premium subscription features

### Under Consideration
- [ ] AI-powered note suggestions
- [ ] Integration with calendar apps
- [ ] Markdown support

---

## 🐛 Bug Reports & Feature Requests

Found a bug or have a feature request? Please check existing [Issues](https://github.com/Thogaruchesti-hemanth/NotesNest/issues) first, then create a new one if needed.

### Bug Report Template
```markdown
**Description**: Brief description of the bug
**Steps to Reproduce**: 
1. Step 1
2. Step 2
**Expected Behavior**: What should happen
**Actual Behavior**: What actually happens
**Device**: Device model and Android version
**App Version**: NotesNest version
**Screenshots**: If applicable
```

---

## 📄 License

© 2025 Thogaruchesti Hemanth - All Rights Reserved

This project is proprietary software. Unauthorised copying, modification, distribution, or use of this software, via any medium, is strictly prohibited without explicit permission from the author.

---

## 👨‍💻 Developer

**Thogaruchesti Hemanth**

- 📧 Email: [saihemanth225@gmail.com](mailto:saihemanth225@gmail.com)
- 💼 LinkedIn: [thogaruchesti-hemanth](https://www.linkedin.com/in/thogaruchesti-hemanth/)
- 🐙 stackOverFlow: [Thogaruchesti-hemanth](https://stackoverflow.com/users/28964013/thogaruchesti-hemanth)

---

## 🙏 Acknowledgments

- **Material Design** - For comprehensive design guidelines
- **Firebase** - For authentication and backend services
- **Android Developers** - For excellent documentation
- **Open Source Community** - For inspiration and libraries

---

## 📊 Project Stats

![GitHub repo size](https://img.shields.io/github/repo-size/Thogaruchesti-hemanth/NotesNest)
![GitHub stars](https://img.shields.io/github/stars/Thogaruchesti-hemanth/NotesNest?style=social)
![GitHub forks](https://img.shields.io/github/forks/Thogaruchesti-hemanth/NotesNest?style=social)
![GitHub issues](https://img.shields.io/github/issues/Thogaruchesti-hemanth/NotesNest)
![GitHub last commit](https://img.shields.io/github/last-commit/Thogaruchesti-hemanth/NotesNest)

---

<div align="center">

**Made with ❤️ by Thogaruchesti Hemanth**

[⬆ Back to Top](#-notesnest)

</div>
