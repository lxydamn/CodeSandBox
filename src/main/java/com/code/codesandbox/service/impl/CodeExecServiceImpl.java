package com.code.codesandbox.service.impl;

import com.code.codesandbox.pojo.CodeConfig;
import com.code.codesandbox.pojo.CodeResult;
import com.code.codesandbox.service.CodeExecService;
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

/**
 * Created by Lxy on 2024/4/12 16:32
 */
@Service
public class CodeExecServiceImpl implements CodeExecService {
    @Value("${docker.java-image}")
    private String JAVA_IMAGE;
    @Value("${docker.api}")
    private String DOCKER_API;

    @Override
    public CodeResult ExecJavaCode(CodeConfig code) throws IOException {

        // 创建容器并设定内存限制和运行时间限制
        String containerId = dockerClient.createContainerCmd(javaImage.getId())
                .withHostConfig(HostConfig.newHostConfig().withMemory(code.getMemory() * 1024L * 1024))
                .withCmd("sh", "-c", "echo '"
                        + code.getCode()
                        + "' > Main.java && javac Main.java " //
                        + " && /usr/bin/time -f \"%U:%M\" -o /home/consume.out java Main") //将程序消耗输入文件中
                .withStopTimeout(30)
                .exec()
                .getId();
    }

}
