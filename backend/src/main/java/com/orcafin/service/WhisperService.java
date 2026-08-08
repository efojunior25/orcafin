package com.orcafin.service;

import com.orcafin.exception.AiUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class WhisperService {

    private static final SimpleClientHttpRequestFactory REQUEST_FACTORY = buildRequestFactory();

    private static SimpleClientHttpRequestFactory buildRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(60));
        return factory;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.whisper.base-url}")
    private String whisperBaseUrl;

    private static final long MAX_AUDIO_BYTES = 10L * 1024 * 1024;
    private static final int MAGIC_BYTES_TO_READ = 16;

    public String transcribe(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("Nenhum áudio foi enviado.");
        }
        if (audioFile.getSize() > MAX_AUDIO_BYTES) {
            throw new IllegalArgumentException("Áudio muito grande (máximo 10MB).");
        }
        String contentType = audioFile.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new IllegalArgumentException("Arquivo enviado não é um áudio válido.");
        }
        if (!hasValidAudioSignature(audioFile)) {
            throw new IllegalArgumentException("Arquivo enviado não é um áudio reconhecido.");
        }
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(whisperBaseUrl)
                    .requestFactory(REQUEST_FACTORY)
                    .build();

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("audio_file", audioFile.getResource());

            String response = client.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/asr")
                            .queryParam("output", "json")
                            .queryParam("task", "transcribe")
                            .queryParam("language", "pt")
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (response == null) {
                throw new AiUnavailableException("O serviço de transcrição não retornou uma resposta.");
            }

            JsonNode node = objectMapper.readTree(response);
            String text = node.path("text").asText("").trim();
            if (text.isBlank()) {
                throw new AiUnavailableException("Não consegui entender o áudio. Tente falar mais claramente.");
            }
            return text;
        } catch (RestClientException e) {
            throw new AiUnavailableException(
                    "Transcrição de áudio indisponível. Verifique se o container 'orcafin-whisper' está rodando (docker compose --profile ai up -d whisper).");
        }
    }

    /**
     * Content-Type do multipart é enviado pelo cliente e é trivialmente falsificável;
     * aqui checamos a assinatura real (magic bytes) dos formatos de áudio aceitos.
     */
    private boolean hasValidAudioSignature(MultipartFile audioFile) {
        byte[] header = new byte[MAGIC_BYTES_TO_READ];
        int read;
        try (InputStream in = audioFile.getInputStream()) {
            read = in.readNBytes(header, 0, header.length);
        } catch (IOException e) {
            return false;
        }
        if (read < 4) {
            return false;
        }
        // WebM/Matroska (MediaRecorder do navegador): 1A 45 DF A3
        if (matches(header, 0, (byte) 0x1A, (byte) 0x45, (byte) 0xDF, (byte) 0xA3)) return true;
        // WAV: "RIFF"
        if (matches(header, 0, (byte) 'R', (byte) 'I', (byte) 'F', (byte) 'F')) return true;
        // OGG: "OggS"
        if (matches(header, 0, (byte) 'O', (byte) 'g', (byte) 'g', (byte) 'S')) return true;
        // MP3 com tag ID3
        if (matches(header, 0, (byte) 'I', (byte) 'D', (byte) '3')) return true;
        // MP3 sem tag ID3 (frame sync 0xFFFB/0xFFF3/0xFFF2)
        if (read >= 2 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0) return true;
        // MP4/M4A: "ftyp" a partir do byte 4
        if (read >= 8 && matches(header, 4, (byte) 'f', (byte) 't', (byte) 'y', (byte) 'p')) return true;
        return false;
    }

    private boolean matches(byte[] data, int offset, byte... signature) {
        if (offset + signature.length > data.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (data[offset + i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
