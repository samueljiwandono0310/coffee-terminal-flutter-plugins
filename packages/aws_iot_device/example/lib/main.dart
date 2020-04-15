import 'package:flutter/material.dart';
import 'package:aws_iot_device/aws_iot_device.dart';
import 'package:flutter/services.dart';

import './init.dart';
import './publish.dart';
import './connect.dart';
import './subscribe.dart';

void handlePlatformException(e, s) {
  print('initAwsIot: platform exception: $e');
  // print('platform exception stack trace: $s');
}

void main() {
  AwsIotDevice awsIotDevice = AwsIotDevice();
  awsIotDevice
      .init(
        endpoint: 'a2tnzrtlx0dz0e-ats.iot.us-east-1.amazonaws.com',
        region: 'us-east-1',
        policyName: 'louis-flutter-client-policy',
        thingName: 'louis-flutter-client',
      )
      // .initAndConnect(
      //   endpoint: 'a2tnzrtlx0dz0e-ats.iot.us-east-1.amazonaws.com',
      //   region: 'us-east-1',
      //   policyName: 'louis-flutter-client-policy',
      //   thingName: 'louis-flutter-client',
      // )
      .then((_) {
        runApp(MyApp());
      })
      .catchError(handlePlatformException, test: (e) => e is PlatformException)
      .catchError((e, s) {
        print('initAwsIot: exception catched: ${e.toString()}');
        // print('stack trace: $s');
      });
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  AwsIotDevice awsIotDevice = AwsIotDevice();

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('AwsIotPlugin Example App')),
        body: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Expanded(
              child: Column(
                children: <Widget>[
                  Connect(),
                  Publish(),
                  Subscribe(),
                ],
              ),
            ),
            Expanded(
              child: StreamBuilder(
                stream: awsIotDeviceEvent.pubSubEvent,
                initialData: 'welcome',
                builder:
                    (BuildContext context, AsyncSnapshot<dynamic> snapshot) =>
                        Text(snapshot.data),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
