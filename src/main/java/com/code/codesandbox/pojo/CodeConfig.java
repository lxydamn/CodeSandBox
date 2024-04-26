package com.code.codesandbox.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * code: user's code  <br/>
 * memory: limit of memory  <br/>
 * runtime: limit of the code execution time  <br/>
 * 2024 4 26  <br/>
 * written by lxy  <br/>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeConfig {
    private String code;
    private Integer memory;
    private Integer runtime;
    private String dockerApi;
    private String dockerImage;
}
