package com.loadtest.project.service;

import java.nio.file.Path;

public record BuildResult(
        boolean success,
        String output,
        Path classesDir
) {}
