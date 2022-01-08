package komasin4.finance.upbit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@MapperScan(value= {"komasin4.finance.upbit.mapper"})
public class Komasin4UpbitApplication {

	public static void main(String[] args) {
		SpringApplication.run(Komasin4UpbitApplication.class, args);
	}
}
