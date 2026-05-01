import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Encode {
    public static void main(String[] args) {
        // BCrypt 加密器（Spring 内置，无需额外依赖）
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "123456"; // 明文密码
        String encodedPassword = encoder.encode(rawPassword); // 加密后的密码

        System.out.println("加密后的密码：" + encodedPassword);
        // 示例输出：$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
    }
}