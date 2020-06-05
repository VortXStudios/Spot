package distributed_systems.spot.Code;
/*
    IAKOVOS EVDAIMON 3130059
    NIKOS KOULOS 3150079
    STEFANOS PAVLOPOULOS 3130168
    GIANNIS IPSILANTIS 3130215
 */

import java.io.Serializable;

public class Value implements Serializable {

    private static final long serialVersionUID = 5706425475419773281L;
    private MusicFile musicFile;
    private boolean failure = false;

    public Value(){}

    public Value(MusicFile musicFile) {
        this.musicFile = musicFile;
        this.failure = false;
    }

    public MusicFile getMusicFile() {
        return musicFile;
    }

    public void setMusicFile(MusicFile musicFile) {
        this.musicFile = musicFile;
    }

    public void setFailure(boolean failure){this.failure = failure;}

    public boolean getFailure(){return this.failure;}
}
