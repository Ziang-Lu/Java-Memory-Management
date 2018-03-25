# Garbage Collection in Java

GC算法:

1. 发现无用的信息对象
2. 回收被无用对象占用的内存空间, 使该空间可被程序再次使用
   * 何时回收这些对象
   * 采用什么样的方式回收

<br>

## 1. 引用计数法 (Reference Counting Collector)

### Algorithm:

Heap中每个对象实例都有一个引用计数器

=> 任何引用计数为0的对象实例可以当做垃圾回收

#### Pros:

可以很快执行、交织在程序运行中; 对需要不被长时间打断的实时环境的程序比较有利

#### Cons:

无法检测出循环引用: 比如父对象有一个对子对象的引用, 子对象反过来引用父对象, 这样他们的引用计数永远不可能为0, 则永远不可能被当做垃圾回收

```java
public class ReferenceCountingGC {
  /**
   * 1 MB = 1024 * 1024 bytes
   */
  private static final int _1MB = 1024 * 1024;

  private Object instance;
  /**
   * Only used to take some memory.
   */
  private byte[] bigSize = new byte[2 * _1MB];

  public static void main(String[] args) {
    ReferenceCountingGC obj1 = new ReferenceCountingGC();
    ReferenceCountingGC obj2 = new ReferenceCountingGC();
    obj1.instance = obj2;
    obj2.instance = obj1;

    obj1 = null;
    obj2 = null;

    System.gc();
    // 两个对象互相引用着, 但垃圾回收器还是把它们回收了, 说明JVM使用的不是引用计数法
  }
}
```

<br>

## 2. 根搜索算法 (Tracing Collector)

### Algorithm:

程序把所有的引用关系看作一张图, 从第一个节点GCRoot开始, 寻找对应的引用节点, 找到这个节点以后, 继续寻找这个节点的引用节点; 当所有的引用节点寻找完毕之后, 剩余的节点则被人问是没有被引用到的节点, 即无用的节点 (如下图所示)

*[本质上是多个DFS]*

可作为GCRoot的对象有

1. Method Area中静态属性引用的对象

   ```java
   private static MyObject obj = new MyObject();
   ```

2. Method Area中常量引用的对象

   ```java
   private final MyObject obj = new MyObject();
   ```

3. JVM栈中引用的对象 (本地变量表)

4. 本地方法栈中JNI引用的对象 (Naive对象) (本地变量表) *(平时很少涉及)*

![image](https://github.com/Ziang-Lu/Java-Memory-Management/blob/master/Java%20Garbage%20Collection/graph_searching.png?raw=true)

如上图所示, obj8,、obj9和obj10都没有到GCRoots对象的引用链, 则即便obj9和obj10之间有循环引用, 它们还是会被当成垃圾处理, 可以进行回收

<br>

#### finalize()方法

finalize()方法在对象被垃圾回收器析构之前调用, 它用来制定对象销毁时候要执行的操作, 并清除回收对象. 例如, 你可以使用finalize()来确保一个对象打开的文件被关闭了

```java
public class FinalizeDemo {

  private static class Cake {
    private int id;
    public Cake(int id) {
      this.id = id;
      System.out.println("Cake object " + id + " is created");
    }

    @Override
    protected void finalize() throws Throwable {
      System.out.println("Cake object " + id + " is disposed");
    }
  }

  public static void main() {
    Cake c1 = new Cake(1); // Cake object 1 is created
    Cake c2 = new Cake(2); // Cake object 2 is created
    Cake c3 = new Cake(3); // Cake object 3 is created

    c2 = null;
    c3 = null;
    System.gc(); // 显示调用full GC, 此次垃圾回收只会回收c2和c3
    // Output:
    // Cake object 2 is disposed
    // Cake object 3 is disposed
  }

}
```

<br>

#### 实际的标记过程: 两次标记-筛选与finalize()方法

对于追踪算法而言, 未到达的对象并非是"非死不可"的, 若要宣判一个对象的死亡, 则至少需要经历两次标记阶段

1. 如果对象进行追踪算法后发现没有与GCRoots相连的引用链, 则该对象被第一次标记, 并进行第一次筛选, 筛选条件为是否有必要执行该对象的finalize()方法

   若对象没有override finalize()方法, 或者finalize()方法已经被JVM执行过了, 则被认为是没有必要执行该对象的finalize()方法, 则该对象会被直接回收

   若对象override了finalize()方法且还未被JVM执行过, 那么这个对象会被放置在一个叫F-Queue的队列中等待第二次标记和筛选

2. 对F-Queue中的对象进行第二次标记和筛选

   如果对象在其finalize()方法中"拯救了自己", 即成功关联上了GCRoots引用链, 那么在第二次标记时该对象将从"即将回收"的集合中移除

   如果对象还是没能"拯救自己", 那就会被回收

   *注意这个"自救"的机会只有一次*

```java
public class FinalizeEscapeDemo {

  private static FinalizeEscapeDemo SAVE_LOOK;

  private string name;

  public FinalizeEscapeDemo(String name) {
    this.name = name;
  }

  public void isAlive() {
    System.out.println("yes, i am still alive :)");
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    System.out.println("finalize() method executed");
    System.out.println(this);
    // 对象在finalize()方法中"拯救了自己", 即成功关联上了GCRoots引用链, 则在第二次标记时此对象将从"即将回收"的集合中移除
    SAVE_HOOK = this;
  }

  public static void main(String[] args) throws InterruptedException {
    SAVE_HOOK = new FinalizeEscapeDemo("ziang");
    System.out.println(SAVE_HOOK); // ziang

    // 对象第一次"自救"
    SAVE_HOOK = null;
    System.out.println(SAVE_HOOK); // null
    System.gc(); // 显示调用full GC, 会先call新的对象的finalize()方法
    // Output:
    // finalize() method executed
    // ziang
    // 相当于在第一次标记-筛选中被筛选出来, 但是由于在finalize()方法中成功"自救", 则成功在第二次标记-筛选时被标记上而未被清除

    // 因为finalize()方法优先级很低, 所以暂停0.5秒以等待它
    Thread.sleep(500);
    if (SAVE_HOOK != null) {
      SAVE_HOOK.isAlive(); // yes, i am still alive :)
    } else {
      System.out.println("no, i am dead :(");
    }

    // 对象第二次"自救"失败, 因为finalize()方法只能被调用一次
    SAVE_HOOK = null;
    System.out.println(SAVE_HOOK); // null
    System.gc(); // 显示调用full GC, 会先call新的对象的finalize()方法
    Thread.sleep(500);
    if (SAVE_HOOK != null) {
      SAVE_HOOK.isAlive();
    } else {
      System.out.println("no, i am dead :("); // no, i am dead :(
    }
  }

}
```

<br>

## 3. 标记-清除算法 (Mark-and-Sweep Collector)

### Algorithm:

1. 当程序运行期间, 若可以使用的内存被耗尽的时候, GC线程就会被触发并将程序暂停
2. 从根集合扫描, 对存活的对象进行标记
3. 标记完毕后, 再扫描整个空间中未标记的对象, 进行回收
4. 程序恢复运行

*为什么要暂停程序?*

*假设完成标记之后, 程序从某个被标记过的对象又new了一个新的对象出来, 但是由于这个新的对象"错过"了标记阶段, 它将在清除阶段被清除, 导致GC线程无法正确工作*

![image](https://github.com/Ziang-Lu/Java-Memory-Management/blob/master/Java%20Garbage%20Collection/mark-and-sweep_collector_illustration.jpg?raw=true)

#### Pros:

1. 不需要进行对象的移动
2. 仅对不存活的对象进行处理, 在存活对象比较多、不存活对象比较少的情况下极为高效

#### Cons:

1. 在进行GC的时候, 需要暂停程序, 导致用户体验不好, 不适用于交互式应用程序
2. 由于直接回收不存活对象, 因此会造成内存碎片 (如上图所示)

<br>

## 3. 标记-整理算法 (Mark-and-Compact Collector)

### Algorithm:

类似标记-清除算法, 但在清除时回收不存活对象占用的空间后, 会将所有的存货对象往左端空闲空间移动, 并更新对应的指针 (如下图所示)

![image](https://github.com/Ziang-Lu/Java-Memory-Management/blob/master/Java%20Garbage%20Collection/mark-and-compact_collector_illustration.jpg?raw=true)

#### Pros:

对比标记-清除算法, 解决了内存碎片的问题

#### Cons:

由于在清除时进行了对象的移动, 因此成本更高 (如上图所示)

<br>

## 4. 停止-复制算法 (Stop-and-Copy Collector)

### Algorithm:

把heap分成一个对象面和多个空闲区域面

1. 程序从对象面为对象分配空间
2. 当对象面满了, JVM将暂停程序进行, 开启复制算法GC线程
3. 垃圾回收系统就从根集合开始扫描活动对象, 并将每个活动对象复制到空闲面 (使得活动对象所占的内存之间没有空洞), 再清除原来的对象面
4. 这样对象面和空闲面就完成了互换
5. 程序恢复, 会在新的对象面(原来的空闲面)中分配内存 (如下图所示)

![image](https://github.com/Ziang-Lu/Java-Memory-Management/blob/master/Java%20Garbage%20Collection/stop-and-copy_collector_illustration.jpg?raw=true)

#### Pros:

1. 解决了两种标记算法中和内存碎片问题和对象移动造成的成本问题
2. 仅对存活的对象进行处理, 在存活对象比较少、不存活对象比较多的情况下极为高效

#### Cons:

内存缩小为了原来的一半

<br>

## 5. 分代回收算法 (Generation Collector)

分代的垃圾回收策略, 是基于这样的一个事实: 不同对象的生命周期是不一样的; 因此, 不同生命周期的对象可以采用不同的回收算法, 以便提高效率

*[本质上是上述各种方法的结合]*

分代如下图所示

![image](https://github.com/Ziang-Lu/Java-Memory-Management/blob/master/Java%20Garbage%20Collection/generation_collector_illustration.png?raw=true)

1. 新生代 (Young Generation)

   新生代内存按照8:1:1的比例 (即上图中SurvivorRatio = $n$ = 8) 分为一个Eden区和两个Survivor区 (上图中的From Space和To Space)

   对象在Eden区中生成

   * 所有新生成的对象首先多事放在新生代的, 新生代的目标就是尽可能快速地回收掉那些生命周期短的对象 (大批对象死去, 少量对象存活)
   * 回收时 *(Minor GC, 不一定等Eden区满了才触发)* 先将Eden区存活对象复制到一个Survivor 0区, 然后清空Eden区
   * 当Survivor 0区也存放满了时, 则将Eden区和Survivor 0区存活对象复制到另一个Survivor 1区, 然后清空Eden区和Survivor 0区, 然后交换Survivor 0区和Survivor 1区, 保证Survivor 1区是空的
   * 如此往复
   * 当Survivor 1区不足以存放Eden区和Survivor 0区的存货对象时, 就将存货对象直接存放到年老代

   只浪费了10%的内存, 这个是可以接受的, 因为我们换来的内存的整齐排列与GC速度

   *新生代发生的GC也叫做Minor GC, 发生频率比较高, 不一定等Eden区满了才触发*

2. 年老代 (Tenured Generation)

   内存比新生代大得多 (大概比例是1:2, 即上图中NewRatio = 2)

   * 在新生代中经历了N次垃圾回收后仍然存活的对象, 就会被放到年老代中; 因此, 可以认为年老代中存放的都是一些生命周期较长的对象 (对象存活率高)
   * 当年老代内存满了时, 会触发一次Major GC即Full GC, 也就是对新生代、年老代都进行回收

3. 持久代 (Permanent Generation)

   用于存放静态文件, 如Java类、方法等

### Algorithm:

1. 新生代

   由于大批对象死去, 少量对象存活, 因此需要效率高的算法; 此外, 新生代有额外空间进行分配担保

   => **停止-复制算法**

2. 年老代

   由于对象存活率高; 此外, 年老代没有额外空间进行分配担保

   => **标记-清除算法** 或 **标记-整理算法**

3. 持久代

   类似于年老代

   => **标记-清除算法** 或 **标记-整理算法**

<br>

## Garbage Collector Implementations

HotSpot JVM实现的垃圾回收器如下图所示

*红色的是**串行 (serial)** 收集器, 绿色的是**并行 (parallel)** 收集器, 黄色的是**并发 (concurrent)** 收集器*

*如果两个回收器之间有连线, 则说明它们可以搭配使用*

![image](https://github.com/Ziang-Lu/Java-Memory-Management/blob/master/Java%20Garbage%20Collection/HotSpot_garbage_collectors.jpg?raw=true)

术语:

吞吐量 = 用户线程时间 / (用户线程时间 + GC线程时间)

* Serial收集器 (**停止-复制算法**)

  **新生代-单线程**收集器, 标记和清除都是单线程, 简单高效

* ParNew收集器 (**停止-复制算法**)

  **新生代-多线程**收集器, 可以认为是Serial收集器的多线程版本, 在多核CPU环境 (**=> parallelism**) 下有着比Serial收集器更好的表现

* Parallel Scavenge (**停止-复制算法**)

  **新生代-并行 (parallel)** 收集器, 追求高吞吐量 (一般为99%), 高效利用CPU

  适合后台应用等对交互要求不高的场景

* Serial Old收集器 (**标记-整理算法**)

  **年老代-单线程**收集器, Serial收集器的年老代版本

* Parallel Old (**标记-整理算法**)

  **年老代-多线程**收集器, Parallel Scavenge收集器的年老代版本, 吞吐量优先

* Concurrent Mark Sweep (**标记-清除算法**)

  高**并发 (concurrent)**、低停顿, 响应时间快, 停顿时间短, 追求最短GC回收停顿时间; CPU占用比较高; 多核CPU (**=> parallelism**) 追求快速响应时间的选择

<br>

## Java有了GC同样会出现内存泄漏问题

1. 各种连接, IO连接、网络连接、数据库连接等没有显示调用close()方法关闭, 不被GC回收导致内存泄漏
2. 监听器的使用, 在释放对象的同事没有相应删除监听器的时候也有可能导致内存泄漏