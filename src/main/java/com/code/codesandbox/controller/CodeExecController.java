package com.code.codesandbox.controller;

import com.code.codesandbox.pojo.CodeConfig;
import com.code.codesandbox.pojo.CodeResult;
import com.code.codesandbox.service.CodeExecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Lxy on 2024/4/12 15:36
 */
@RestController
@RequestMapping("/api/code")
public class CodeExecController {

    @Autowired
    private CodeExecService codeExecService;

    @PostMapping("/java")
    public ResponseEntity<CodeResult> getCodeExecResult(@RequestBody CodeConfig data) {

        try {
            return ResponseEntity.ok(codeExecService.ExecJavaCode(data));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
