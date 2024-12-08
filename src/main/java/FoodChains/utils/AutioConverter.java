package FoodChains.utils;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.File;
import java.io.IOException;

public class AutioConverter {

    public static void convertWavToOpus(File inputWav, File outputOpus) throws IOException {
        // FFmpeg 인스턴스 생성
        FFmpeg ffmpeg = new FFmpeg("src/main/resources/ffmpeg"); // ffmpeg 실행 파일 경로 설정
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(inputWav.getAbsolutePath()) // 입력 파일
                .overrideOutputFiles(true) // 기존 출력 파일 덮어쓰기
                .addOutput(outputOpus.getAbsolutePath()) // 출력 파일 설정
                .setAudioCodec("libopus") // Opus로 인코딩
                .done();

        // FFmpegExecutor 생성
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);

        // 변환 실행
        executor.createJob(builder).run();
    }
}
