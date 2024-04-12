package com.code.codesandbox.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Lxy on 2024/4/12 19:29
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeConfig {
    private String code;
    private String language;
    private Integer memory;
    private Integer runtime;

}
