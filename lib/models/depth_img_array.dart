class DepthImgArrays {
  List<int> xBuffer;
  List<int> yBuffer;
  List<double> dBuffer;
  List<double> percentageBuffer;
  int length;

  DepthImgArrays(
      {required this.xBuffer,
      required this.yBuffer,
      required this.dBuffer,
      required this.percentageBuffer,
      required this.length});

  factory DepthImgArrays.fromMap(Map<String, dynamic> map) {
    return DepthImgArrays(
      xBuffer: List<int>.from(map['xBuffer']),
      yBuffer: List<int>.from(map['yBuffer']),
      dBuffer: List<double>.from(map['dBuffer']),
      percentageBuffer: List<double>.from(map['percentageBuffer']),
      length: map['length'],
    );
  }

  Map<String, dynamic> toJson() => {
    'xBuffer': xBuffer,
    'yBuffer': yBuffer,
    'dBuffer': dBuffer,
    'percentageBuffer': percentageBuffer,
    'length': length,
  };
}
