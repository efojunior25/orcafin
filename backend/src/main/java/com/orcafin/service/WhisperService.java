package com.orcafin.service;

import com.orcafin.exception.AiUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class WhisperService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.whisper.base-url}")
    private String whisperBaseUrl;

    public String transcribe(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("Nenhum áudio foi enviado.");
        }
        try {
            RestClient client = RestClient.create(whisperBaseUrl);

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
}
