package distributed_systems.spot;

import android.media.MediaDataSource;

import java.io.IOException;

public class StreamMediaDataSource extends MediaDataSource {
    private byte[] data;

    public StreamMediaDataSource(){}

    public StreamMediaDataSource(byte[] data){
        this.data = data;
    }

    @Override
    public int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
        int length = data.length;
        if (position >= length) {
            return -1; // -1 indicates EOF
        }
        if (position + size > length) {
            size -= (position + size) - length;
        }
        System.arraycopy(data, (int)position, buffer, offset, size);
        return size;
    }



    @Override
    public long getSize() throws IOException {
        return this.data.length;
    }

    @Override
    public void close() throws IOException {
    }




}
