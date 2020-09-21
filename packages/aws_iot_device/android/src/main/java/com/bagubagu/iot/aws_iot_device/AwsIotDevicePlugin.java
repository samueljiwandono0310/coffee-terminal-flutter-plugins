package com.bagubagu.iot.aws_iot_device;

import android.util.Log;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.regions.Region;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPolicyRequest;
import com.amazonaws.services.iot.model.AttachThingPrincipalRequest;
import com.amazonaws.services.iot.model.AttachThingPrincipalResult;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.KeyStore;

import static com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper.getIotKeystore;
import static com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper.saveCertificateAndPrivateKey;

/**
 * AwsIotDevicePlugin
 */
public class AwsIotDevicePlugin implements MethodCallHandler {
  private static final String TAG = "awsIotDevicePlugin";

  private static final String keystoreName = "iot_keystore";
  private static final String keystorePassword = "password";
  private static final String certificateId = "default";

  private static KeyStore clientKeyStore = null;

  private static AWSIotMqttManager mqttManager;
  private static EventChannel.EventSink pubSubEventSink;
  private static EventChannel.EventSink connectionStatus;

  private final Registrar registrar;

  private AwsIotDevicePlugin(Registrar registrar) {
    this.registrar = registrar;
  }

  /**
   * Plugin registration.
   **/
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(),
        "bagubagu.com.iot/aws_iot_device");

    channel.setMethodCallHandler(new AwsIotDevicePlugin(registrar));

   final EventChannel pubSubChannel = new EventChannel(registrar.messenger(),
       "bagubagu.com.iot/aws_iot_device_pubsub");

   pubSubChannel.setStreamHandler(new EventChannel.StreamHandler() {
     @Override
     public void onListen(Object o, EventChannel.EventSink eventSink) {
       pubSubEventSink = eventSink;
     }

     @Override
     public void onCancel(Object o) {
       pubSubEventSink = null;
     }
   });

    final EventChannel connectionChannel = new EventChannel(registrar.messenger(),
        "bagubagu.com.iot/aws_iot_device_connection");

    connectionChannel.setStreamHandler(new EventChannel.StreamHandler() {
      @Override
      public void onListen(Object o, EventChannel.EventSink eventSink) {
        connectionStatus = eventSink;
      }

      @Override
      public void onCancel(Object o) {
        connectionStatus = null;
      }
    });
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    String jsonString = new Gson().toJson(call);

    // Log.d(TAG, "onMethodCall() call: " + jsonString);

    switch (call.method) {
      case "initAndConnect":
        initAndConnect(call, result);
        break;

      case "init":
        init(call, result);
        break;

      case "connect":
        connect(call, result);
        break;

      case "connectUsingALPN":
        connectUsingALPN(call, result);
        break;

      case "publish":
        publish(call, result);
        break;

      case "subscribe":
        subscribe(call, result);
        break;

      case "unsubscribe":
        unsubscribe(call, result);
        break;

      case "disconnect":
        disconnect(call, result);
        break;

      case "saveCertificateAndPrivateKey":
        doSaveCertificateAndPrivateKey(call, result);
        break;

      default:
        result.notImplemented();
    }
  }

  private void doSaveCertificateAndPrivateKey(MethodCall call, Result result) {
    final String certId = call.argument("certId");
    final String certPem = call.argument("certPem");
    final String keyPem = call.argument("keyPem");
    final String keystorePath = call.argument("keystorePath");
    final String keystoreName = call.argument("keystoreName");
    final String keystorePassword = call.argument("keystorePassword");

    // Log.d(TAG, "doSaveCertificateAndPrivateKey: " + "keyPem: " + keyPem + " keystorePath: " + keystorePath +
        // " keystoreName: " + keystoreName + " keystorePassword: " + keystorePassword);

    if (certId == null || certPem == null || keyPem == null || keystorePath == null || keystoreName == null || keystorePassword == null) {
      result.error("", "saveCertificateAndPrivateKey unsuccessful: please supply all required parameters", null);
    }

    try {
      // Log.d(TAG, "new certificate will be created");
      saveCertificateAndPrivateKey(certId, certPem, keyPem, keystorePath, keystoreName, keystorePassword);
      
      clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId, keystorePath, keystoreName, keystorePassword);
    } catch (Exception e) {
      // Log.d(TAG, "error on saving new certificate files" + e.toString());
      result.error(e.getLocalizedMessage(), e.toString(), null);
    }

    result.success(null);
  }

  // No longer used
  private void initAndConnect(MethodCall call, Result result) {
    final String region = call.argument("region");
    final String endpoint = call.argument("endpoint");
    final String thingName = call.argument("thingName");
    final String certId = call.argument("certId");
    final String keystorePath = call.argument("keystorePath");
    final String keystoreName = call.argument("keystoreName");
    final String keystorePassword = call.argument("keystorePassword");

    // Log.d(TAG, "initAndConnect: " + "region: " + region + " endpoint: " + endpoint +
        // " thingName: " + thingName + " certId: " + certId + " keystorePath: " + keystorePath +
        // " keystoreName: " + keystoreName + " keystorePassword: " + keystorePassword);

    if (region == null || thingName == null || endpoint == null || certId == null
        || keystorePath == null || keystoreName == null || keystorePassword == null) {
      result.error("", "init2 unsuccessful: please supply all required parameters", null);
    }

    clientKeyStore = getIotKeystore(certId, keystorePath, keystoreName, keystorePassword);

    try {
      initMqttManager(thingName, endpoint);
      mqttManager.disconnect();
      mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
        @Override
        public void onStatusChanged(AWSIotMqttClientStatus status, Throwable throwable) {
          // Log.d(TAG, "connect.onStatusChanged: " + status);

          if (status != null && connectionStatus != null) {
            // Log.d(TAG, "status on init & connect: " + status);
//             connectionStatus.success(status.toString());
          } else {
            // Log.d(TAG, "connect.onStatusChanged: either status or connectionStatus is null");
          }

          if (throwable != null) {
            // Log.d(TAG, "connect.onStatusChanged throwable: ", throwable);
          }
        }
      });
    } catch (Exception e) {
      result.error(e.getLocalizedMessage(), e.toString(), null);
    }

    result.success(null);
  }

  private void init(MethodCall call, Result result) {
    final String region = call.argument("region");
    final String endpoint = call.argument("endpoint");
    final String thingName = call.argument("thingName");
    final String clientId = call.argument("clientId");
    final String policyName = call.argument("policyName");
    final String keystorePath = call.argument("keystorePath");

     Log.d(TAG, "init: " + "region: " + region + " endpoint: " + endpoint +
         " thingName: " + thingName + " policyName: " + policyName + " clientId: " + clientId);

    if (region == null || policyName == null || thingName == null || endpoint == null) {
      result.error("", "init unsuccessful: please supply all required parameters", null);
    }

    try {
      initIoTClient(keystorePath);
      initMqttManager(clientId, endpoint);

    } catch (Exception e) {
      result.error(e.getLocalizedMessage(), e.toString(), null);
    }

    result.success(null);
  }

  private void connect(MethodCall call, Result result) {
    try {
       Log.d(TAG, "client key store: " + clientKeyStore);
      mqttManager.disconnect();
      mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {


        @Override
        public void onStatusChanged(AWSIotMqttClientStatus status, Throwable throwable) {
          Log.d(TAG, "connect.onStatusChanged: " + status.name());

          if (status != null && connectionStatus != null) {
            Log.d(TAG, "status on connect: " + status.name());
            onConnectionStatusArrived osa = new onConnectionStatusArrived(status.name());
            ThreadUtils.runOnUiThread(osa);
//            connectionStatus.success(status.toString());
          } else {
             Log.d(TAG, "connect.onStatusChanged: either status or connectionStatus is null");
          }

          if (throwable != null) {
             Log.d(TAG, "connect.onStatusChanged throwable: ", throwable);
          }
        }
      });
    } catch (Exception e) {
       Log.d(TAG, "error on connect" + e.toString());
    }
    result.success(null);
  }

  private void connectUsingALPN(MethodCall call, Result result) {

    mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
      @Override
      public void onStatusChanged(AWSIotMqttClientStatus status, Throwable throwable) {
        // Log.d(TAG, "connectUsingALPN.onStatusChanged: " + status);

        if (status != null && connectionStatus != null) {
          // connectionStatus.success(String.valueOf(status));
        } else {
          // Log.d(TAG, "connect.onStatusChanged: either status or connectionStatus is null");
        }

        if (throwable != null) {
          // Log.d(TAG, "connect.onStatusChanged throwable: ", throwable);
        }
      }
    });

    result.success(null);
  }

  private void publish(MethodCall call, Result result) {
    final String topic = call.argument("topic");
    final String message = call.argument("message");
    final String qosValue = call.argument("qos");

    AWSIotMqttQos qos;
    if (qosValue == null) {
      qos = AWSIotMqttQos.QOS0;
    } else {
      qos = qosValue.equals("1") ? AWSIotMqttQos.QOS1 : AWSIotMqttQos.QOS0;
    }

    // Log.d(TAG, "publish: message: " + message + " topic: " + topic + " qos: " + qos);

    try {
      mqttManager.publishString(message, topic, qos);
    } catch (Exception e) {
      result.error(e.getMessage(), e.toString(), null);
    }

    result.success(null);
  }

  private void subscribe(MethodCall call, Result result) {
    final String topic = call.argument("topic");
    final String qosValue = call.argument("qos");

    AWSIotMqttQos qos;
    if (qosValue == null) {
      qos = AWSIotMqttQos.QOS0;
    } else {
      qos = qosValue.equals("1") ? AWSIotMqttQos.QOS1 : AWSIotMqttQos.QOS0;
    }

    try {
      // Log.d(TAG, "subscribe: subscribing to topic: " + topic + " with qos: " + qos);

      mqttManager.subscribeToTopic(topic, qos, new AWSIotMqttNewMessageCallback() {
        @Override
        public void onMessageArrived(final String topic, final byte[] data) {

          OnMessageArrived oma = new OnMessageArrived(topic, data);
          ThreadUtils.runOnUiThread(oma);
        }});
    } catch (Exception e) {
      Log.e(TAG, "subscribe: ", e);
      pubSubEventSink.error(e.getMessage(), e.toString(), null);
    }
    result.success(null);
  }

  private void unsubscribe(MethodCall call, Result result) {
    final String topic = call.argument("topic");

    try {
      // Log.d(TAG, "unsubscribe: unsubscribing to topic: " + topic);

      mqttManager.unsubscribeTopic(topic);
    } catch (Exception e) {
      Log.e(TAG, "unsubscribe: ", e);
    }
    result.success(null);
  }

  private void disconnect(MethodCall call, Result result) {
    // Log.d(TAG, "disconnect");

    try {
      mqttManager.disconnect();
    } catch (Exception e) {
      result.error(e.getMessage(), e.toString(), null);
    }

    result.success(null);
  }

  private void initMqttManager(String mqttClientId, String endpoint) {
    Log.d(TAG, "initMqttManager: mqttClientId: " + mqttClientId + " endpoint: " + endpoint);

    mqttManager = new AWSIotMqttManager(mqttClientId, endpoint);
    mqttManager.setMaxAutoReconnectAttempts(-1);
    mqttManager.setKeepAlive(50);

    AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
        "Android client lost connection", AWSIotMqttQos.QOS0);
    mqttManager.setMqttLastWillAndTestament(lwt);
  }

  // No longer used
  private void setupNewCertificate(final String region, final String policyName, final String thingName) {
    Log.i(TAG, "setupNewCertificate: creating new key and certificate.");

    final String keystorePath = registrar.context().getFilesDir().getPath();
    final AWSMobileClient awsMobileClient = AWSMobileClient.getInstance();

    final AWSIotClient awsIotClient = new AWSIotClient(awsMobileClient);
    awsIotClient.setRegion(Region.getRegion(region));

    final String jsonString = "{\n" +
        "  \"Version\": \"1.0\",\n" +
        "  \"IdentityManager\": {\n" +
        "    \"Default\": {}\n" +
        "  },\n" +
        "  \"CredentialsProvider\": {\n" +
        "    \"CognitoIdentity\": {\n" +
        "      \"Default\": {\n" +
        "        \"PoolId\": \"us-east-1:628f665d-33a4-42c6-bd25-f9c18c702311\",\n" +
        "        \"Region\": \"us-east-1\"\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}";

    try {
      JSONObject jsonObject = new JSONObject(jsonString);
      AWSConfiguration awsConfiguration = new AWSConfiguration(jsonObject);

      awsMobileClient.initialize(registrar.context(), awsConfiguration, new Callback<UserStateDetails>() {
        @Override
        public void onResult(UserStateDetails userStateDetails) {
          // Log.d(TAG, "setupNewCert() initialize onResult Details" + userStateDetails.getDetails());

          try {
            // Create a new private key and certificate. This call
            // creates both on the server and returns them to the device.
            CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                new CreateKeysAndCertificateRequest();
            createKeysAndCertificateRequest.setSetAsActive(true);

            final CreateKeysAndCertificateResult createKeysAndCertificateResult;
            createKeysAndCertificateResult =
                awsIotClient.createKeysAndCertificate(createKeysAndCertificateRequest);

            String bar = awsMobileClient.getIdentityId();
           Log.i(TAG, "Cert ID: " + createKeysAndCertificateResult.getCertificateId() + " created.");
           Log.i(TAG, "Cert Arn: " + createKeysAndCertificateResult.getCertificateArn());
           Log.i(TAG, "AWSMobile instance: " + bar);

            // store in keystore for use in MQTT client
            // saved as alias "default" so a new certificate isn't
            // generated each run of this application
            saveCertificateAndPrivateKey(certificateId,
                createKeysAndCertificateResult.getCertificatePem(),
                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                keystorePath, keystoreName, keystorePassword);

            // load keystore from file into memory to pass on connection
            clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                keystorePath, keystoreName, keystorePassword);

            // Attach a policy to the newly created certificate.
            // This flow assumes the policy was already created in
            // AWS IoT and we are now just attaching it to the certificate.
            AttachPolicyRequest policyAttachReq = new AttachPolicyRequest();
            policyAttachReq.setPolicyName(policyName);
            policyAttachReq.setTarget(createKeysAndCertificateResult.getCertificateArn());
            awsIotClient.attachPolicy(policyAttachReq);
           Log.i(TAG, "setupNewCert() attachPolicy iotClient " + policyName);

            // Atach certificateId to thingName
            AttachThingPrincipalRequest attachThingPrincipalRequest = new AttachThingPrincipalRequest();
            attachThingPrincipalRequest.setPrincipal(createKeysAndCertificateResult.getCertificateArn());
            attachThingPrincipalRequest.setThingName(thingName);
            AttachThingPrincipalResult attachThingPrincipalResult =
                awsIotClient.attachThingPrincipal(attachThingPrincipalRequest);
           Log.i(TAG, "setupNewCert() AttachThingPrincipal result: " + attachThingPrincipalResult);

          } catch (Exception e) {
            Log.e(TAG, "Exception occurred when generating new private key and certificate.", e);
          }
        }

        @Override
        public void onError(Exception e) {
          Log.e(TAG, "setupNewCert() AWSMobileClient initialize onError: ", e);
        }
      });

    } catch (JSONException e) {
      Log.e(TAG, "Error in constructing AWSConfiguration.");
    }
  }

  private void initIoTClient(String keystorePath) {
  // Log.d(TAG, "initIotClient: region: " + region + " policyName: " + policyName + " thingName " + thingName);
  Log.d(TAG, "initIotClient: keystorePath: " + keystorePath);

    try {
      if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
        if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
            keystoreName, keystorePassword)) {
         Log.i(TAG, "Certificate " + certificateId + " found in keystore - using for MQTT.");
          clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
              keystorePath, keystoreName, keystorePassword);
        } else {
         Log.i(TAG, "Key/cert " + certificateId + " not found in keystore.");
        }
      } else {
       Log.i(TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
      }
    } catch (Exception e) {
      Log.e(TAG, "An error occurred retrieving cert/key from keystore.", e);
    }

    // if (clientKeyStore == null) {
    //   Log.i(TAG, "Cert/key was not found in keystore - creating new key and certificate.");
    //   setupNewCertificate(region, policyName, thingName);
    // }
  }

  class OnMessageArrived implements Runnable {
    String topic;
    byte[] data;
    String TAG = "awsIotDevicePlugin-OnMessageArrived";

    public OnMessageArrived(String topic, byte[] data) {
      this.topic = topic;
      this.data = data;
    }

    @Override
    public void run() {
          try {
            String message = new String(data, "UTF-8");
           // Log.d(TAG, "onMessageArrived: topic: " + topic + " message: " + message);

            if (pubSubEventSink != null) {
              pubSubEventSink.success("{ \"topic\": \"" + topic + "\", \"message\": " + message + "}");
            }
          } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "onMessageArrived: Message encoding error." + e);
          } catch (Exception e) {
            Log.e(TAG, "onMessageArrived error: " + e);
          }
    }
  }

  class onConnectionStatusArrived implements Runnable {
    String status;
    String TAG = "onConnectionStatusArrived-OnMessageArrived";

    public onConnectionStatusArrived(String status) {
      this.status = status;
    }

    @Override
    public void run() {
      try {
        if (connectionStatus != null) {
          connectionStatus.success(status);
        }
      } catch (Exception e) {
        Log.e(TAG, "onConnectionStatusArrived error: " + e);
      }
    }
  }
}
