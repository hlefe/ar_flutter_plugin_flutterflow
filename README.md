# ar_flutter_plugin_flutterflow
[![pub package](https://img.shields.io/pub/v/ar_flutter_plugin_flutterflow.svg)](https://pub.dev/packages/ar_flutter_plugin_flutterflow)



This version is a direct adaptation of the original ar_flutter_plugin (https://pub.dev/packages/ar_flutter_plugin), maintaining all its powerful features and capabilities.
This fork was created because the original plugin had not been updated since 2022. <br><br>
➡ Changes include an update to the AR Core endpoint, a gradle upgrade, and compatibility with FlutterFlow.
<br><br>
<b>❤️ I invite you to collaborate and contribute to the improvement of this plugin.</b><br>
To contribute code and discuss ideas, [create a pull request](https://github.com/hlefe/ar_flutter_plugin_flutterflow/compare), [open an issue](https://github.com/hlefe/ar_flutter_plugin_flutterflow/issues/new), or [start a discussion](https://github.com/hlefe/ar_flutter_plugin_flutterflow/discussions).

## Fluterflow demo app
<table>
<td>
<img src="https://avatars.githubusercontent.com/u/74943865?s=48&amp;v=4" width="30" height="30" style="max-width: 100%; margin-bottom: -9px;"> </img>
</td>
<td>
<b> You can find a complete example running on FlutterFlow here :</b><br>
<a href="https://app.flutterflow.io/project/a-r-flutter-lib-ipqw3k">https://app.flutterflow.io/project/a-r-flutter-lib-ipqw3k</a>
</td>
</table>

### Installing

Add the Flutter package to your project by running:

```bash
flutter pub add ar_flutter_plugin_flutterflow
```

Or manually add this to your `pubspec.yaml` file (and run `flutter pub get`):

```yaml
dependencies:
  ar_flutter_plugin_flutterflow: ^0.7.55
```

Or in FlutterFlow : 

<table>
<td>
<img src="https://avatars.githubusercontent.com/u/74943865?s=48&amp;v=4" width="30" height="30" style="max-width: 100%; margin-bottom: -9px;"> </img>
</td>
<td> Simply add : <br> <b>ar_flutter_plugin_flutterflow: ^0.7.55 </b> <br> in pubspecs dependencies of your widget.
</td>
</table>

### Importing

Add this to your code:

```dart
import 'package:ar_flutter_plugin_flutterflow/ar_flutter_plugin.dart';
```
## IOS Permissions
* To prevent your application from crashing when launching augmented reality on iOS, you need to add the following permission to the Info.plist file (located under ios/Runner) :

  ```
  <key>NSCameraUsageDescription</key>
  <string>This application requires camera access for augmented reality functionality.</string>
  
  ```
  <br>
<table>
<td>
<img src="https://avatars.githubusercontent.com/u/74943865?s=48&amp;v=4" width="30" height="30" style="max-width: 100%; margin-bottom: -9px;"> </img>
</td>
<td><b> If you're using FlutterFlow, go to "App Settings" > "Permissions"<br>
 For the "Camera" line, toggle the switch to "On" and add the description :<br> "This application requires access to the camera to enable augmented reality features."  </b><br>
<br>

</td></table>

If you have problems with permissions on iOS (e.g. with the camera view not showing up even though camera access is allowed), add this to the ```podfile``` of your app's ```ios``` directory:

```pod
  post_install do |installer|
    installer.pods_project.targets.each do |target|
      flutter_additional_ios_build_settings(target)
      target.build_configurations.each do |config|
        # Additional configuration options could already be set here

        # BEGINNING OF WHAT YOU SHOULD ADD
        config.build_settings['GCC_PREPROCESSOR_DEFINITIONS'] ||= [
          '$(inherited)',

          ## dart: PermissionGroup.camera
          'PERMISSION_CAMERA=1',

          ## dart: PermissionGroup.photos
          'PERMISSION_PHOTOS=1',

          ## dart: [PermissionGroup.location, PermissionGroup.locationAlways, PermissionGroup.locationWhenInUse]
          'PERMISSION_LOCATION=1',

          ## dart: PermissionGroup.sensors
          'PERMISSION_SENSORS=1',

          ## dart: PermissionGroup.bluetooth
          'PERMISSION_BLUETOOTH=1',

          # add additional permission groups if required
        ]
        # END OF WHAT YOU SHOULD ADD
      end
    end
  end
```

In FlutterFlow :

<table>
<td style="min-width:30px">
<img src="https://avatars.githubusercontent.com/u/74943865?s=48&amp;v=4" width="30" height="30" style="max-width: 100%; margin-bottom: -9px;"> </img>
</td>
<td>
Unfortunately, at this stage, it is not possible to carry out the procedure above within FlutterFlow.  <br>
Therefore, it is necessary to publish your project with github and make the modifications manually. <br> And then publish wih Github selected in Deployment Sources : <br> <a href="https://docs.flutterflow.io/customizing-your-app/manage-custom-code-in-github#id-9.-deploy-from-the-main-branch">FlutterFlow Publish from Github</a>
</td>
</table>


### Example Applications

| Example Name                 | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | Link to Code                                                                                                                                         |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |------------------------------------------------------------------------------------------------------------------------------------------------------|
| Debug Options                | Simple AR scene with toggles to visualize the world origin, feature points and tracked planes                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | [Debug Options Code](https://github.com/hlefe/ar_flutter_plugin_flutterflow/blob/main/examples/debug_options.dart)                                   |
| Local & Online Objects        | AR scene with buttons to place GLTF objects from the flutter asset folders, GLB objects from the internet, or a GLB object from the app's Documents directory at a given position, rotation and scale. Additional buttons allow to modify scale, position and orientation with regard to the world origin after objects have been placed.                                                                                                                                                                                                                                                                | [Local & Online Objects Code](https://github.com/hlefe/ar_flutter_plugin_flutterflow/blob/main/examples/local_and_web_objects.dart)                  |
| Objects & Anchors on Planes  | AR Scene in which tapping on a plane creates an anchor with a 3D model attached to it                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | [Objects & Anchors on Planes Code](https://github.com/hlefe/ar_flutter_plugin_flutterflow/blob/main/examples/objects_on_planes.dart)                 |
| Object Transformation Gestures | Same as Objects & Anchors on Planes example, but objects can be panned and rotated using gestures after being placed                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | [Objects Gestures](https://github.com/hlefe/ar_flutter_plugin_flutterflow/blob/main/examples/object_gestures.dart)                                   |
| Screenshots                  | Same as Objects & Anchors on Planes Example, but the snapshot function is used to take screenshots of the AR Scene                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | [Screenshots Code](https://github.com/hlefe/ar_flutter_plugin_flutterflow/blob/main/examples/screenshot.dart)                            |
| Cloud Anchors                | AR Scene in which objects can be placed, uploaded and downloaded, thus creating an interactive AR experience that can be shared between multiple devices. Currently, the example allows to upload the last placed object along with its anchor and download all anchors within a radius of 100m along with all the attached objects (independent of which device originally placed the objects). As sharing the objects is done by using the Google Cloud Anchor Service and Firebase, this requires some additional setup, please read [Getting Started with cloud anchors](cloudAnchorSetup.md)        | [Cloud Anchors Code](https://github.com/hlefe/ar_flutter_plugin_flutterflow/blob/main/examples/cloud_anchor.dart)                         |
| External Object Management   | Similar to the Cloud Anchors example, but contains UI to choose between different models. Rather than being hard-coded, an external database (Firestore) is used to manage the available models. As sharing the objects is done by using the Google Cloud Anchor Service and Firebase, this requires some additional setup, please read [Getting Started with cloud anchors](cloudAnchorSetup.md). Also make sure that in your Firestore database, the collection "models" contains some entries with the fields "name", "image", and "uri", where "uri" points to the raw file of a model in GLB format | [External Model Management Code](https://github.com/hlefe/ar_flutter_plugin_flutterflow/blob/main/examples/external_model_management.dart) |


## Plugin Architecture

This is a rough sketch of the architecture the plugin implements:

![ar_plugin_architecture](https://github.com/CariusLars/ar_flutter_plugin/raw/main/AR_Plugin_Architecture_highlevel.svg)