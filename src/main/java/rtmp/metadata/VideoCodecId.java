package rtmp.metadata;

// @ref https://rtmp.veriskope.com/pdf/video_file_format_spec_v10.pdf > Video tags (page 9)
public enum VideoCodecId {

    // 1) Not allowed codecs
    //JPEG(1), // (currently unused)
    //SorensonH263(2),
    //ScreenVideo(3),
    //On2VP6(4),
    //On2VP6WithAlphaChannel(5),
    //ScreenVideoVersion2(6),

    // 2) Allowed codecs
    AVC(7),
    HEVC(12)
    ;

    final int id;
    VideoCodecId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static String getCodecIdListString() {
        StringBuilder result = new StringBuilder();
        for (VideoCodecId videoCodecId : VideoCodecId.values()) {
            result.append(videoCodecId.name()).append(":").append(videoCodecId.getId());
            result.append(",");
        }
        return result.toString();
    }

}
