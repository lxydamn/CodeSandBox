package com.code.codesandbox.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Lxy on 2024/4/27 10:02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserConfig {
    private String code;
    private String input;
    private Integer memory;
    private Integer runtime;
}
