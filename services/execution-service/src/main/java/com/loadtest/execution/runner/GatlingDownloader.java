package com.loadtest.execution.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Component
public class GatlingDownloader {

    private static final Logger log = LoggerFactory.getLogger(GatlingDownloader.class);

    @Value("${gatling.home}")
    private String gatlingHome;

    @Value("${gatling.version}")
    private String gatlingVersion;

    @Value("${gatling.download-url}")
    private String downloadUrl;

    @EventListener(ContextRefreshedEvent.class)
    public void ensureGatlingInstalled() {
        Path home = Path.of(gatlingHome);
        Path libDir = home.resolve("lib");

        if (Files.exists(libDir) && hasJars(libDir)) {
            log.info("Gatling {} already installed at {}", gatlingVersion, gatlingHome);
            return;
        }

        log.info("Downloading Gatling {} from {}", gatlingVersion, downloadUrl);
        try {
            download();
        } catch (Exception e) {
            log.error("Failed to download Gatling: {}", e.getMessage(), e);
            throw new IllegalStateException("Gatling installation failed", e);
        }
    }

    private boolean hasJars(Path libDir) {
        try {
            return Files.list(libDir).anyMatch(p -> p.toString().endsWith(".jar"));
        } catch (IOException e) {
            return false;
        }
    }

    private void download() throws IOException, InterruptedException {
        Path home = Path.of(gatlingHome);
        Path zipFile = Files.createTempFile("gatling-", ".zip");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected HTTP status " + response.statusCode() + " downloading Gatling");
        }

        try (InputStream in = response.body()) {
            Files.copy(in, zipFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        Files.createDirectories(home);
        extract(zipFile, home);
        Files.deleteIfExists(zipFile);
        log.info("Gatling {} installed to {}", gatlingVersion, gatlingHome);
    }

    private void extract(Path zipFile, Path targetDir) throws IOException, InterruptedException {
        // Unzip then flatten: gatling-{version}/* → gatling.home/*
        Path tmpExtract = Files.createTempDirectory("gatling-extract-");
        try {
            Process unzip = new ProcessBuilder("unzip", "-q", zipFile.toString(), "-d", tmpExtract.toString())
                    .redirectErrorStream(true)
                    .start();
            if (unzip.waitFor() != 0) {
                throw new IOException("unzip failed with exit code " + unzip.exitValue());
            }

            // Find the single top-level directory inside the zip
            Path inner = Files.list(tmpExtract)
                    .filter(Files::isDirectory)
                    .findFirst()
                    .orElseThrow(() -> new IOException("Unexpected zip structure"));

            // Move contents to gatling.home
            Files.list(inner).forEach(src -> {
                try {
                    Path dest = targetDir.resolve(inner.relativize(src).toString());
                    Files.move(src, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } finally {
            deleteDirectory(tmpExtract);
        }
    }

    private void deleteDirectory(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
        } catch (IOException e) {
            log.warn("Could not delete temp dir {}: {}", dir, e.getMessage());
        }
    }
}
