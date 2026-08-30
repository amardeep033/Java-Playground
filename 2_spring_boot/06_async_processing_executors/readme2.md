# Sync vs Async vs Blocking: Mental Model

Self-study notes. Core idea: these are **separate questions** about the same operation. Most confusion comes from mixing them up.

---

## 1. Two kinds of work

**CPU-bound**: time goes on *computing* (parse, hash, compress, serialize).
Elapsed time ≈ CPU time. More cores help.

**IO-bound**: time goes on *waiting for something outside the CPU* (disk, network, DB, another service).
The CPU issues a command, and someone else's hardware does the slow part.
Elapsed time ≫ CPU time. More cores don't help.

```
one request (20 ms):

CPU  ▓                                   ▓         ← ~0.3 ms of real CPU
     |----------- waiting on IO ---------|
     build query                    parse response
```

> Everything touches the CPU. "IO-bound" means the CPU is *idle* for most of the interval.
> That idle time is the whole reason to do something else in the meantime.

Note: a plain memcpy (page cache → buffer) is memory-bound, not "CPU-bound work" in the useful sense.

---

## 2. Three independent dimensions

| # | Question | Options |
|---|----------|---------|
| 1 | **Where does the work execute?** | same thread / another thread (same service) / another process / another service (API, DB) |
| 2 | **What does the caller do?** | waits for result (**sync**) or continues and gets result later (**async**) |
| 3 | **For any thread that is waiting: parked or free?** | parked by OS (**blocking**) or free to run other work (**non-blocking**) |

Things to remember:

- **Sync/async is about the caller's code flow, not about threads.** A sync REST call goes to another service and the caller still waits.
- **Blocking is per-thread, not per-system.** Caller thread and callee thread each have their own answer.
- All three vary independently.

```
Scenario                                  Where          Caller   Blocked thread
----------------------------------------  -------------  -------  -----------------------
read() on main thread                     same thread    sync     main
await db.fetch()  (Tokio, socket)         DB server      async    none
await tokio::fs::read()                   pool thread    async    pool thread (your worker is free)
REST call, wait for response              other service  sync     your thread
publish to Kafka, move on                 broker         async    none
read(O_NONBLOCK) in a loop                same thread    sync     none, but busy-polling
```

---

## 3. Thoughts, corrected

- **Threads have nothing to do with async.** Work can be done by another thread, another service, or the same thread later.
- **Async is non-blocking for the *caller*, not necessarily end to end.** The callee may still block on its own DB call or sleep.
- **Most common combos** (hence the confusion):
  - async + non-blocking + other thread/service
  - sync + blocking + single thread
- **Async + blocking is not bad by itself.** Thread-per-request and `spawn_blocking` are exactly this. It becomes a problem when the blocked thread is one that others depend on (event-loop worker), or blocking calls exhaust a small pool.
- **Sync + non-blocking** is rare: the call returns "not ready" and you poll in a loop. An event loop is how you get *async on one thread*, which is a different thing.

```
Bad:   async fn handler() { std::thread::sleep(1s); }   // parks the worker + every task queued on it
Good:  async fn handler() { tokio::time::sleep(1s).await; }
Good:  spawn_blocking(|| heavy_sync_call())              // block a pool thread, not the event loop
```

---

## 4. Async on the same thread: how suspension works

One thread, many requests, **never two instructions at once**. It switches at explicit pause points (`.await`).

```rust
async fn handle(id: u64) -> Response {
    let user  = db.fetch(id).await;      // pause point 1
    let score = compute(&user);          // NO pause: owns the thread
    cache.set(id, score).await;          // pause point 2
    Response::new(score)
}
```

The compiler turns this into a **state machine** (an enum), not a thread with a stack:

```rust
enum Handle {
    Start        { id },
    WaitingDb    { id, db_fut },          // suspended at pause 1
    WaitingCache { score, cache_fut },    // suspended at pause 2
    Done,
}
```

Suspended task = a small heap object (tens to hundreds of bytes) holding only the variables still needed.
A parked OS thread = its whole stack (MBs). That's why 1 thread can hold 100k tasks, but 100k threads is a problem.

```
one thread, requests A B C:

t1  run A → hits await, IO submitted, A stored as WaitingDb
t2  run B → hits await, stored
t3  run C → hits await, stored
t4  nothing ready → thread sleeps in epoll_wait
t5  A's IO done   → resume A → compute → next await
t6  B's IO done   → resume B ...
```

Rules of this model:

- Tasks can't be interrupted; they must yield themselves.
- A 50 ms `compute()` inside a task freezes every other task on that thread, including ones whose IO already finished.
- If work is CPU-bound, single-threaded async gains nothing.

---

## 5. How does the thread know IO is done?

First, what actually happens when a task "does IO": it goes through the **kernel**.

```
your task ──syscall──▶ kernel ──command──▶ NIC / SSD (own processor, DMA)
                                              │
                                    wire, remote server, NAND ...   ← CPU not involved
                                              │
your task ◀── wake ── kernel ◀── interrupt ───┘
```

CPU: a few microseconds at the start (issue) and at the end (copy + parse). The device does the long middle.

### epoll: reactor ("tell me when it's *ready*")

Notification means "an operation on this fd won't block now", **not** "IO is done".

```
task A: socket.read().await

1. read()               → kernel: EAGAIN (nothing yet)
2. runtime registers fd with epoll, stores A's Waker, suspends A
3. worker runs other tasks, or sleeps in epoll_wait (the one place it may sleep)
   ... packet arrives → DMA into RAM → interrupt → kernel marks fd readable
4. epoll_wait returns → runtime calls A's Waker → A is ready
5. A polled again → read() succeeds now → CPU copies data → A continues
```

You perform the final `read()` yourself.

### io_uring: proactor ("do it, tell me when it's *done*")

```
1. A pushes  "read 4KB from fd into buf"  onto the Submission Queue (SQ)
2. kernel picks it up and performs the whole read
3. data lands in buf
4. kernel pushes a result onto the Completion Queue (CQ)
5. runtime reads CQ → wakes A → data is already in buf
```

The kernel performs the final step, then tells you. The buffer must stay valid until completion, so the API takes ownership of it.

### Why this matters for files

Regular files are always "ready" on Linux, so epoll cannot express "notify me when this disk read finishes". A cache-miss read just blocks the thread.

| Approach | What really happens |
|---|---|
| `tokio::fs` (epoll era) | forwarded to a blocking thread pool. Your worker stays free, but a pool thread is parked |
| io_uring (`tokio-uring`, `glommio`) | real async for files and sockets, no parked thread |

```
reactor  = readiness-based  = "you can read now"
proactor = completion-based = "your read is finished"
```

---

## 6. How do 8 cores handle 1000 requests?

Only because they're mostly IO-bound: each request needs ~1-2% CPU, so 1000 in-flight is affordable.

- **8 worker threads** (one per core). When a request waits on IO, the worker picks up another.
- **Work-stealing** (Tokio multi-thread): idle workers steal from busy ones. No worker "owns" 125 requests, and a task may resume on a different worker after an `.await`.
- **Pinned connections** (Netty, Seastar, glommio): each connection stays on one worker's event loop.
- If each request needs 50 ms of pure CPU, 1000 requests = 50 s of work over 8 cores. Most of them queue, and async doesn't help.

---

## 7. Language cheat sheet

**Java** (plain Java; Spring's own version is `@Async`)

| Type | Same thread | Another thread |
|---|---|---|
| `Runnable` (no return) | `run()` | `new Thread(r).start()` / `executor.execute(r)` |
| `Callable` (returns) | `call()` | `executor.submit(c)` → `Future` |

- `submit()` + immediate `get()` = effectively sync, since `get()` blocks.
- `CompletableFuture` avoids this by chaining (`thenApply`, `thenCompose`) instead of waiting.

**Rust**

- `async fn` just builds a **lazy future**. Nothing runs until it's polled.
- `.await` **suspends the task and frees the thread**. It does not block. It's sequential in *logic*, not in *thread usage*.
- `tokio::spawn(fut)` creates a **task** on the runtime's workers (not a new thread), and starts it right away, like Java's `submit`.
- Two things concurrently inside one task: `tokio::join!(a(), b())`.
- Blocking or CPU-heavy code: `spawn_blocking`.

---

## 8. Across services

| Style | Caller | Coupling | Typical pattern |
|---|---|---|---|
| Request/response | waits | caller knows callee | REST/gRPC, **orchestration** (central coordinator) |
| Event-driven | doesn't wait | caller doesn't know consumers | pub/sub via broker, **choreography** (services react) |
| Middle path | returns `202`, callee calls back later | direct, no broker | callback / webhook ("async over sync") |

Sync/async and orchestration/choreography are related but **different axes**. An orchestrator can be async (commands over Kafka, Temporal).

Callback caveats: needs retries, idempotency, and a correlation id.

### Decision: do I need the result to continue?

```
need the result (payment, inventory check)  → call the service directly (sync, or async-and-await)
don't need it (email, analytics, downstream) → publish an event
```

### Outbox pattern

Solves the **dual-write problem**: update DB *and* publish an event atomically.

```
BEGIN
  update orders ...
  insert into outbox (event ...)     ← same transaction
COMMIT
relay process → reads outbox → publishes to broker → marks sent
```

Use it whenever an event **must not be lost**, internal or external. Good default habit for any event publish.

A DB call isn't "calling another service" in this decision. It's just IO from your own thread.

---

## One-page summary

```
CPU-bound  → parallelism (more cores)
IO-bound   → overlap the waiting (async / non-blocking)

Q1  who runs the work?            same thread | other thread | other service
Q2  does my code flow wait?       sync | async
Q3  is a waiting thread parked?   blocking | non-blocking

async task  = state machine, suspended at .await, tiny
epoll       = "ready, you go read"
io_uring    = "done, data is in your buffer"
```