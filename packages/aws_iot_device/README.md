# AWS IoT Device Plugin for Flutter

Supports android only.

## Usage

Init, connect, then publish and subscribe as you please.

See [example](https://github.com/bagubagu/flutter_plugins/blob/master/packages/aws_iot_device/example/lib)

### Initialize

```dart
const params = {
  endpoint: 'xxxxxxxxxxxxx-ats.iot.us-east-1.amazonaws.com',
  region: 'us-east-1',
  policyName: 'xxx-client-policy',
  thingName: 'xxx-thing-name',
}

AwsIotDevice.getInstance().init(params);
```

### Connect, disconnect, and connection status

See `example/lib/connect.dart`

```dart
AwsIotDevice.getInstance().connect(),
AwsIotDevice.getInstance().disconnect(),
```

```dart
return StreamBuilder(
  stream: AwsIotDevice.getInstance().connectionStatus,
    initialData: 'no data',
    builder: (BuildContext context, AsyncSnapshot<dynamic> snapshot) =>
      Text('status: ${snapshot.data}'),
  );
}
```

### Subscribe and Publish

Subscribe to topic

```dart
AwsIotDevice.getInstance().subscribe("sample-topic"),
```

Send message to topic

```dart
AwsIotDevice.getInstance().publish("sample-topic", "hellooo world!"),
```
