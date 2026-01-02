# 📝 NotesNest

<div align="center">

![NotesNest Logo](https://img.shields.io/badge/NotesNest-Productivity-blue?style=for-the-badge)
[![Android](https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android)](https://www.android.com/)
[![Java](https://img.shields.io/badge/Language-Java-orange?style=for-the-badge&logo=java)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red?style=for-the-badge)](LICENSE)

**Your All-in-One Productivity Companion**

A comprehensive Android productivity application designed to help you efficiently manage notes, tasks, reminders, and personal goals within a single, secure, and intuitive platform.

[Features](#-features) • [Screenshots](#-screenshots) • [Installation](#-installation) • [Tech Stack](#-tech-stack) • [Architecture](#-architecture) • [Changelog](#-changelog) • [Contributing](#-contributing)

</div>

---

## 🌟 Features

### 📝 **Smart Note Management**
- Create, edit, delete, and organize notes effortlessly
- Category and tag system for better organization
- Advanced search functionality with filters
- Switch between list and grid layout views
- Drag-and-drop reordering support
- Separate section for important/priority notes

### ⏰ **Intelligent Reminders**
- Set time-based reminders with local notifications
- Support for repeat reminders
- WorkManager/AlarmManager integration for reliability
- Never miss important tasks or deadlines

### ✅ **Task Management**
- Create and manage to-do lists
- Mark tasks as complete or pending
- Track daily productivity efficiently
- Dedicated wish list for future goals

### 📤 **Export & Share**
- Export notes as **Plain Text (.txt)**
- Export notes as **PDF** (using PdfDocument API)
- Export notes as **Images** (Canvas rendering)
- Share via Android Intent to any supported app (Email, WhatsApp, etc.)

### 🏠 **Home Screen Widgets**
- **Note Preview Widget**: Display selected notes directly on home screen
- **Quick Add Widget**: Add new notes instantly without opening the app
- Real-time widget updates

### 💾 **Backup & Restore**
- **Local Backup**: Encrypted SQLite export for maximum privacy
- **Cloud Backup**: Google Drive API integration for seamless sync
- Easy restore functionality
- Data migration support across devices

### 🎨 **Modern UI/UX**
- Full Material Design 3 implementation
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
- Dynamic badge counts in navigation drawer
- Profile editing (name, picture)
- Contact support and FAQ section
- Firebase Remote Config for dynamic feature control
- Firebase Crashlytics for real-time crash monitoring
- Semantic versioning for clear release tracking

---

## 📱 Screenshots

<div align="center">

| Dashboard | Notes View | Reminders |
|-----------|------------|-----------|
| ![Dashboard](link-to-screenshot) | ![Notes](link-to-screenshot) | ![Reminders](link-to-screenshot) |

| Dark Mode | Widgets | Export Options |
|-----------|---------|----------------|
| ![Dark Mode](link-to-screenshot) | ![Widgets](link-to-screenshot) | ![Export](link-to-screenshot) |

</div>

---

## 🚀 Installation

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or higher
- Android SDK (API 24+)
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
- `title`
- `content`
- `category`
- `timestamp`
- `isImportant`
- `order`

**Reminders Table**
- `id` (Primary Key)
- `noteId` (Foreign Key)
- `reminderTime`
- `isRepeating`
- `repeatInterval`

**Tasks Table**
- `id` (Primary Key)
- `title`
- `isCompleted`
- `priority`
- `dueDate`

**Wishlist Table**
- `id` (Primary Key)
- `itemName`
- `description`
- `dateAdded`

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
- Database optimization with indexing
- Memory leak fixes
- App size optimization
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
- [ ] Cloud synchronization across devices
- [ ] Collaboration features (shared notes)
- [ ] Voice-to-text note creation
- [ ] Rich text editor with formatting
- [ ] Note templates
- [ ] Advanced analytics dashboard
- [ ] Multi-language support
- [ ] Wear OS companion app
- [ ] Premium subscription features

### Under Consideration
- [ ] Web version (PWA)
- [ ] Desktop application (Windows/Mac/Linux)
- [ ] AI-powered note suggestions
- [ ] Integration with calendar apps
- [ ] Markdown support
- [ ] Note linking and backlinks

---

## 🤝 Contributing

Contributions are welcome! This project follows standard open-source contribution guidelines.

### How to Contribute

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/AmazingFeature
   ```
3. **Commit your changes**
   ```bash
   git commit -m 'Add some AmazingFeature'
   ```
4. **Push to the branch**
   ```bash
   git push origin feature/AmazingFeature
   ```
5. **Open a Pull Request**

### Contribution Guidelines

- Follow existing code style and conventions
- Write clear commit messages
- Add appropriate comments and documentation
- Test your changes thoroughly
- Update README if adding new features
- Ensure all existing tests pass

### Code Style
- Follow Android coding standards
- Use meaningful variable and method names
- Keep methods small and focused
- Add JavaDoc comments for public methods
- Maintain consistent indentation (4 spaces)

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

This project is proprietary software. Unauthorized copying, modification, distribution, or use of this software, via any medium, is strictly prohibited without explicit permission from the author.

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

## 💖 Support

If you find this project helpful, please consider:

- ⭐ Starring the repository
- 🐛 Reporting bugs
- 💡 Suggesting new features
- 📖 Improving documentation
- 🔀 Contributing code

---

<div align="center">

**Made with ❤️ by Thogaruchesti Hemanth**

[⬆ Back to Top](#-notesnest)

</div>
