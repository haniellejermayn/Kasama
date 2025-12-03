# Kasama 🏠

**A collaborative household management app for roommates and families**

Repository for the final submission in partial completion of the final project in MOBICOM.

## About

Kasama (Tagalog for "together" or "companion") is a mobile application designed to help households and roommates collaborate on daily responsibilities. The app provides a dedicated space for managing shared chores and notes—eliminating the chaos of scattered group chats.

**Target Users**: University students, dormmates, roommates, and families in shared living spaces.

## Features

- **Household Management**: Create or join households using invite codes or QR codes
- **Chore Tracking**: Assign, complete, and track chores with due dates and recurring options (daily, weekly, monthly, yearly)
- **Shared Notes**: Leave reminders, shopping lists, and announcements for all members
- **Push Notifications**: Get alerted for new assignments, approaching deadlines, and new notes
- **Offline-First Architecture**: Create and view chores/notes without internet, syncs automatically when reconnected
- **Real-Time Sync**: See updates from household members instantly across all devices
- **Local Reminders**: Background notifications for due chores, even when app is closed

## Tech Stack

**Architecture**
- MVVM (Model-View-ViewModel) Pattern
- Repository Pattern for data abstraction
- Offline-First Design with automatic conflict resolution

**Android & Kotlin**
- Kotlin Coroutines & Flow for reactive programming
- Android XML Layouts
- RecyclerView with custom adapters
- WorkManager for background sync and notifications

**Local Storage**
- Room Database (SQLite)
- Type Converters for complex data
- DAO (Data Access Object) pattern

**Backend & Cloud Services**
- Firebase Authentication (Email/Password)
- Firebase Firestore (Real-time database with listeners)
- Firebase Cloud Messaging (Push notifications)
- Firebase Storage (Profile pictures)

**Key Technical Features**
- Bi-directional sync between Room and Firestore
- Sync status tracking with `isSynced` flags
- Pending deletion queue for offline operations
- Network-aware background synchronization
- Multi-household support

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│                  UI LAYER                       │
│  (Activities, Adapters, ViewModels)             │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│              REPOSITORY LAYER                   │
│  (ChoreRepository, NoteRepository, etc.)        │
└─────────┬───────────────────────┬───────────────┘
          │                       │
┌─────────▼─────────┐   ┌─────────▼──────────────┐
│   LOCAL SOURCE    │   │   REMOTE SOURCE        │
│   (Room DB)       │◄─►│   (Firebase)           │
│   • Offline-first │   │   • Real-time sync     │
│   • SQLite        │   │   • Cloud backup       │
└───────────────────┘   └────────────────────────┘
```

**Offline-First Sync Flow:**
1. User creates/updates data → Saved to Room immediately (`isSynced = false`)
2. UI updates instantly from Room Flow
3. WorkManager schedules background sync when network available
4. SyncWorker pushes changes to Firestore and marks `isSynced = true`
5. Firebase listeners notify other devices in real-time

## Team

- **Hanielle Chua** - BSCS-ST¹
- **Gabrielle Kelsey** - BSCS-ST²
- **Hephzi Tolentino** - BSCS-ST³

## License

This project is created for academic purposes as part of MOBICOM coursework at De La Salle University.

---

**Built with ❤️ for better household collaboration**
