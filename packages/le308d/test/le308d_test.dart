import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:le308d/le308d.dart';

void main() {
  const MethodChannel channel = MethodChannel('le308d');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await Le308d.platformVersion, '42');
  });
}
