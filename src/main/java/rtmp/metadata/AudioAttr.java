package rtmp.metadata;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class AudioAttr {

    @SerializedName("codec_id")
    private String audioCodecId;

    @SerializedName("data_rate")
    private String audioDataRate;

    @SerializedName("sample_rate")
    private String audioSampleRate;

    @SerializedName("sample_size")
    private String audioSampleSize;

    @SerializedName("channels")
    private String audioChannels;

}
