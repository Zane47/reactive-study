package delay;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author czhang27@trip.com
 * @date 2023/3/29
 */
public class DelaySubscriptionTest {

    @Test
    public void emptySources() {
        AtomicBoolean cancelled = new AtomicBoolean();
        Mono<String> empty1 = Mono.empty();
        Mono<String> empty2 = Mono.empty();

        final Mono<String> empty3 = Mono.<String>empty()
                .delaySubscription(Duration.ofMillis(10))
                .doOnCancel(() -> cancelled.set(true));

        Duration d = StepVerifier.create(
                        Mono.zip(empty1, empty2, empty3))
                .verifyComplete();


        assert cancelled.get();

        assert d.compareTo(Duration.ofMillis(500)) < 0;
    }

}
