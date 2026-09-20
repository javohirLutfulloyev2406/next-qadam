package uz.nextqadam.bot;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import uz.nextqadam.bot.common.util.TimeUtil;

@SpringBootApplication
public class NextQadamApplication {

    public static void main(String[] args) {
        SpringApplication.run(NextQadamApplication.class, args);
    }

    /**
     * JVM darajasidagi qo'shimcha xavfsizlik qatlami — zone ko'rsatilmagan LocalDate.now()/
     * LocalDateTime.now() kabi chaqiruvlar ham to'g'ri ishlashi uchun. Asosiy hisob-kitoblar
     * baribir TimeUtil.TASHKENT_ZONE bilan aniq qilinishi kerak.
     */
    @PostConstruct
    public void setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(TimeUtil.TASHKENT_ZONE));
    }
}

