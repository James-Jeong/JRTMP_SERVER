package rtmp.metadata;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class VideoAttr {

    @SerializedName("width")
    private String videoWidth;

    @SerializedName("height")
    private String videoHeight;

    @SerializedName("codec_id")
    private String videoCodecId;

    @SerializedName("data_rate")
    private String videoDataRate;

    @SerializedName("frame_rate")
    private String videoFrameRate;

}
