package com.code.codesandbox.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Lxy on 2024/4/12 19:36
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeResult {
    private String result;
    private Double runtime;
    private Double memory;
}
