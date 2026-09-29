package codecs;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.List;

import org.lwjgl.BufferUtils;

import asset.AssetLoader;
import asset.AssetPoolHandler;
import asset.Disposable;
import sound.AudioClip;
import static org.lwjgl.stb.STBVorbis.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.libc.LibCStdlib.free;

public class AudioClipCodec implements AssetLoader<AudioClip, AudioClipCodec.AudioClipData> {

    public static class AudioClipData implements Disposable {
        public ShortBuffer rawAudioBuffer;
        public int channels;
        public int sampleRate;

        public AudioClipData(ShortBuffer rawAudioBuffer, int channels, int sampleRate) {
            this.rawAudioBuffer = rawAudioBuffer;
            this.channels = channels;
            this.sampleRate = sampleRate;
        }

        @Override
        public void dispose() {
            free(rawAudioBuffer);
        }

    }

    public static final List<String> EXTENSIONS = List.of("ogg", "oga");

    @Override
    public List<String> supportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    public AudioClipData decode(byte[] bytes) {
        ByteBuffer encoded = BufferUtils.createByteBuffer(bytes.length).put(bytes).flip();
        stackPush();
        IntBuffer channelsBuffer = stackMallocInt(1);
        stackPush();
        IntBuffer sampleRateBuffer = stackCallocInt(1);
        ShortBuffer rawAudioBuffer = stb_vorbis_decode_memory(encoded, channelsBuffer, sampleRateBuffer);

        if (rawAudioBuffer == null) {
            stackPop();
            stackPop();
            throw new IllegalStateException("can't decode audio");
        }

        int channels = channelsBuffer.get();
        int sampleRate = sampleRateBuffer.get();
        stackPop();
        stackPop();
        return new AudioClipData(rawAudioBuffer, channels, sampleRate);
    }

    @Override
    public AudioClip finish(AudioClipData decoded, AssetPoolHandler handler) {
        try {
            return new AudioClip(decoded.rawAudioBuffer, decoded.channels, decoded.sampleRate);
        } finally {
            decoded.dispose();
        }
    }
}