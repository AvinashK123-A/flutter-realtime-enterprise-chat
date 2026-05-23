# Real-Time Enterprise Messaging Platform — Setup Guide

## Prerequisites
- Flutter SDK 3.16.0+
- Dart SDK 3.2.0+
- Android Studio / VS Code with Flutter plugin
- Xcode 15+ (for iOS)
- Node.js 18+ (for Firebase CLI and backend)
- WebSocket server (configurable via .env)

## Quick Start

### 1. Clone Repository
```bash
git clone https://github.com/AvinashK123-A/flutter-realtime-enterprise-chat.git
cd flutter-realtime-enterprise-chat
```

### 2. Firebase Setup
```bash
npm install -g firebase-tools
firebase login
flutterfire configure
```

### 3. Environment Setup
```bash
cp .env.example .env
# Configure:
# SOCKET_URL — WebSocket server URL
# API_BASE_URL — REST API base URL
# ENCRYPTION_KEY — End-to-end encryption key
```

### 4. Install Dependencies
```bash
flutter pub get
dart run build_runner build --delete-conflicting-outputs
```

## WebSocket Server Configuration
The app connects to a WebSocket server for real-time messaging.
Default: `wss://ws.yourserver.com`

For development, you can use:
```bash
# Local WebSocket server (Node.js)
npx ws --port 8080
```

Or use any of these free public WebSocket services for testing:
- wss://echo.websocket.events
- wss://ws.postman-echo.com/raw

## Message Encryption
End-to-end encryption uses AES-256-GCM.
Keys are exchanged via Diffie-Hellman protocol on first connection.
Configure the base encryption key in .env:
```
ENCRYPTION_KEY=your_32_char_hex_key_here
```

## Push Notifications (FCM)
1. Enable Cloud Messaging in Firebase Console
2. iOS: Upload APNs key in Firebase Console → Project Settings → Cloud Messaging
3. Android: google-services.json auto-configured by flutterfire

## Run Configurations
```bash
# Development
flutter run --flavor dev -t lib/main_dev.dart

# Production
flutter run --flavor prod -t lib/main_prod.dart
```

## Project Structure
```
lib/
├── core/
│   ├── di/             # Dependency injection (GetIt)
│   ├── network/        # Dio HTTP client
│   ├── socket/         # Socket.IO manager
│   ├── router/         # GoRouter navigation
│   ├── theme/          # Material 3 theming
│   ├── encryption/     # E2E encryption layer
│   └── widgets/        # Reusable widgets
├── features/
│   ├── auth/           # Authentication
│   ├── chat/           # Chat rooms & messages
│   ├── contacts/       # Contact management
│   ├── calls/          # Voice/video calls (WebRTC)
│   ├── groups/         # Group management
│   └── settings/       # User preferences
└── main.dart
```

## Architecture
- **Pattern**: Clean Architecture + Feature-first
- **State Management**: BLoC
- **DI**: GetIt + Injectable
- **Navigation**: GoRouter
- **Real-time**: Socket.IO
- **HTTP**: Dio with interceptors
- **Storage**: Hive + flutter_secure_storage
- **Encryption**: AES-256-GCM (E2E)

## Testing
```bash
flutter test test/unit/
flutter test integration_test/
```

## Features
- Real-time 1:1 and group messaging
- End-to-end encryption
- File/image sharing
- Voice messages
- Read receipts & typing indicators
- Message reactions
- Pinned messages
- Push notifications (FCM)
- Offline message sync
- Message search
- User presence (online/offline/away)
