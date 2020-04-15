import 'dart:convert';
import 'dart:io' as io;
import 'package:path_provider/path_provider.dart';
import 'package:http/http.dart' as http;
import 'package:aws_iot_device/aws_iot_device.dart';

final String defaultEndpoint = 'a2tnzrtlx0dz0e-ats.iot.us-east-1.amazonaws.com';
final String defaultApiEndpoint =
    'https://03crnudn2a.execute-api.us-east-1.amazonaws.com/Prod';
final String defaultThingName = 'foo6';
final String defaultCertId = 'default';
final String defaultKeystoreName = 'iot_keystore';
final String defaultKeystorePassword = 'password';
final String defaultRegion = 'us-east-1';

class ProvisionThingResponse {
  String endpoint;
  String certificate;
  String privateKey;
  String publicKey;
  String error;

  ProvisionThingResponse({
    this.endpoint,
    this.certificate,
    this.privateKey,
    this.publicKey,
    this.error,
  });

  factory ProvisionThingResponse.fromJson(Map<String, dynamic> json) {
    return ProvisionThingResponse(
      error: json['error'],
      endpoint: json['endpoint'],
      certificate: json['certificate'],
      privateKey: json['privateKey'],
      publicKey: json['publicKey'],
    );
  }
}

Future<ProvisionThingResponse> provisionThing({
  String apiEndpoint,
  String thingName,
}) async {
  var response = await http.post(
    '$apiEndpoint/provision-thing2',
    body: json.encode({'thingName': defaultThingName}),
  );
  print('Response status: ${response.statusCode}');
  print('Response body: ${response.body}');

  return ProvisionThingResponse.fromJson(json.decode(response.body));
}

Future<void> initAwsIot() async {
  AwsIotDevice awsIotDevice = AwsIotDevice();

  final directory = await getApplicationDocumentsDirectory();
  final String certificateFilePath = '${directory.path}/certificate';
  final String privateKeyFilePath = '${directory.path}/privateKey';
  final String keystoreFilePath = '${directory.path}/$defaultKeystoreName';

  print("certificateFilePath: $certificateFilePath");
  print("privateKeyFilePath: $privateKeyFilePath");
  print("keystoreFilePath: $keystoreFilePath");

  if (io.File(certificateFilePath).existsSync() &&
      io.File(privateKeyFilePath).existsSync() &&
      io.File(keystoreFilePath).existsSync()) {
    // do nothing
  } else {
    ProvisionThingResponse provisionThingResponse = await provisionThing(
      apiEndpoint: defaultApiEndpoint,
      thingName: defaultThingName,
    );

    if (provisionThingResponse.error == null ||
        provisionThingResponse.error.isEmpty) {
      if (!io.File(certificateFilePath).existsSync()) {
        final file = await io.File(certificateFilePath).create(recursive: true);
        file.writeAsStringSync(provisionThingResponse.certificate);
        print(
            'file ${file.path} created with content ${provisionThingResponse.certificate}');
      }

      if (!io.File(privateKeyFilePath).existsSync()) {
        final file = await io.File(privateKeyFilePath).create(recursive: true);
        file.writeAsStringSync(provisionThingResponse.privateKey);
        print(
            'file ${file.path} created with content ${provisionThingResponse.certificate}');
      }

      await awsIotDevice.saveCertificateAndPrivateKey(
        certId: defaultCertId,
        certPem: provisionThingResponse.certificate,
        keyPem: provisionThingResponse.privateKey,
        keystoreName: defaultKeystoreName,
        keystorePassword: defaultKeystorePassword,
        keystorePath: directory.path,
      );
    } else {
      print('provisionThing error: ${provisionThingResponse.error}');
    }
  }

  // awsInit
  // must have rootCA yang CA-1

  // await awsIotDevice.initAndConnect(
  //   thingName: defaultThingName,
  //   region: defaultRegion,
  //   certId: defaultCertId,
  //   endpoint: defaultEndpoint,
  //   keystoreName: defaultKeystoreName,
  //   keystorePassword: defaultKeystorePassword,
  //   keystorePath: directory.path,
  // );

  // handler
  // awsIotDevice.pubSubEvent.listen((data) {
  //   print('data: $data');
  // });

  // subscribe to events
  var thingName = defaultThingName;
  // awsIotDevice.subscribe('\$aws/things/$thingName/shadow/update/accepted');
  // awsIotDevice.subscribe('\$aws/things/$thingName/shadow/update/rejected');
  // awsIotDevice.subscribe('\$aws/things/$thingName/shadow/update/documents');
  // awsIotDevice.subscribe('\$aws/things/$thingName/shadow/update/delta');
  // awsIotDevice.subscribe('\$aws/things/$thingName/shadow/get/accepted');
  // awsIotDevice.subscribe('\$aws/things/$thingName/shadow/get/rejected');
  // awsIotDevice.subscribe('\$aws/things/$thingName/shadow/delete/accepted');
  // awsIotDevice.subscribe('\$aws/things/$thingName/shadow/delete/rejected');
  // awsIotDevice.subscribe('\$aws/things/$thingName/jobs/notify');
  // awsIotDevice.subscribe('\$aws/things/$thingName/jobs/accepted');
  // awsIotDevice.subscribe('\$aws/things/$thingName/jobs/rejected');

  // initial get
  // awsIotDevice.publish('\$aws/things/$thingName/shadow/get', '');
}
