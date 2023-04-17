import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.SQLOutput;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author czhang27@trip.com
 * @date 2023/3/1
 */
@Slf4j
public class WahahaTest {


    @Test
    public void subscribe1() {
        Flux.range(1, 100)
                .log()
                .limitRate(10)
                .subscribe(System.out::println);
    }

    @Test
    public void subscribe2() {
        Flux.range(1, 100).log()
                .map(i -> {
                    if (i <= 3) {
                        return i * 2;
                    }
                    throw new RuntimeException("test 4");
                })
                .subscribe(System.out::println, error -> System.out.println("error: " + error));
    }


    /**
     * 错误信号和完成信号都是终止事件，并且彼此互斥（你永远不会同时得到这两个信号）
     */
    @Test
    public void subscribe3() {
        Flux.range(1, 4)
                .subscribe(System.out::println,
                        error -> System.err.println("Error " + error),
                        () -> System.out.println("Done"));
    }

    /**
     * 表示我们最多希望从源（实际上会发出10个元素）中接收到 5 个元素。
     */
    @Test
    public void subscribe4() {
        Flux.range(1, 10)
                .subscribe(System.out::println,
                        error -> System.err.println("Error " + error),
                        () -> System.out.println("Done"),
                        sub -> sub.request(5));
    }



    @Test
    public void test() {
        long now = System.currentTimeMillis();

        // Flux.interval()


        System.out.println("------------------------------");

        Flux.range(1, 100)
                .limitRate(1)
                .log()
                .flatMap(this::times2)
                .delayElements(Duration.ofMillis(1000))
                .subscribe(System.out::println);
        System.out.println(System.currentTimeMillis() - now);

        /*long start = System.currentTimeMillis();
        Flux.range(1, 100)
                .log()
                .flatMap(this::times2)
                // .limitRate(1)
                .subscribe(System.out::println);
        System.out.println(System.currentTimeMillis() - now);*/
    }


    @Test
    public void testDelay() {
        List<Integer> ints = new ArrayList<>();
        ints.add(1);
        ints.add(2);
        ints.add(3);
        ints.add(4);
        ints.add(5);

        // ------------------------ Flux ------------------------

        /*Flux.defer(() -> Flux.fromIterable(ints)
                .delayElements(Duration.ofSeconds(2))
                .flatMap(i -> {
                    System.out.println(System.currentTimeMillis());
                    return Mono.just(i * 2);
                })).subscribe();*/

        Flux.fromIterable(ints)
                // .delayElements(Duration.ofSeconds(2))
                .flatMap(i -> {
                    System.out.println(System.currentTimeMillis());
                    return Mono.just(i * 2);
                })
                .delaySubscription(Duration.ofSeconds(2))
                .subscribe();

        // ------------------------ mono.delay -> wrong ------------------------
        /*for (final Integer num : ints) {
            Mono.delay(Duration.ofSeconds(1))
                    .flatMap(i -> {
                        System.out.println("in: " + System.currentTimeMillis());
                        return Mono.just(num * 2);
                    })
                    .log()
                    .subscribe();

            System.out.println("out: " + System.currentTimeMillis());

        }*/



    }


    private Mono<Integer> times2(int i) {
        return Mono.just(2 * i);
    }


}
