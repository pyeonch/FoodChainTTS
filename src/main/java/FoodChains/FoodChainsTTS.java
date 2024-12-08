package FoodChains;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;


import java.io.*;
import java.nio.ByteBuffer;

import static FoodChains.Consts.*;

public class FoodChainsTTS extends ListenerAdapter {

    static Area currentArea = Area.FOREST;
    private AudioManager audioManager;
    private TextToSpeechClient ttsClient;
    private AudioPlayerManager playerManager;
    private AudioPlayer player;


    public static void main(String[] args) {
        try {
            // 인증 파일 경로 지정 및 TTS 클라이언트 초기화
            String credentialsPath = "src/main/resources/endless-set-444008-f4-01242aec48da.json";
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath));
            TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build();
            TextToSpeechClient ttsClient = TextToSpeechClient.create(settings);

            // JDA 초기화
            JDA jda = JDABuilder.createDefault(currentArea.getToken()).enableIntents(GatewayIntent.MESSAGE_CONTENT).build();
            jda.addEventListener(new FoodChainsTTS(ttsClient));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1); // 오류 발생 시 프로그램 종료
        }
    }

    public FoodChainsTTS(TextToSpeechClient ttsClient) {
        this.ttsClient = ttsClient;
        playerManager = new DefaultAudioPlayerManager();
        player = playerManager.createPlayer();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(event.getAuthor().isBot()) return;
        if (!event.getChannel().getId().equals(currentArea.getChatChannelId())) {
            return;
        }

        String message = event.getMessage().getContentDisplay();
        String voiceChannelId = currentArea.getChannelId();
        Guild guild = event.getGuild();

        if(message.equalsIgnoreCase("/join")) {
            joinVoiceChannel(guild, event, voiceChannelId);
        } else if (message.equalsIgnoreCase("/leave")) {
            leaveVoiceChannel(event);
        } else {
            // /join 상태가 아닌 경우 TTS 처리 무시
            if (audioManager == null || !audioManager.isConnected()) {
                event.getChannel().sendMessage("먼저 `/join` 명령어로 음성 채널에 연결하세요!").queue();
                return;
            }

            // TTS 변환 및 재생
            File audioFile = convertTextToSpeech(message);

            if (audioFile == null) {
                event.getChannel().sendMessage("TTS 변환 중 오류가 발생했습니다!").queue();
            } else {
                playAudio(audioFile, guild);
            }
        }
    }

    private void joinVoiceChannel(Guild guild, MessageReceivedEvent event, String voiceChannelId) {
        AudioChannel voiceChannel = guild.getVoiceChannelById(voiceChannelId);

        if (voiceChannel != null) {
            audioManager = guild.getAudioManager();
            audioManager.openAudioConnection(voiceChannel);
            event.getChannel().sendMessage("음성 채널에 연결되었습니다!").queue();
        } else {
            event.getChannel().sendMessage("해당 음성 채널을 찾을 수 없습니다!").queue();
        }
    }

    private void leaveVoiceChannel(MessageReceivedEvent event) {
        if (audioManager != null && audioManager.isConnected()) {
            audioManager.closeAudioConnection();
            event.getChannel().sendMessage("음성 채널 연결이 해제되었습니다!").queue();
        } else {
            event.getChannel().sendMessage("현재 연결된 음성 채널이 없습니다!").queue();
        }
    }

    private File convertTextToSpeech(String text) {
        try {
            // 텍스트 입력
            SynthesisInput input = SynthesisInput.newBuilder()
                    .setText(text)
                    .build();

            // 음성 선택 (언어와 성별 지정)
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode("ko-KR") // 한국어
                    .setSsmlGender(SsmlVoiceGender.FEMALE) // 여성 음성
                    .build();

            // 오디오 설정
            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.LINEAR16) // PCM (WAV)
                    .build();

            // API 호출
            SynthesizeSpeechResponse response = ttsClient.synthesizeSpeech(input, voice, audioConfig);

            // 오디오 데이터를 파일로 저장
            ByteString audioContents = response.getAudioContent();
            File outputFile = new File("src/main/resources/output.wav");
            try (OutputStream out = new FileOutputStream(outputFile)) {
                out.write(audioContents.toByteArray());
            }

            return outputFile;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void playAudio(File audioFile, Guild guild) {
        File outputOpus = new File("src/main/resources/output.opus");

        try {
            // WAV 파일을 Opus로 변환
            AutioConverter.convertWavToOpus(audioFile, outputOpus);




        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
