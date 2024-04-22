import 'dart:typed_data';

class CameraImage {
  CameraImage({
    this.bytes,
    this.width,
    this.height,
  })  : assert(bytes != null),
        assert(width != null && width > 0),
        assert(height != null && height > 0);

  final Uint8List? bytes;
  final int? width;
  final int? height;

  static CameraImage fromMap(Map<dynamic, dynamic> map) {
    return CameraImage(
      bytes: map['bytes'],
      width: map['width'],
      height: map['height'],
    );
  }

  Map<String, dynamic> toMap() => <String, dynamic>{
    'bytes': bytes,
    'width': width,
    'height': height
  }..removeWhere((String k, dynamic v) => v == null);
}