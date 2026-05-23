package com.loadtest.project.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class GitService {

    private static final Logger log = LoggerFactory.getLogger(GitService.class);

    private static final Pattern CLASS_PATTERN =
            Pattern.compile("class\\s+(\\w+)\\s+extends\\s+Simulation");
    private static final Pattern PACKAGE_PATTERN =
            Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);

    public void cloneRepository(String gitUrl, String branch, Path targetDir) throws GitAPIException {
        log.info("Cloning {} branch={} into {}", gitUrl, branch, targetDir);
        try (Git git = Git.cloneRepository()
                .setURI(gitUrl)
                .setBranch(branch)
                .setDirectory(targetDir.toFile())
                .call()) {
            log.info("Cloned successfully: {}", git.getRepository().getDirectory());
        }
    }

    public void pullRepository(Path localDir) throws IOException, GitAPIException {
        log.info("Pulling repository at {}", localDir);
        try (Git git = Git.open(localDir.toFile())) {
            git.pull().call();
            log.info("Pull complete for {}", localDir);
        }
    }

    /**
     * Scans Java source files under src/ to find classes extending Gatling's Simulation.
     * Returns fully qualified class names.
     */
    public List<String> findSimulationClasses(Path projectDir) {
        Path srcDir = projectDir.resolve("src");
        if (!Files.exists(srcDir)) {
            log.warn("No src/ directory found in {}", projectDir);
            return List.of();
        }

        List<String> simulations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(srcDir)) {
            files.filter(p -> p.toString().endsWith(".java"))
                    .forEach(javaFile -> {
                        try {
                            String content = Files.readString(javaFile);
                            Matcher classMatcher = CLASS_PATTERN.matcher(content);
                            if (classMatcher.find()) {
                                String simpleName = classMatcher.group(1);
                                Matcher pkgMatcher = PACKAGE_PATTERN.matcher(content);
                                String fqn = pkgMatcher.find()
                                        ? pkgMatcher.group(1) + "." + simpleName
                                        : simpleName;
                                simulations.add(fqn);
                                log.debug("Found simulation: {}", fqn);
                            }
                        } catch (IOException e) {
                            log.warn("Could not read {}: {}", javaFile, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to walk src directory {}: {}", srcDir, e.getMessage());
        }

        return simulations;
    }
}
