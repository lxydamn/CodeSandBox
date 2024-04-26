package com.code.codesandbox.service.utils;

import com.code.codesandbox.pojo.CodeConfig;
import com.code.codesandbox.pojo.CodeResult;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

/**
 * Created by Lxy on 2024/4/26 10:12
 */
public class CodeExecJob implements Callable<CodeResult> {

    private final CodeConfig config;
    private final String[] cmd;

    public CodeExecJob(CodeConfig config, String[] cmd) {
        this.config = config;
        this.cmd = cmd;
    }

    /**
     * running import code and get result  <br/>
     * @return code result  <br/>
     */
    public CodeResult execCode() {
        // 连接到Docker
        DockerClientConfig standard = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(config.getDockerApi())
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(standard.getDockerHost()).build();

        DockerClient dockerClient = DockerClientImpl.getInstance(standard, httpClient);

        Image image = CodeUtil.findImage(dockerClient.listImagesCmd().exec(), config.getDockerImage());

        // 创建容器并设定内存限制和运行时间限制
        String containerId = dockerClient.createContainerCmd(image.getId())
                .withHostConfig(HostConfig.newHostConfig().withMemory(config.getMemory() * 1024L * 1024))
                .withCmd(cmd) //将程序消耗输入文件中
                .withStopTimeout(config.getRuntime()) // limit code run time
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
        String fileContent = readContainerFile(
                dockerClient,
                containerId,
                "/home/consume.out",
                "consume.out");

        String[] strings = null;

        if (fileContent != null) {
            strings = fileContent.split(" ");
        }

        // 删除容器
        dockerClient.removeContainerCmd(containerId).exec();

        return new CodeResult(
                "success",
                outputStream.toString(),
                strings == null ? 0 : Double.parseDouble(strings[0]),
                strings == null ? 0d : Double.parseDouble(strings[1])
        );
    }

    /**
     * read consume of memory and time in docker
     * @param dockerClient docker client
     * @param containerId container id
     * @param fileName the file name
     * @return get output file content
     */
    private String readContainerFile(DockerClient dockerClient, String containerId, String filepath, String fileName) {
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

                    return outputStream.toString();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public CodeResult call() {
        return execCode();
    }
}
