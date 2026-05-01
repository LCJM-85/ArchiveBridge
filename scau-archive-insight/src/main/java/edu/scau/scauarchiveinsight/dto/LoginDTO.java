package edu.scau.scauarchiveinsight.dto;

import lombok.Data;

/*
  登录请求参数
 */
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class LoginDTO implements Serializable {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "验证码不能为空")
    private String captcha;
}
