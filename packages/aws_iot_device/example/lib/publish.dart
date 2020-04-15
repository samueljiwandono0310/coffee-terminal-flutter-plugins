import 'package:flutter/material.dart';
import 'package:aws_iot_device/aws_iot_device.dart';

class Publish extends StatefulWidget {
  @override
  _PublishState createState() => _PublishState();
}

class _PublishState extends State<Publish> {
  AwsIotDevice awsIotDevice = AwsIotDevice();
  String _topic;
  String _message;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: <Widget>[
        SizedBox(width: 16.0),
        Expanded(
          child: TextField(
            onChanged: (topic) => setState(() => _topic = topic.trim()),
            decoration: InputDecoration(
              labelText: 'topic',
              border: OutlineInputBorder(),
            ),
          ),
        ),
        SizedBox(width: 16.0),
        Expanded(
          child: TextField(
            onChanged: (message) => setState(() => _message = message.trim()),
            decoration: InputDecoration(
              labelText: 'message',
              border: OutlineInputBorder(),
            ),
          ),
        ),
        SizedBox(width: 16.0),
        RaisedButton(
          child: Text('publish'),
          onPressed: () => awsIotDevice.publish(_topic, _message),
        ),
        SizedBox(width: 16.0),
      ],
    );
  }
}
