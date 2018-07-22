# JVM Memory Structure (JVM内存结构)

![image](https://github.com/Ziang-Lu/Java-Memory-Management/blob/master/java_memory_distribution.png?raw=true)

Notes:

* 上图由JVM规范定义, 不同的JVM实现可能会稍有不同, 但总体上还是遵循规范的.
* Method Area只是一种概念上的区域, 并说明了其应该具有什么功能, 但是并没有严格规定这个区域到底应该处于何处. 所以对于不同的JVM实现来说, 是有一定的自由度的.

<br>

# Java Memory Model (Java内存模型)

## 由硬件开始说起: 计算机内存模型

首先看一个简化的**双核CPU的缓存结构**:

<img src="https://github.com/Ziang-Lu/Java-Memory-Management/blob/master/2-core_cpu_cache.png?raw=true" width="400px">

让我们来看一看single-thread / multi-threading对其的影响:

* Single-thread

  * **缓存只被一个thread访问, 不会出现访问冲突.**

* Multi-threading

  * 单核CPU

    * Process中的多个thread会同时访问process中的共享数据.

    * CPU将某块内存加载到缓存后, 不同thread在访问相同的物理地址的时候, 都会映射到相同的缓存位置, 这样**即使发生thread的切换, 缓存仍然不会失效**.

      *即多个thread共享缓存*

    * **由于任何时刻只能有一个thread在执行, 因此不会出现缓存的访问冲突.**

  * 多核CPU

    * 若多个thread分别在不同的核上执行 (每个核都至少有一个L1缓存), 当这些thread访问process中某个共享内存的时候, 每个核都会在其各自的缓存中保留一份共享内存的拷贝.

    * **由于多核之间是可以parallel执行的, 可能会出现多个thread同时写各自缓存的情况, 而各自的缓存之间的数据就有可能不同.**

    * 即每个核自己的缓存中, 关于同一个数据的缓存内容可能不一致.

      => **Cache Consistency Problem (缓存一致性问题)**

**总结: 在CPU和主存之间增加缓存, 在多线程场景下会出现缓存一致性问题.**

<br>

## Concurrent Programming Requirements

由缓存一致性问题、Processor Optimization Problem (处理器优化问题) (by processor) 和Instruction Reordering Problem (指令重排问题) (by modern compilers) 等底层问题, 可以得到在**concurrent programming中, 为了保证数据的安全, 需要满足以下三个特性**:

* **Sequence (顺序性)**

  程序执行的顺序按照代码的先后顺序执行

  *本质上就是指令重排问题*

* **Atomicity (原子性)**

  在一个操作中CPU不可以在中途暂停然后再调度, 即不被中断操作: 要么执行完成, 要么根本就不执行

* **Visibility (可见性)**

  当多个thread访问同一个变量时, 一个thread修改了这个变量的值, 其他thread能够立即看得到修改后的值

  *本质上就是缓存一致性问题*

**总结: 为了保证concurrent programming中可以满足顺序性、原子性以及可见性, 内存模型诞生了.**

<br>

## Memory Model

为了保证共享内存的正确性 (顺序性、原子性、可见性), **内存模型定义了共享内存系统中multi-threading的读写操作的行为规范. 通过这些规则来规范来对共享内存的读写操作, 从而保证指令执行的正确性.**

它解决了CPU多级缓存、处理器优化、指令重排等导致的内存访问冲突问题, 保证了concurrent场景下的顺序性、原子性和可见性.

<br>

## Java Memory Model (JMM) — Java中Memory Model的具体实现

* Java的multi-threading之间是通过共享内存进行通信的, 而这也会导致一系列问题例如上述的顺序性、原子性、可见性等问题.
* **JMM就是一种符合内存模型规范的, 屏蔽了各种硬件和操作系统的访问差异的, 保证了Java程序在各种平台下对内存的访问都能保证效果一直的机制和规范.**
* 目的是保证concurrent场景下的顺序性、原子性和可见性.

<br>

JMM规定了所有的变量都存储在主内存中, 每条thread还有自己的工作内存 (图中"本地内存"), thread的工作内存中保存了该thread中使用到的变量的主内存副本拷贝; thread对变量的所有操作都必须在工作内存中进行, 而不能直接对主内存进行读写; 不同的thread之间也无法直接访问对方工作内存中的变量, thread间变量的传递均需要自己的工作内存和主内存之间进行数据同步来进行.

如下图所示:

<img src="https://github.com/Ziang-Lu/Java-Memory-Management/blob/master/java_memory_model_(jmm).png?raw=true" width="500px">

<br>

JMM定义了一些语法集, 这些语法集映射到Java语言中就是`synchronized`、`volatile`等关键字. 这些就是JMM封装了底层实现之后提供给开发者使用的一些关键字.

Concurrent programming要解决有序性、原子性和一致性问题, 那么在Java中, 分别使用什么方式来保证?

* 顺序性

  * `synchronized` 来保证同一时刻只允许一条thread进行操作
  * `volatile` 禁止指令重排

* 原子性

  * `synchronized` 来保证同一时刻只允许一条thread进行操作, 进而保证方法和代码块内的操作是原子性的
    * 专门解决原子性问题

* 可见性

  * 在一个thread修改变量之后, 将新值同步回主内存; 另一个thread在从主内存读取前, 刷新变量值

    *即依赖主内存作为传递媒介*

  * **`volatile` 被其修饰的变量在被修改之后可以立即同步到主内存, 被其修饰的变量在每次使用之前都从主内存刷新**

    *即实现了不同thread之间同步该被`volatile`修饰的变量*

    * 专门解决可见性问题

    * 缺点是由于实现了变量的值的同步, 性能会相应有所缺失

      Note: 有时multi-threading的场景下, 即使不使用`volatile`, 操作系统也会实现可见性, 但这不是guaranteed的; 而使用`volatile`可以guarantee可见性.

    * Check out `VolatileDemo.java`

  * `synchronized` 也可实现可见性

Note:

看似`synchronized`是万能的, 它可以同时满足以上三种特性, 这其实也是很多人滥用`synchronized`的原因.

但是`synchronized`是比较影响性能的, 虽然编译器提供了很多locking optimization技术, 也不建议过度使用.

<br>

# Java对象模型

和Java对象在虚拟机中的表现形式有关, 指的是Java对象在内存中的存储的结构

