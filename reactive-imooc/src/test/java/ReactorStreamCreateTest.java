import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Slf4j
public class ReactorStreamCreateTest {

    @Test
    public void fluxJust() {
        Flux<String> stockSequence1 = Flux.just("APPL", "AMZN", "TSLA");
        int a = 0;
    }

    @Test
    public void fluxFromIterable() {
        Flux<String> stockSeq2 = Flux.fromIterable(Arrays.asList("APPL", "AMZN", "TSLA"));
    }

    @Test
    public void fluxFromArray() {
        Flux<String> stockSeq3 = Flux.fromArray(new String[]{"APPL", "AMZN", "TSLA"});
    }

    @Test
    public void fluxFromStream() {
        Flux<String> stockSeq4 = Flux.fromStream(Stream.of("APPL", "AMZN", "TSLA"));
        stockSeq4.subscribe();
        stockSeq4.subscribe(); //can only subscribe once! will throw error
    }

    @Test
    public void fluxEmpty() {
        Flux<String> stockSeq5 = Flux.empty(); //generic type still honored
    }

    @Test
    public void fluxRange() {
        // 5, 6, 7
        Flux<Integer> numbers = Flux.range(5, 3);
        int a = 0;
    }

    @Test
    public void fluxGenerate() {
        //synchronous, one-by-one
        Flux<Long> flux = Flux.generate(
            AtomicLong::new,
            (state, sink) -> {
                long i = state.getAndIncrement();
                // sink序列号发送出去
                sink.next(i);
                if (i == 10) sink.complete();
                return state;
            },
            // 可以把数据库连接关闭等操作放在这里
            (state) -> System.out.println("I'm done")
        );
        flux.subscribe(System.out::println);
    }


    @Test
    public void fluxCreate() {
        Flux<String> stockSeq6 = Flux.create((t) -> {
            t.next("APPL");
            t.next("AMZN");
            t.complete();
        });

        // 更有意义的例子
        Flux<String> stockSeq7 = Flux.create(sink -> {
            //pretend we are registering a listener here
            new MyDataListener() {
                public void onReceiveData(String str) {
                    sink.next(str);
                }

                public void onComplete() {
                    sink.complete();
                }
            };
        },
            // 下游背压策略
            FluxSink.OverflowStrategy.DROP);
    }

    public class MyDataListener {
        public void onReceiveData(String str) {
            //do something
        }

        public void onComplete() {
            //do something
        }
    }

    @Test
    public void fluxDefer() {
        Flux.defer(() -> Flux.just("APPL", "AMZN", "TSLA"))
            .subscribe(System.out::println);

        //due to the nature of defer, fromStream will not throw exception now
        Flux<String> stockSeq4 = Flux.defer(() -> Flux.fromStream(Stream.of("APPL", "AMZN", "TSLA")));
        stockSeq4.subscribe();
        stockSeq4.subscribe();
    }

    @Test
    public void fluxInterval() throws InterruptedException {
        Flux.interval(Duration.of(1, ChronoUnit.SECONDS))
            .subscribe((t) -> log.info(String.valueOf(t)));
        log.info("Going to pause test thread, so that we don't end the test method before flux emits");
        Thread.sleep(10000);
    }
}
