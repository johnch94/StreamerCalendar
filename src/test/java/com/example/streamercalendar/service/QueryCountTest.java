package com.example.streamercalendar.service;

import com.example.streamercalendar.domain.Platform;
import com.example.streamercalendar.domain.Source;
import com.example.streamercalendar.domain.StreamRecord;
import com.example.streamercalendar.domain.Streamer;
import com.example.streamercalendar.dto.StreamRecordResponse;
import com.example.streamercalendar.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 서비스 메서드가 실행하는 SQL 수를 Hibernate Statistics로 세어 N+1이 없는지 검증한다.
 * 로컬 PostgreSQL을 그대로 쓰고(contextLoads와 동일), 테스트마다 트랜잭션이 롤백되므로 데이터는 남지 않는다.
 * 스키마를 건드리지 않도록 ddl-auto=none으로 고정한다.
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({StreamRecordService.class, StreamerService.class})
class QueryCountTest {

    private static final int STREAMER_COUNT = 3;
    private static final int RECORDS_PER_STREAMER = 2;
    private static final int TEST_YEAR = 2099;

    @Autowired
    private EntityManager em;

    @Autowired
    private StreamRecordService streamRecordService;

    @Autowired
    private StreamerService streamerService;

    private Statistics statistics;
    private final List<Streamer> streamers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        for (int i = 0; i < STREAMER_COUNT; i++) {
            Streamer streamer = new Streamer("쿼리테스트-" + i, null);
            em.persist(streamer);
            streamers.add(streamer);
            for (int day = 1; day <= RECORDS_PER_STREAMER; day++) {
                em.persist(new StreamRecord(streamer, LocalDate.of(TEST_YEAR, 1, day), Platform.CHZZK,
                        "방송 " + day, null, null, Source.MANUAL));
            }
        }
        // 영속성 컨텍스트를 비워 실제 조회처럼 DB에서 다시 읽게 한다
        em.flush();
        em.clear();

        statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
    }

    @Test
    void 방송기록_목록_조회는_스트리머_수와_관계없이_쿼리_1번으로_끝난다() {
        List<StreamRecordResponse> result = streamRecordService.getStreamRecords(null, null, TEST_YEAR, 1);

        assertThat(result).hasSize(STREAMER_COUNT * RECORDS_PER_STREAMER);
        assertThat(result).allSatisfy(r -> assertThat(r.streamerName()).startsWith("쿼리테스트-"));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void 방송기록_상세_조회는_쿼리_1번으로_끝난다() {
        Long recordId = streamRecordService.getStreamRecords(streamers.get(0).getId(), null, null, null).get(0).id();
        em.clear();
        statistics.clear();

        StreamRecordResponse result = streamRecordService.getStreamRecord(recordId);

        assertThat(result.streamerName()).isEqualTo("쿼리테스트-0");
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void 스트리머_삭제는_방송기록_수와_관계없이_쿼리_2번으로_끝난다() {
        Long streamerId = streamers.get(0).getId();

        streamerService.deleteStreamer(streamerId);

        // 방송 기록 일괄 DELETE 1 + 스트리머 DELETE 1 (기록 건수만큼 DELETE가 나가지 않아야 함)
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        assertThat(em.find(Streamer.class, streamerId)).isNull();
        assertThat(streamRecordService.getStreamRecords(streamerId, null, null, null)).isEmpty();
    }

    @Test
    void 없는_스트리머를_삭제하면_404_예외가_나고_다른_데이터는_그대로다() {
        assertThatThrownBy(() -> streamerService.deleteStreamer(-1L))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(streamRecordService.getStreamRecords(null, null, TEST_YEAR, 1))
                .hasSize(STREAMER_COUNT * RECORDS_PER_STREAMER);
    }
}
