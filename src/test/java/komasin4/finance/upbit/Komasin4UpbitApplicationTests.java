package komasin4.finance.upbit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class Komasin4UpbitApplicationTests {
	
	@Test
	void contextLoads() {
		TestController test = new TestController();
		test.main(null);
	}

}
