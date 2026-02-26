package java.lang;

/**
 * A thread is a thread of execution in a program. The Java Virtual Machine
 * allows an application to have multiple threads of execution running
 * concurrently.
 *
 * Every thread has a priority. Threads with higher priority are executed
 * in preference to threads with lower priority.
 */
public class Thread implements Runnable {
    /**
     * The minimum priority that a thread can have. The value is 1.
     */
    public static final int MIN_PRIORITY = 1;
    /**
     * The default priority that is assigned to a thread.
     */
    public static final int NORM_PRIORITY = 5;
    /**
     * The maximum priority that a thread can have. The value is 10.
     */
    public static final int MAX_PRIORITY = 10;
    private static int threadInitNumber;
    private volatile String name;
    private int priority;
    private Thread threadQ;
    private long eetop;
    private boolean single_step;
    private boolean daemon = false;
    private Runnable target;
    private ThreadGroup group;
    private ClassLoader contextClassLoader;
    /**
     * The current state of this thread.
     */
    private int threadStatus = 0;

    /**
     * Allocates a new Thread object. This constructor has the same effect
     * as Thread (null, null, gname), where gname is a newly generated name.
     */
    public Thread() {
        init(null, null, "Thread-" + nextThreadNum(), 0);
    }

    /**
     * Allocates a new Thread object. This constructor has the same effect
     * as Thread (group, target, gname) ,where gname is a newly generated name.
     */
    public Thread(Runnable target) {
        init(null, target, "Thread-" + nextThreadNum(), 0);
    }

    /**
     * Allocates a new Thread object. This constructor has the same effect
     * as Thread (null, target, name).
     */
    public Thread(String name) {
        init(null, null, name, 0);
    }

    /**
     * Allocates a new Thread object. This constructor has the same effect
     * as Thread (group, target, name), where target is the newly created
     * runnable object whose run method is called when this thread is started.
     */
    public Thread(ThreadGroup group, Runnable target) {
        init(group, target, "Thread-" + nextThreadNum(), 0);
    }

    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }

    /**
     * Returns the current thread
     */
    public static native Thread currentThread();

    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds.
     */
    public static native void sleep(long millis) throws InterruptedException;

    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds plus the specified
     * number of nanoseconds.
     */
    public static void sleep(long millis, int nanos) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                                "nanosecond timeout value out of range");
        }

        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }

        sleep(millis);
    }

    /**
     * Returns the current number of active threads in the virtual machine.
     */
    public static int activeCount() {
        return currentThread().getThreadGroup().activeCount();
    }

    /**
     * Copies into the specified array every active thread in the current
     * thread's thread group and its subgroups.
     */
    public static int enumerate(Thread tarray[]) {
        return currentThread().getThreadGroup().enumerate(tarray);
    }

    /**
     * Prints a stack trace of the current thread to the standard error stream.
     */
    public static void dumpStack() {
        new Exception("Stack trace").printStackTrace();
    }

    /**
     * Returns an estimate of the number of active threads in the current
     * thread group and its subgroups.
     */
    public static int enumerate(Thread tarray[]) {
        return currentThread().getThreadGroup().enumerate(tarray);
    }

    private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize) {
        Thread parent = currentThread();
        this.name = name;
        this.target = target;
        setPriority(parent.getPriority());
    }

    /**
     * Causes this thread to begin execution; the Java Virtual Machine
     * calls the run method of this thread.
     *
     * The result is that two threads are running concurrently: the current
     * thread (which returns from the call to the start method) and the other
     * thread (which executes its run method).
     */
    public synchronized void start() {
        if (threadStatus != 0)
            throw new IllegalThreadStateException();
        start0();
    }

    private native void start0();

    /**
     * If this thread was constructed using a separate
     * Runnable run object, then that
     * Runnable object's run method is called;
     * otherwise, this method does nothing and returns.
     */
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    /**
     * This method is called by the system to give a Thread
     * a chance to clean up before it actually exits.
     */
    private void exit() {
        if (group != null) {
            group.threadTerminated(this);
            group = null;
        }
    }

    /**
     * Returns this thread's name.
     */
    public final String getName() {
        return name;
    }

    /**
     * Changes the name of this thread to be equal to the argument name.
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the number of stack frames in this thread.
     */
    public native int countStackFrames();

    /**
     * Waits at most millis milliseconds for this thread to die.
     */
    public final synchronized void join(long millis)
    throws InterruptedException {
        long base = System.currentTimeMillis();
        long now = 0;

        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (millis == 0) {
            while (isAlive()) {
                wait();
            }
        } else {
            while (isAlive()) {
                long delay = millis - now;
                if (delay <= 0) {
                    break;
                }
                wait(delay);
                now = System.currentTimeMillis() - base;
            }
        }
    }

    /**
     * Waits at most millis milliseconds plus nanos nanoseconds for this
     * thread to die.
     */
    public final synchronized void join(long millis, int nanos)
    throws InterruptedException {

        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if ((nanos < 0) || (nanos > 999999)) {
            throw new IllegalArgumentException(
                                "nanosecond timeout value out of range");
        }

        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }

        join(millis);
    }

    /**
     * Waits for this thread to die.
     */
    public final void join() throws InterruptedException {
        join(0);
    }

    /**
     * Tests if this thread is a daemon thread.
     */
    public final boolean isDaemon() {
        return daemon;
    }

    /**
     * Marks this thread as either a daemon thread or a user thread.
     */
    public final void setDaemon(boolean on) {
        if (isAlive())
            throw new IllegalThreadStateException();
        daemon = on;
    }

    /**
     * Returns this thread's priority.
     */
    public final int getPriority() {
        return priority;
    }

    /**
     * Changes the priority of this thread.
     */
    public final void setPriority(int newPriority) {
        ThreadGroup g;
        if (newPriority > MAX_PRIORITY || newPriority < MIN_PRIORITY) {
            throw new IllegalArgumentException();
        }
        if((g = getThreadGroup()) != null) {
            if (newPriority > g.getMaxPriority()) {
                newPriority = g.getMaxPriority();
            }
            setPriority0(priority = newPriority);
        }
    }

    /**
     * Returns this thread's thread group.
     */
    public final ThreadGroup getThreadGroup() {
        return group;
    }

    /**
     * Tests whether this thread is alive. A thread is alive if it has
     * been started and has not yet died.
     */
    public final native boolean isAlive();

    /**
     * Suspends this thread.
     */
    public final void suspend() {
        suspend0();
    }

    /**
     * Resumes a thread that has been suspended.
     */
    public final void resume() {
        resume0();
    }

    /**
     * Changes the priority of this thread.
     */
    private native void setPriority0(int newPriority);

    private native void suspend0();

    private native void resume0();
}

