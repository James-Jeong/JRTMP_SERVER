package rtmp.metadata;

// @ref https://rtmp.veriskope.com/pdf/video_file_format_spec_v10.pdf > Audio tags (page 9)
public enum AudioCodecId {

    // 1) Not allowed codecs
    //LinearPCMPlatformEndian(0),
    //ADPCM(1),
    //MP3(2),
    //LinearPCMLittleEndian(3),
    //Nellymoser16kHzMono(4),
    //Nellymoser8kHzMono(5),
    //Nellymoser(6),
    //G711alawLogarithmicPCM(7),
    //G711mulawLogarithmicPCM(8),
    //reserved(9),
    //Speex(11),
    //MP38Khz(14),
    //DeviceSpecificSound(15),

    // 2) Allowed codecs
    AAC(10)
    ;

    final int id;

    AudioCodecId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static String getCodecIdListString() {
        StringBuilder result = new StringBuilder();
        for (AudioCodecId audioCodecId : AudioCodecId.values()) {
            result.append(audioCodecId.name()).append(":").append(audioCodecId.getId());
            result.append(",");
        }
        return result.toString();
    }

}
