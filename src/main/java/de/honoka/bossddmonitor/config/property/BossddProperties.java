package de.honoka.bossddmonitor.config.property;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.bossdd")
@Data
public class BossddProperties {
    
    @NotNull(message = "搜索关键词不能为空")
    private String searchWord;
    
    @NotNull(message = "城市代码不能为空")
    private String cityCode;
}
