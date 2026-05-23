import 'dart:async';
import 'dart:convert';
import 'package:rxdart/rxdart.dart';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'package:web_socket_channel/status.dart' as status;
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:injectable/injectable.dart';

import '../constants/api_constants.dart';
import '../error/failures.dart';

enum SocketConnectionState {
  connecting,
  connected,
  disconnected,
  reconnecting,
  error,
}

@singleton
class SocketManager {
  WebSocketChannel? _channel;
  StreamSubscription? _connectionSub;
  Timer? _reconnectTimer;
  Timer? _pingTimer;

  int _reconnectAttempts = 0;
  static const int _maxReconnectAttempts = 10;
  static const Duration _pingInterval = Duration(seconds: 25);
  static const Duration _baseReconnectDelay = Duration(seconds: 2);

  final _messageController = BehaviorSubject<Map<String, dynamic>>();
  final _connectionStateController =
      BehaviorSubject<SocketConnectionState>.seeded(
    SocketConnectionState.disconnected,
  );

  Stream<Map<String, dynamic>> get messages => _messageController.stream;
  Stream<SocketConnectionState> get connectionState =>
      _connectionStateController.stream;
  bool get isConnected =>
      _connectionStateController.value == SocketConnectionState.connected;

  Future<void> connect(String token) async {
    if (isConnected) return;
    _connectionStateController.add(SocketConnectionState.connecting);

    try {
      final uri = Uri.parse(
        '${ApiConstants.wsBaseUrl}?token=$token',
      );
      _channel = WebSocketChannel.connect(uri);
      await _channel!.ready;

      _connectionStateController.add(SocketConnectionState.connected);
      _reconnectAttempts = 0;
      _startPingTimer();
      _listenToMessages();
      _listenToConnectivity();
    } catch (e) {
      _connectionStateController.add(SocketConnectionState.error);
      _scheduleReconnect(token);
    }
  }

  void _listenToMessages() {
    _channel?.stream.listen(
      (data) {
        try {
          final decoded = jsonDecode(data as String) as Map<String, dynamic>;
          _messageController.add(decoded);
        } catch (e) {
          // Ignore malformed messages
        }
      },
      onError: (error) {
        _connectionStateController.add(SocketConnectionState.error);
      },
      onDone: () {
        if (_connectionStateController.value == SocketConnectionState.connected) {
          _connectionStateController.add(SocketConnectionState.disconnected);
        }
      },
    );
  }

  void _listenToConnectivity() {
    _connectionSub = Connectivity().onConnectivityChanged.listen((result) {
      if (result == ConnectivityResult.none) {
        _connectionStateController.add(SocketConnectionState.disconnected);
      }
    });
  }

  void _startPingTimer() {
    _pingTimer?.cancel();
    _pingTimer = Timer.periodic(_pingInterval, (_) {
      if (isConnected) {
        send({'type': 'ping', 'timestamp': DateTime.now().millisecondsSinceEpoch});
      }
    });
  }

  void _scheduleReconnect(String token) {
    if (_reconnectAttempts >= _maxReconnectAttempts) {
      _connectionStateController.add(SocketConnectionState.error);
      return;
    }
    _reconnectAttempts++;
    _connectionStateController.add(SocketConnectionState.reconnecting);

    // Exponential backoff
    final delay = Duration(
      seconds: _baseReconnectDelay.inSeconds * _reconnectAttempts,
    );
    _reconnectTimer?.cancel();
    _reconnectTimer = Timer(delay, () => connect(token));
  }

  void send(Map<String, dynamic> message) {
    if (!isConnected) {
      throw const SocketFailure(message: 'Not connected to socket server');
    }
    _channel?.sink.add(jsonEncode(message));
  }

  void sendMessage({
    required String conversationId,
    required String content,
    required String messageType,
    String? replyToId,
  }) {
    send({
      'type': 'send_message',
      'conversation_id': conversationId,
      'content': content,
      'message_type': messageType,
      if (replyToId != null) 'reply_to_id': replyToId,
      'timestamp': DateTime.now().millisecondsSinceEpoch,
    });
  }

  void sendTypingIndicator({
    required String conversationId,
    required bool isTyping,
  }) {
    send({
      'type': isTyping ? 'typing_start' : 'typing_stop',
      'conversation_id': conversationId,
    });
  }

  void markMessageRead({
    required String conversationId,
    required String messageId,
  }) {
    send({
      'type': 'mark_read',
      'conversation_id': conversationId,
      'message_id': messageId,
    });
  }

  Future<void> disconnect() async {
    _pingTimer?.cancel();
    _reconnectTimer?.cancel();
    _connectionSub?.cancel();
    await _channel?.sink.close(status.goingAway);
    _channel = null;
    _connectionStateController.add(SocketConnectionState.disconnected);
  }

  void dispose() {
    disconnect();
    _messageController.close();
    _connectionStateController.close();
  }
}
