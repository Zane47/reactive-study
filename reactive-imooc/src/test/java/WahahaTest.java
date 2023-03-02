import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * @author czhang27@trip.com
 * @date 2023/3/1
 */
@Slf4j
public class WahahaTest {



    @Test
    public void test() {
        long now = System.currentTimeMillis();
        Flux.range(1, 100)
                .log()
                .limitRate(1)
                .flatMap(this::times2)
                .subscribe(System.out::println);
        System.out.println(System.currentTimeMillis() - now);

        long start = System.currentTimeMillis();
        Flux.range(1, 100)
                .log()
                .flatMap(this::times2)
                // .limitRate(1)
                .subscribe(System.out::println);
        System.out.println(System.currentTimeMillis() - now);
    }



    private Mono<Integer> times2(int i) {
        return Mono.just(2 * i);
    }


}
