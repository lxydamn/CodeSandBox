package com.code.codesandbox.service;

import com.code.codesandbox.pojo.CodeConfig;
import com.code.codesandbox.pojo.CodeResult;
import org.springframework.web.servlet.function.EntityResponse;

import java.io.IOException;

/**
 * Created by Lxy on 2024/4/12 15:42
 */
public interface CodeExecService {
    CodeResult ExecJavaCode(CodeConfig code) throws IOException;
}
