import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:firebase_core/firebase_core.dart';

import 'core/di/injection.dart';
import 'core/router/app_router.dart';
import 'core/theme/app_theme.dart';
import 'core/bloc_observer/app_bloc_observer.dart';
import 'features/chat/data/models/hive/chat_message_hive.dart';
import 'features/chat/data/models/hive/conversation_hive.dart';
import 'firebase_options.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize Firebase
  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  );

  // Initialize Hive local database
  await Hive.initFlutter();
  Hive.registerAdapter(ChatMessageHiveAdapter());
  Hive.registerAdapter(ConversationHiveAdapter());
  await Hive.openBox<ChatMessageHive>('messages');
  await Hive.openBox<ConversationHive>('conversations');

  // Setup dependency injection
  await configureDependencies();

  // Set BLoC observer for global event monitoring
  Bloc.observer = AppBlocObserver();

  // Lock orientation to portrait
  await SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);

  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
    ),
  );

  runApp(const EnterpriseChatApp());
}

class EnterpriseChatApp extends StatelessWidget {
  const EnterpriseChatApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'Enterprise Chat',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.darkTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: ThemeMode.dark,
      routerConfig: AppRouter.router,
    );
  }
}
