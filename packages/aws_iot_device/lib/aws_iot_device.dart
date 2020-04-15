import 'dart:async';
import 'package:flutter/services.dart';

const MethodChannel _channel = MethodChannel('bagubagu.com.iot/aws_iot_device');

const EventChannel _pubSubChannel =
    EventChannel('bagubagu.com.iot/aws_iot_device_pubsub');

/// An AWS Iot Device
class AwsIotDevice {
  AwsIotDevice._();

  static final AwsIotDevice _instance = AwsIotDevice._();

  /// Get this singleton instance
  factory AwsIotDevice() {
    return _instance;
  }

  /// Initialize with this device configuration parameters
  Future<void> init(
      {endpoint, region, policyName, thingName, clientId, keystorePath}) async {
    await _channel.invokeMethod('init', {
      'endpoint': endpoint,
      'region': region,
      'policyName': policyName,
      'thingName': thingName,
      'clientId': clientId,
      'keystorePath': keystorePath
    });
  }

  /// Initialize with this device configuration parameters
  Future<void> initAndConnect({
    endpoint,
    thingName,
    region = 'us-east-1',
    certId,
    keystorePath,
    keystoreName,
    keystorePassword,
  }) async {
    await _channel.invokeMethod('initAndConnect', {
      'endpoint': endpoint,
      'region': region,
      'thingName': thingName,
      'certId': certId,
      'keystorePath': keystorePath,
      'keystoreName': keystoreName,
      'keystorePassword': keystorePassword
    });
  }

  /// Save certificate and private key to keystore
  Future<void> saveCertificateAndPrivateKey(
      {certId,
      certPem,
      keyPem,
      keystorePath,
      keystoreName,
      keystorePassword}) async {
    return await _channel.invokeMethod('saveCertificateAndPrivateKey', {
      'certId': certId,
      'certPem': certPem,
      'keyPem': keyPem,
      'keystorePath': keystorePath,
      'keystoreName': keystoreName,
      'keystorePassword': keystorePassword,
    });
  }

  /// Connect to IoT Core
  Future<void> connect() async {
    return await _channel.invokeMethod('connect');
  }

  /// Disconnect from IoT Core
  Future<void> disconnect() async {
    return await _channel.invokeMethod('disconnect');
  }

  /// Connect using MQTT over https
  Future<void> connectUsingALPN() async {
    return await _channel.invokeMethod('connectUsingALPN');
  }

  /// Publish message to topic
  Future<void> publish(topic, message) async {
    return await _channel
        .invokeMethod('publish', {'topic': topic, 'message': message});
  }

  /// Subscribe to a topic
  Future<void> subscribe(topic) async {
    await _channel.invokeMethod('subscribe', {'topic': topic});
  }

  /// Unsubscribe to a topic
  Future<void> unsubscribe(topic) async {
    await _channel.invokeMethod('unsubscribe', {'topic': topic});
  }
}

class AwsIotDeviceEvent {
  Stream<dynamic> _pubSubEvent;

  /// Stream of all messages from all topics
  Stream<dynamic> get pubSubEvent {
    if (_pubSubEvent == null) {
      _pubSubEvent =
          _pubSubChannel.receiveBroadcastStream().map((dynamic event) => event);
    }
    return _pubSubEvent;
  }
}

class AwsIotDeviceConnection {
  static EventChannel _connectionChannel =
      EventChannel('bagubagu.com.iot/aws_iot_device_connection');

  AwsIotDeviceConnection._();

  static final AwsIotDeviceConnection _instance = AwsIotDeviceConnection._();

  /// Get this singleton instance
  factory AwsIotDeviceConnection() {
    return _instance;
  }

  Stream<dynamic> _connectionStatus;

  /// Streams of connection status
  Stream<dynamic> get connectionStatus {
    if (_connectionStatus == null) {
      _connectionStatus =
          _connectionChannel.receiveBroadcastStream().map((value) => value);
    }
    return _connectionStatus;
  }
}

final awsIotDeviceEvent = AwsIotDeviceEvent();
final awsIotDeviceConnection = AwsIotDeviceConnection();
