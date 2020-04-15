import 'package:flutter/material.dart';
import 'package:aws_iot_device/aws_iot_device.dart';

class Subscribe extends StatefulWidget {
  @override
  _SubscribeState createState() => _SubscribeState();
}

class _SubscribeState extends State<Subscribe> {
  final AwsIotDevice awsIotDevice = AwsIotDevice();
  String _topic;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: <Widget>[
        Expanded(
          child: TextField(
            onChanged: (topic) => setState(() => _topic = topic.trim()),
            decoration: InputDecoration(
              labelText: 'topic',
              border: OutlineInputBorder(),
            ),
          ),
        ),
        RaisedButton(
          child: Text('subscribe to topic'),
          onPressed: () => awsIotDevice.subscribe(_topic),
        ),
      ],
    );
  }
}
