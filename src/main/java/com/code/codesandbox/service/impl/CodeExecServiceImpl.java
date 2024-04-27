package com.code.codesandbox.service.impl;

import com.code.codesandbox.pojo.CodeConfig;
import com.code.codesandbox.pojo.CodeResult;
import com.code.codesandbox.pojo.UserConfig;
import com.code.codesandbox.service.CodeExecService;
import com.code.codesandbox.service.utils.CodeExecJob;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by Lxy on 2024/4/12 16:32
 */
@Service
public class CodeExecServiceImpl implements CodeExecService {
    @Value("${docker.java-image}")
    private String JAVA_IMAGE;
    @Value("${docker.api}")
    private String DOCKER_API;

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            5,
            100,
            0,
            java.util.concurrent.TimeUnit.MILLISECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>()
    );

    @Override
    public CodeResult ExecJavaCode(UserConfig config) throws ExecutionException, InterruptedException {

        String[] cmd = new String[] {
                "sh",
                "-c",
                "echo '"
                        + config.getCode()
                        + "' > Main.java "
                        + "&& echo '"
                        + config.getInput()
                        + "' > input.in "
                        + "&& javac Main.java"
                        + " && /usr/bin/time -f \"%U:%X\" -o /home/consume.out java Main < input.in"
        };

        CodeConfig codeConfig = new CodeConfig(
              config.getCode(),
              config.getMemory(),
              config.getRuntime(),
              DOCKER_API,
              JAVA_IMAGE
        );

        FutureTask<CodeResult> codeResult = new FutureTask<>(new CodeExecJob(
                codeConfig,
                cmd
        ));

        executor.submit(codeResult);

        return codeResult.get();
    }

}
