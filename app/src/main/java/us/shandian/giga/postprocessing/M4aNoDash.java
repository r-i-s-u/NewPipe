package us.shandian.giga.postprocessing;

import org.schabi.newpipe.streams.Mp4DashReader;
import org.schabi.newpipe.streams.Mp4FromDashWriter;
import org.schabi.newpipe.streams.Mp4Tagger;
import org.schabi.newpipe.streams.io.SharpStream;

import java.io.IOException;

class M4aNoDash extends Postprocessing {

    private transient boolean dash = true;
    private transient byte[] udta;

    M4aNoDash() {
        super(false, true, ALGORITHM_M4A_NO_DASH);
    }

    @Override
    boolean test(SharpStream... sources) throws IOException {
        // check if the mp4 file is DASH (youtube)

        Mp4DashReader reader = new Mp4DashReader(sources[0]);
        reader.parse();

        switch (reader.getBrands()[0]) {
            case 0x64617368:// DASH
            case 0x69736F35:// ISO5
                dash = true;
                return true;
            default:
                break;
        }

        // plain (non-DASH) mp4/m4a: no remuxing needed, only the tags are added
        dash = false;
        sources[0].rewind();
        if (!Mp4Tagger.canTag(sources[0])) {
            return false;
        }
        udta = metadataBox();
        return udta != null;
    }

    @Override
    int process(SharpStream out, SharpStream... sources) throws IOException {
        if (!dash) {
            Mp4Tagger.tag(sources[0], out, udta);
            return OK_RESULT;
        }

        Mp4FromDashWriter muxer = new Mp4FromDashWriter(sources[0]);
        muxer.setMainBrand(0x4D344120);// binary string "M4A "
        muxer.parseSources();
        muxer.selectTracks(0);
        applyMetadata(muxer);
        muxer.build(out);

        return OK_RESULT;
    }
}
