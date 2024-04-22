import 'dart:typed_data';

import 'depth_img_array.dart';

class ARImage {
  ARImage({
    this.width,
    this.height,
    this.depthImgBytes,
    this.depthImgArrays,
    this.rawDepthImgBytes,
    this.confidenceImgBytes,
    this.rawDepthImgArrays,
    this.confidenceImgArrays,
  })  : assert(depthImgBytes != null),
        assert(width != null && width > 0),
        assert(height != null && height > 0);

  final Uint8List? depthImgBytes;
  final Uint8List? rawDepthImgBytes;
  final Uint8List? confidenceImgBytes;
  final int? width;
  final int? height;
  final DepthImgArrays? depthImgArrays;
  final DepthImgArrays? rawDepthImgArrays;
  final DepthImgArrays? confidenceImgArrays;

  static ARImage fromMap(Map<dynamic, dynamic> map) {
    return ARImage(
      depthImgBytes: map['depthImgBytes'],
      rawDepthImgBytes: map['rawDepthImgBytes'],
      confidenceImgBytes: map['confidenceImgBytes'],
      width: map['width'],
      height: map['height'],
      depthImgArrays: DepthImgArrays.fromMap(Map.from(map['depthImgArrays'])),
      rawDepthImgArrays:
          map['rawDepthImgArrays'] == null
              ? null
              : DepthImgArrays.fromMap(Map.from(map['rawDepthImgArrays'])),
      confidenceImgArrays:
          map['confidenceImgArrays'] == null
              ? null
              : DepthImgArrays.fromMap(Map.from(map['confidenceImgArrays'])),
    );
  }
}
