#import "ArFlutterPlugin.h"
#if __has_include(<ar_flutter_plugin_flutterflow/ar_flutter_plugin_flutterflow-Swift.h>)
#import <ar_flutter_plugin_flutterflow/ar_flutter_plugin_flutterflow-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "ar_flutter_plugin_flutterflow-Swift.h"
#endif

@implementation ArFlutterPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftArFlutterPlugin registerWithRegistrar:registrar];
}
@end
