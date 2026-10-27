package com.example.loanassistant.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import com.example.loanassistant.domain.LoanApplication;
import com.example.loanassistant.domain.LoanDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentStorageService {

    private final Path uploadDir;

    public DocumentStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public List<LoanDocument> store(LoanApplication app, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        Path dir = uploadDir.resolve("application-" + app.getId());
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload directory", e);
        }
        return files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .map(f -> storeOne(dir, app, f))
                .toList();
    }

    private LoanDocument storeOne(Path dir, LoanApplication app, MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = dir.resolve(UUID.randomUUID() + "_" + safeName);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store uploaded file " + original, e);
        }
        LoanDocument doc = new LoanDocument();
        doc.setApplication(app);
        doc.setOriginalFilename(original);
        doc.setContentType(file.getContentType());
        doc.setSizeBytes(file.getSize());
        doc.setStoragePath(target.toString());
        doc.setExtractedText(extractText(file.getContentType(), target));
        return doc;
    }

    private String extractText(String contentType, Path target) {
        if (contentType == null || !contentType.startsWith("text/")) {
            return null;
        }
        try {
            String text = Files.readString(target, StandardCharsets.UTF_8);
            return text.length() > 100_000 ? text.substring(0, 100_000) : text;
        } catch (IOException e) {
            return null;
        }
    }

    public Resource load(LoanDocument doc) {
        try {
            Path path = Path.of(doc.getStoragePath()).normalize();
            if (!path.startsWith(uploadDir)) {
                throw new IllegalArgumentException("Invalid document path");
            }
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                throw new IllegalArgumentException("Document file not found");
            }
            return resource;
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("Invalid document path", e);
        }
    }
}
