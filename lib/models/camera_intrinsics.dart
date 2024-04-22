class CameraIntrinsics {
  final double focalLengthX;
  final double focalLengthY;
  final int imageWidth;
  final int imageHeight;
  final double principalPointX;
  final double principalPointY;

  CameraIntrinsics({
    required this.focalLengthX,
    required this.focalLengthY,
    required this.imageWidth,
    required this.imageHeight,
    required this.principalPointX,
    required this.principalPointY,
  });

  static CameraIntrinsics fromMap(Map<dynamic, dynamic> map) {
    return CameraIntrinsics(
      focalLengthX: map['focalLengthX'],
      focalLengthY: map['focalLengthY'],
      imageWidth: map['imageWidth'],
      imageHeight: map['imageHeight'],
      principalPointX: map['principalPointX'],
      principalPointY: map['principalPointY'],
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'focalLengthX': focalLengthX,
      'focalLengthY': focalLengthY,
      'imageWidth': imageWidth,
      'imageHeight': imageHeight,
      'principalPointX': principalPointX,
      'principalPointY': principalPointY,
    };
  }
}
