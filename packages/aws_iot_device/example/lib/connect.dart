import 'package:flutter/material.dart';
import 'package:aws_iot_device/aws_iot_device.dart';

class Connect extends StatelessWidget {
  final AwsIotDevice awsIotDevice = AwsIotDevice();

  @override
  Widget build(BuildContext context) {
    return ButtonBar(
      children: <Widget>[
        _buildConnectButton(),
        _buildDisconnectButton(),
        _buildStatusText(),
      ],
    );
  }

  RaisedButton _buildConnectButton() {
    return RaisedButton(
      child: Text('Connect'),
      onPressed: () => awsIotDevice.connect(),
    );
  }

  RaisedButton _buildDisconnectButton() {
    return RaisedButton(
      child: Text('Disconnect'),
      onPressed: () => awsIotDevice.disconnect(),
    );
  }

  StreamBuilder _buildStatusText() {
    return StreamBuilder(
      stream: awsIotDeviceConnection.connectionStatus,
      initialData: 'unknown',
      builder: (BuildContext context, AsyncSnapshot<dynamic> snapshot) =>
          Text('status: ${snapshot.data}'),
    );
  }
}
