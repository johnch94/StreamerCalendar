package com.example.streamercalendar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 관리자 계정은 로컬 secret 파일 유무와 관계없이 테스트용 값으로 고정한다
@SpringBootTest(properties = {
		"app.admin.username=test-admin",
		"app.admin.password={noop}test-password",
		"app.admin.remember-me-key=test-remember-me-key"
})
class StreamercalendarApplicationTests {

	@Test
	void contextLoads() {
	}

}
