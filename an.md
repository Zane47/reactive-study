# 从异步IO说起



## BIO VS NIO

推荐阅读1：[面试官:都说阻塞 I/O 模型将会使线程休眠，为什么 Java 线程状态却是 RUNNABLE？](https://cloud.tencent.com/developer/article/1517734)

因此，阻塞IO能承载的并发量并不高，取决于线程池的容量而非机器的配置，单纯扩大线程池也只能获得有限的提升。

期望上我们希望CPU能获得最大限度地利用，理想情况下越高越好，而在实际应用中，应当保留一定的空间应对波动。

## IO密集 VS CPU密集

推荐阅读2：[线程池工作原理及参数计算](https://cloud.tencent.com/developer/article/1632213)

这里需要指出一个常见误区：存在大量的IO场景不一定就是IO密集型服务，具体取决于IO等待时间与CPU时间的比例，而阻塞IO的等待时间归属于CPU时间。

目前常用的IO场景中，仅有数据库IO只能选择阻塞IO（主要指MySql，其他DB例如MongoDB提供了异步API）。

# 背压

虽然异步IO解决了性能问题，但另一个问题随之而来——简陋的异步API及其带来的回调地狱。

JDK8引入的CompletableFuture基本上解决了原本的回调地狱问题，但它的API并不是那样丰富，稍微复杂一点的场景下，开发者仍旧需要自己进行封装。

并且，单纯的异步模式同样面临着一个致命的问题——背压（Backpressure），即上游的请求量大于下游的处理速度。

在系统设计上，我们通常引入消息队列来解决这一问题，通过将处理请求的控制权转移到下游，避免过多的上游请求造成雪崩。



# 理解Reactor

是一个基于观察者模式和reactive stream规范实现的框架

[响应式(流式)编程相关](http://conf.ctripcorp.com/pages/viewpage.action?pageId=300947368)念。

- ## 核心概念

  - 自底向上产生控制流
  - 自顶向下产生数据流

- ## 核心角色

  1. Publisher：生产者
     1. Flux：0-N个元素的Publisher，N可以是无限大（后文提供关于无限流和有限流的应用例子）
        1. Flux的语义是流，和Stream类似
     2. Mono：0-1个元素的Publisher
        1. Mono在英语中是一个词根，意为“单个的，一个的”
     3. Flux和Mono主要是1和N个元素的区别，对应的API会有一些细微的差别，但工作原理是相同的
  2. Subscriber：消费者
  3. Subscription：消费者对生产者的控制器
  4. Processor：生产者+消费者
  5. Sink：

- ## 核心步骤

  - Publisher的声明
    - 待处理的数据
  - Subscriber的声明
    - 数据如何处理
  - Subscription的建立
    - 建立关系
  - Subsription的发生
    - 下游向上游发起信号
  - 数据流动
    - Next
      - 上游向下游投递1个元素
    - Error
      - 异常信号，需要下游处理
    - Complete
      - 上游投递完毕所有元素，向下游发出信号
      - 由于无限流不会结束，故永远不会触发
  - 销毁

- ## 注意事项

  - 任何操作都需要是null-safe的，这一点和Stream不同
  - 对于成员可变类型的操作，需要生成新的实例（functional program）
  - 大多数时候，都需要对元素的个数进行预估，尤其是0个/1个/处理上限（reactor默认值是256），需要选用合适的API（下文会进行说明）

# 常用方法

## 创建 - 有限流 - 

### 从已有数据创建

```java
final String aString = "abc";
final String anotherString = "123";
final List<String> stringList = List.of(aString, anotherString);
 
// 单个元素
final Flux<String> stringFlux = Flux.just(aString);
final Mono<String> stringMono = Mono.just(aString);
 
// 多个元素
final Flux<String> stringArrayFlux = Flux.just(aString, anotherString);
final Flux<String> stringIterableFlux = Flux.fromIterable(stringList);
```

```java
// 元素不可为null
final String nullString = null;
final Optional<String> optional = Optional.ofNullable(nullString);
// 异常
final Flux<String> stringFlux = Flux.just(nullString);
final Mono<String> stringMono = Mono.just(nullString);
 
// null-safe的创建方式
final Mono<String> nullMono = Mono.justOrEmpty(nullString);
final Mono<String> optionalMono = Mono.justOrEmpty(optional);
```

```java
// generate提供的API是SynchronousSink，必须在同一个线程中生成元素
//
Flux.generate(sink -> {
    sink.next(1);
    sink.complete();
});
 
// 这种方式可以生成多个元素
final Queue<Integer> strings = new LinkedList<>();
strings.offer(1);
strings.offer(2);
strings.offer(3);
// 第一个参数是生成第一个元素的方式
// 第二个参数是对每个元素的处理，并控制元素的流动或终止，并且生成下一个元素
// 本例生成[1,2,3]
Flux.<String, Integer>generate(strings::poll, (element, sink) -> {
    // 根据当前元素是否为null，判断是否中止
    Optional.ofNullable(element)
            // 对元素进行处理
            .map(String::valueOf)
            .ifPresentOrElse(sink::next, sink::complete);
    // 生成下一个元素
    return strings.poll();
});
 
// 其他逻辑不变，更换generate的第一个参数
// 这个例子生成[0,1,2,3]
final Queue<Integer> strings = new LinkedList<>();
strings.offer(1);
strings.offer(2);
strings.offer(3);
Flux.<String, Integer>generate(()-> 0, (element, sink) -> {
    Optional.ofNullable(element)
            .map(String::valueOf)
            .ifPresentOrElse(sink::next, sink::complete);
    return strings.poll();
});
```

### 从异步数据创建

```java
// 从CompletableFuture获取
final CompletableFuture<String> completableFuture = completableFuture();
final Mono<String> fromCompletableFuture = Mono.fromFuture(completableFuture);
 
// 从ListenableFuture获取
final ListenableFuture<String> listenableFuture = listenableFuture();
final Mono<String> fromListenableFuture = Mono.create(sink ->
    Futures.addCallback(listenableFuture, new FutureCallback<>() {
        @Override
        public void onSuccess(final String result) {
            sink.success(result);
        }
 
        @Override
        public void onFailure(final Throwable t) {
             sink.error(t);
        }
    }));
```

