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

        // 连接到Docker
        DockerClientConfig standard = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(DOCKER_API)
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(standard.getDockerHost()).build();
        DockerClient dockerClient = DockerClientImpl.getInstance(standard, httpClient);

        Image javaImage = findImage(dockerClient.listImagesCmd().exec(), JAVA_IMAGE);

        // 创建容器并设定内存限制和运行时间限制
        String containerId = dockerClient.createContainerCmd(javaImage.getId())
                .withHostConfig(HostConfig.newHostConfig().withMemory(code.getMemory() * 1024L * 1024))
                .withCmd("sh", "-c", "echo '"
                        + code.getCode()
                        + "' > Main.java && javac Main.java " //
                        + " && /usr/bin/time -f \"%U:%K\" -o /home/consume.out java Main") //将程序消耗输入文件中
                .withStopTimeout(30)
                .exec()
                .getId();
        // 启动容器
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
                    .awaitCompletion(); // 等待容器运行结束
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 读取容器消耗
        String[] strings = readContainerFile(dockerClient, containerId, "/home/consume.out", "consume.out");

        // 删除容器
        dockerClient.removeContainerCmd(containerId).exec();

        return new CodeResult(
                outputStream.toString(),
                strings == null ? 0 : Double.parseDouble(strings[0]),
                strings == null ? 0d : Double.parseDouble(strings[1])
        );
    }

    /**
     * 读取容器中的代码执行后的运行时间和内存消耗
     * @param dockerClient
     * @param containerId
     * @param fileName
     * @return
     */
    private String[] readContainerFile(DockerClient dockerClient, String containerId, String filepath, String fileName) {
        InputStream inputStream = dockerClient.copyArchiveFromContainerCmd(containerId, filepath)
                .exec();
        try (TarArchiveInputStream tarInputStream = new TarArchiveInputStream(inputStream)) {
            TarArchiveEntry entry;
            while ((entry = tarInputStream.getNextTarEntry()) != null) {
                if (entry.isFile() && entry.getName().equals(fileName)) {
                    // 读取文件内容
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = tarInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    return outputStream.toString().split(":");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 取出对应镜像
     * @param images
     * @param imageName
     * @return
     */
    private Image findImage(List<Image> images, String imageName) {
        return images
                .stream()
                .filter(image -> imageName.equals(image.getRepoTags()[0]))
                .findFirst()
                .orElse(null);
    }
}
