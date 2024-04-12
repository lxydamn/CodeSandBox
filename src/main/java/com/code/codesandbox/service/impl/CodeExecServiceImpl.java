package com.code.codesandbox.service.impl;

import com.code.codesandbox.pojo.CodeConfig;
import com.code.codesandbox.pojo.CodeResult;
import com.code.codesandbox.service.CodeExecService;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

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

        DockerClientConfig standard = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(DOCKER_API)
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(standard.getDockerHost()).build();
        DockerClient dockerClient = DockerClientImpl.getInstance(standard, httpClient);

        Image javaImage = findImage(dockerClient.listImagesCmd().exec(), JAVA_IMAGE);

        String containerId = dockerClient.createContainerCmd(javaImage.getId())
                .withHostConfig(HostConfig.newHostConfig().withMemory(code.getMemory() * 1024L * 1024))
                .withCmd("sh", "-c", "echo '" + code.getCode() + "' > Main.java && javac Main.java && java Main")
                .withStopTimeout(30)
                .exec()
                .getId();

        long startTime = System.currentTimeMillis();
        dockerClient.startContainerCmd(containerId).exec();
        // 捕获容器的输出
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();



        try {
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallbackTemplate<>() {
                        @Override
                        public void onNext(Frame frame) {
                            try {
                                outputStream.write(frame.getPayload());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .awaitCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();

        dockerClient.removeContainerCmd(containerId).exec();

        return new CodeResult(
                outputStream.toString(),
                endTime - startTime,
                0D
        );
    }

    private Image findImage(List<Image> images, String imageName) {
        return images
                .stream()
                .filter(image -> imageName.equals(image.getRepoTags()[0]))
                .findFirst()
                .orElse(null);
    }
}
