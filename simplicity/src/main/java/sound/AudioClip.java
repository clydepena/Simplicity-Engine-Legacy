package sound;

// import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import asset.Disposable;

import static org.lwjgl.openal.AL11.*;
// import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename;
// import static org.lwjgl.system.MemoryStack.*;
// import static org.lwjgl.system.libc.LibCStdlib.free;

public class AudioClip implements Disposable {
    
    private int bufferId = 0;
    // private int sourceId = 0;
    // private String filepath;
    // private boolean isPlaying = false;

    public AudioClip(ShortBuffer rawAudioBuffer, int channels, int sampleRate) {
        genAudio(rawAudioBuffer, channels, sampleRate);
    }

    private void genAudio(ShortBuffer rawAudioBuffer, int channels, int sampleRate) {
        int format = -1;
        switch (channels) {
            case 1:
                format = AL_FORMAT_MONO16;
                break;
            case 2:
                format = AL_FORMAT_STEREO16;
                break;
        }

        bufferId = alGenBuffers();
        alBufferData(bufferId, format, rawAudioBuffer, sampleRate);

        // sourceId = alGenSources();
        // alSourcei(sourceId, AL_BUFFER, bufferId);
        // alSourcei(sourceId, AL_LOOPING, 0);
        // alSourcei(sourceId, AL_POSITION, 0);
        // alSourcef(sourceId, AL_GAIN, 0.3f);
    }

    // public AudioClip(String filepath) {

    //     stackPush();
    //     IntBuffer channelsBuffer = stackMallocInt(1);
    //     stackPush();
    //     IntBuffer sampleRateBuffer = stackCallocInt(1);

    //     ShortBuffer rawAudioBuffer = stb_vorbis_decode_filename(filepath, channelsBuffer, sampleRateBuffer);

    //     if (rawAudioBuffer == null) {
    //         System.out.println("Could not load sound '" + filepath + "'");
    //         stackPop();
    //         stackPop();
    //         return;
    //     }

    //     int channels = channelsBuffer.get();
    //     int sampleRate = sampleRateBuffer.get();
    //     stackPop();
    //     stackPop();

    //     genAudio(rawAudioBuffer, channels, sampleRate);

    //     free(rawAudioBuffer);
    // }

    // public void destroy() {
    //     dispose();
    // }

    // public void play() {
    //     int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
    //     if (state == AL_STOPPED) {
    //         isPlaying = false;
    //         alSourcei(sourceId, AL_POSITION, 0);
    //     }
    //     if (!isPlaying) {
    //         alSourcePlay(sourceId);
    //         isPlaying = true;
    //     }
    // }

    // public void stop() {
    //     if (isPlaying) {
    //         alSourceStop(sourceId);
    //         isPlaying = false;
    //     }
    // }

    // public String getFilepath() {
    //     return this.filepath;
    // }

    // public boolean isPlaying() {
    //     int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
    //     if (state == AL_STOPPED) {
    //         isPlaying = false;
    //     }
    //     return this.isPlaying;
    // }

    @Override
    public void dispose() {
        // if (sourceId <= 0) {
        //     alDeleteSources(sourceId);
        // }
        if (bufferId <= 0) {
            alDeleteBuffers(bufferId);
        }
    }
}
