package com.pkmnapps.nearby_connections;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import android.util.Log;

/**
 * NearbyConnectionsPlugin
 */
public class NearbyConnectionsPlugin implements MethodCallHandler {
    private Activity activity;
    private static final String SERVICE_ID = "com.pkmnapps.nearby_connections";
    private static MethodChannel channel;

    private NearbyConnectionsPlugin(Activity activity) {
        this.activity = activity;
    }

    /**
     * Plugin registration.
     */

    public static void registerWith(Registrar registrar) {
        channel = new MethodChannel(registrar.messenger(), "nearby_connections");
        channel.setMethodCallHandler(new NearbyConnectionsPlugin(registrar.activity()));
    }

    @Override
    public void onMethodCall(MethodCall call, final Result result) {

        switch (call.method) {
            case "checkPermissions":
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    result.success(false);
                } else {
                    result.success(true);
                }
                break;
            case "askPermissions":
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
                result.success(null);
                break;
            case "stopAdvertising":
                Log.d("NearbyCon java", "stopAdvertising");
                Nearby.getConnectionsClient(activity).stopAdvertising();
                result.success(null);
                break;
            case "stopDiscovery":
                Log.d("NearbyCon java", "stopDiscovery");
                Nearby.getConnectionsClient(activity).stopDiscovery();
                result.success(null);
                break;
            case "startAdvertising": {
                String userNickName = (String) call.argument("userNickName");
                int strategy = (int) call.argument("strategy");

                assert userNickName != null;
                AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder()
                        .setStrategy(getStrategy(strategy)).build();

                Nearby.getConnectionsClient(activity)
                        .startAdvertising(
                                userNickName, SERVICE_ID, advertConnectionLifecycleCallback, advertisingOptions)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("NearbyCon java", "startAdvertising");
                                result.success(true);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                result.error("Failure", e.getMessage(), null);
                            }
                        });
                break;
            }
            case "startDiscovery": {
                String userNickName = (String) call.argument("userNickName");
                int strategy = (int) call.argument("strategy");

                assert userNickName != null;
                DiscoveryOptions discoveryOptions =
                        new DiscoveryOptions.Builder().setStrategy(getStrategy(strategy)).build();
                Nearby.getConnectionsClient(activity)
                        .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("NearbyCon java", "startDiscovery");
                                result.success(true);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                result.error("Failure", e.getMessage(), null);
                            }
                        });
                break;
            }
            case "stopAllEndpoints":
                Log.d("NearbyCon java", "stopAllEndpoints");
                Nearby.getConnectionsClient(activity).stopAllEndpoints();
                result.success(null);
                break;
            case "disconnectFromEndpoint": {
                Log.d("NearbyCon java", "disconnectFromEndpoint");
                String endpointId = call.argument("endpointId");
                assert endpointId != null;
                Nearby.getConnectionsClient(activity).disconnectFromEndpoint(endpointId);
                result.success(null);
                break;
            }
            case "requestConnection": {
                Log.d("NearbyCon java", "requestConnection");
                String userNickName = (String) call.argument("userNickName");
                String endpointId = (String) call.argument("endpointId");

                assert userNickName != null;
                assert endpointId != null;
                Nearby.getConnectionsClient(activity)
                        .requestConnection(userNickName, endpointId, discoverConnectionLifecycleCallback)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                result.success(true);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                result.error("Failure", e.getMessage(), null);
                            }
                        });
                break;
            }
            case "acceptConnection": {
                String endpointId = (String) call.argument("endpointId");

                assert endpointId != null;
                Nearby.getConnectionsClient(activity)
                        .acceptConnection(endpointId, payloadCallback)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                result.success(true);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                result.error("Failure", e.getMessage(), null);
                            }
                        });
                break;
            }
            case "rejectConnection": {
                String endpointId = (String) call.argument("endpointId");

                assert endpointId != null;
                Nearby.getConnectionsClient(activity)
                        .rejectConnection(endpointId)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                result.success(true);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                result.error("Failure", e.getMessage(), null);
                            }
                        });
                break;
            }
            case "sendPayload": {
                String endpointId = (String) call.argument("endpointId");
                byte[] bytes = call.argument("bytes");

                assert endpointId != null;
                assert bytes != null;
                Nearby.getConnectionsClient(activity).sendPayload(endpointId, Payload.fromBytes(bytes));
                Log.d("NearbyCon java", "sentPayload");
                result.success(true);
                break;
            }
            case "sendFilePayload": {
                String endpointId = (String) call.argument("endpointId");
                String filePath = (String) call.argument("filePath");

                assert endpointId != null;
                assert filePath != null;

                try {
                    File file = new File(filePath);

                    Payload filePayload = Payload.fromFile(file);
                    Nearby.getConnectionsClient(activity).sendPayload(endpointId, filePayload);
                    Log.d("NearbyCon java", "sentFilePayload");
                    result.success(filePayload.getId()); //return payload id to dart
                } catch (FileNotFoundException e) {
                    Log.e("NearbyCon java", "File not found", e);
                    result.error("Failure", "File Not found", null);
                    return;
                }
                break;
            }
            default:
                result.notImplemented();
        }
    }

    private final ConnectionLifecycleCallback advertConnectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
            Log.d("NearbyCon java", "ad.onConnectionInitiated");
            Map<String, Object> args = new HashMap<>();
            args.put("endpointId", endpointId);
            args.put("endpointName", connectionInfo.getEndpointName());
            args.put("authenticationToken", connectionInfo.getAuthenticationToken());
            args.put("isIncomingConnection", connectionInfo.isIncomingConnection());
            channel.invokeMethod("ad.onConnectionInitiated", args);
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
            Log.d("NearbyCon java", "ad.onConnectionResult");
            Map<String, Object> args = new HashMap<>();
            args.put("endpointId", endpointId);
            int statusCode = -1;
            switch (connectionResolution.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    statusCode = 0;
                    // We're connected! Can now start sending and receiving data.
                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    statusCode = 1;
                    // The connection was rejected by one or both sides.
                    break;
                case ConnectionsStatusCodes.STATUS_ERROR:
                    statusCode = 2;
                    // The connection broke before it was able to be accepted.
                    break;
                default:
                    // Unknown status code
            }
            args.put("statusCode", statusCode);
            channel.invokeMethod("ad.onConnectionResult", args);
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            Log.d("NearbyCon java", "ad.onDisconnected");
            Map<String, Object> args = new HashMap<>();
            args.put("endpointId", endpointId);
            channel.invokeMethod("ad.onDisconnected", args);
        }
    };

    private final ConnectionLifecycleCallback discoverConnectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
            Log.d("NearbyCon java", "dis.onConnectionInitiated");
            Map<String, Object> args = new HashMap<>();
            args.put("endpointId", endpointId);
            args.put("endpointName", connectionInfo.getEndpointName());
            args.put("authenticationToken", connectionInfo.getAuthenticationToken());
            args.put("isIncomingConnection", connectionInfo.isIncomingConnection());
            channel.invokeMethod("dis.onConnectionInitiated", args);
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
            Log.d("NearbyCon java", "dis.onConnectionResult");
            Map<String, Object> args = new HashMap<>();
            args.put("endpointId", endpointId);
            int statusCode = -1;
            switch (connectionResolution.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    statusCode = 0;
                    // We're connected! Can now start sending and receiving data.
                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    statusCode = 1;
                    // The connection was rejected by one or both sides.
                    break;
                case ConnectionsStatusCodes.STATUS_ERROR:
                    statusCode = 2;
                    // The connection broke before it was able to be accepted.
                    break;
                default:
                    // Unknown status code
            }
            args.put("statusCode", statusCode);
            channel.invokeMethod("dis.onConnectionResult", args);
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            Log.d("NearbyCon java", "dis.onDisconnected");
            Map<String, Object> args = new HashMap<>();
            args.put("endpointId", endpointId);
            channel.invokeMethod("dis.onDisconnected", args);
        }
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            Log.d("NearbyCon java", "onPayloadReceived");
            Map<String, Object> args = new HashMap<>();
            args.put("endpointId", endpointId);
            args.put("payloadId", payload.getId());
            args.put("type", payload.getType());

            if (payload.getType() == Payload.Type.BYTES) {
                byte[] bytes = payload.asBytes();
                assert bytes != null;
                args.put("bytes", bytes);
            }

            channel.invokeMethod("onPayloadReceived", args);
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
            //required for files and streams

            Log.d("NearbyCon java", "onPayloadTransferUpdate");
            Map<String, Object> args = new HashMap<>();
            args.put("endpointId", endpointId);
            args.put("payloadId", payloadTransferUpdate.getPayloadId());
            args.put("status", payloadTransferUpdate.getStatus());
            args.put("bytesTransferred", payloadTransferUpdate.getBytesTransferred());
            args.put("totalBytes", payloadTransferUpdate.getTotalBytes());

            channel.invokeMethod("onPayloadTransferUpdate", args);
        }
    };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
            Log.d("NearbyCon java", "onEndpointFound");
            Map<String, Object> args = new HashMap<>();
            args.put("endpointId", endpointId);
            args.put("endpointName", discoveredEndpointInfo.getEndpointName());
            args.put("serviceId", discoveredEndpointInfo.getServiceId());
            channel.invokeMethod("dis.onEndpointFound", args);
        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {
            Log.d("NearbyCon java", "onEndpointLost");
            Map<String, Object> args = new HashMap<>();
            args.put("endpointId", endpointId);
            channel.invokeMethod("dis.onEndpointLost", args);
        }
    };

    private Strategy getStrategy(int strategy) {
        switch (strategy) {
            case 0:
                return Strategy.P2P_CLUSTER;
            case 1:
                return Strategy.P2P_STAR;
            case 2:
                return Strategy.P2P_POINT_TO_POINT;
            default:
                return Strategy.P2P_CLUSTER;
        }
    }
}
