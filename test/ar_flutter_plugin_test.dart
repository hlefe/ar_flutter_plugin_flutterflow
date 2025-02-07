import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ar_flutter_plugin_2/ar_flutter_plugin.dart';

void main() {
  const MethodChannel channel = MethodChannel('ar_flutter_plugin_2');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await ArFlutterPlugin.platformVersion, '42');
  });
}
