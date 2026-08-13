package com.orcafin.service;

import com.orcafin.entity.TransactionType;
import com.orcafin.dto.StatementEntry;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrai o texto bruto de um extrato/fatura enviado (PDF, CSV ou OFX). PDF e CSV
 * viram texto puro e são interpretados pela IA local (AiParsingService); OFX é
 * estruturado o suficiente pra ter um parser determinístico, sem depender de IA.
 */
@Service
public class StatementParsingService {

    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    public enum Format { PDF, CSV, OFX, UNKNOWN }

    public Format detectFormat(String filename) {
        if (filename == null) return Format.UNKNOWN;
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return Format.PDF;
        if (lower.endsWith(".csv")) return Format.CSV;
        if (lower.endsWith(".ofx") || lower.endsWith(".qfx")) return Format.OFX;
        return Format.UNKNOWN;
    }

    public String extractRawText(MultipartFile file, Format format) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo foi enviado.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Arquivo muito grande (máximo 10MB).");
        }
        try {
            return switch (format) {
                case PDF -> extractPdfText(file.getBytes());
                case CSV, OFX -> new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                default -> throw new IllegalArgumentException("Formato de arquivo não reconhecido (use PDF, CSV ou OFX).");
            };
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o arquivo enviado.");
        }
    }

    private String extractPdfText(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    // OFX: blocos <STMTTRN>...<TRNAMT>-40.82</TRNAMT><DTPOSTED>20260806...</DTPOSTED><NAME>...</NAME>...</STMTTRN>
    private static final Pattern TRN_BLOCK = Pattern.compile("<STMTTRN>(.*?)</STMTTRN>", Pattern.DOTALL);
    private static final Pattern TRNAMT = Pattern.compile("<TRNAMT>([^<\\r\\n]+)");
    private static final Pattern DTPOSTED = Pattern.compile("<DTPOSTED>([^<\\r\\n]+)");
    private static final Pattern NAME = Pattern.compile("<NAME>([^<\\r\\n]+)");
    private static final Pattern MEMO = Pattern.compile("<MEMO>([^<\\r\\n]+)");
    private static final DateTimeFormatter OFX_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<StatementEntry> parseOfx(String rawText) {
        List<StatementEntry> entries = new ArrayList<>();
        Matcher blocks = TRN_BLOCK.matcher(rawText);
        while (blocks.find()) {
            String block = blocks.group(1);

            Matcher amtMatcher = TRNAMT.matcher(block);
            if (!amtMatcher.find()) continue;
            BigDecimal rawAmount = new BigDecimal(amtMatcher.group(1).trim());

            Matcher dateMatcher = DTPOSTED.matcher(block);
            if (!dateMatcher.find()) continue;
            String rawDate = dateMatcher.group(1).trim();
            LocalDate date = LocalDate.parse(rawDate.substring(0, 8), OFX_DATE);

            String description = firstMatch(NAME, block).orElseGet(() -> firstMatch(MEMO, block).orElse("Lançamento"));

            TransactionType type = rawAmount.signum() < 0 ? TransactionType.DESPESA : TransactionType.RECEITA;
            entries.add(new StatementEntry(date, description.trim(), rawAmount.abs(), type));
        }
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Não foi possível interpretar nenhum lançamento no arquivo OFX.");
        }
        return entries;
    }

    private java.util.Optional<String> firstMatch(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? java.util.Optional.of(m.group(1)) : java.util.Optional.empty();
    }
}
