import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors


val logger: Logger = LoggerFactory.getLogger("CoroutinesPlayground")

suspend fun bathTime() {
    logger.info("Going to take a bath")
    delay(500L)
    logger.info("Bath done, exiting")
}

suspend fun boilingWater() {
    logger.info("Boiling water")
    delay(100L)
    logger.info("Water boiled")
}

suspend fun makeCoffee() {
    logger.info("Starting to make coffee")
    delay(500L)
    logger.info("Done making coffee")
}

suspend fun sequentialMorningRoutine() {
    coroutineScope {
        bathTime()
    }

    coroutineScope {
        boilingWater()
    }
}

suspend fun concurrentMorningRoutine() {
    coroutineScope {
        launch {
            bathTime()
        }
        launch {
            boilingWater()
        }
    }
}

suspend fun noStrucConcurrencyMorningRoutine() {
    GlobalScope.launch { bathTime() }
    GlobalScope.launch { boilingWater() }
}

// plan coroutines
/*
    take  a batch
    startBoiling the water

    after both are done => drink coffee.
 */
suspend fun morningRoutineWithCoffee() {
    coroutineScope {
        val bathTimeJob = launch { bathTime() }
        val boilingWaterJob = launch { boilingWater() }
        bathTimeJob.join()
        boilingWaterJob.join()
        launch { makeCoffee() }
    }
}

suspend fun nestedMorningRoutine() {
    coroutineScope {
        coroutineScope {
            launch { bathTime() }
            launch { boilingWater() }
        }
        makeCoffee()
    }
}

// return values from coroutines
suspend fun preparingJavaCoffee(): String {
    logger.info("Starting to make coffee")
    delay(500L)
    logger.info("Done making coffee")
    return "java Coffee"
}

suspend fun toastingBread(): String {
    logger.info("Starting to toast bread")
    delay(1000L)
    logger.info("Done toasting bread")
    return "toasted bread"
}

suspend fun prepareBreakfast() {
    coroutineScope {
        val coffee = async {
            preparingJavaCoffee()
        }
        val bread = async {
            toastingBread()
        }
        val finalCoffee = coffee.await()
        val finalToast = bread.await()

        logger.info("I am eating $finalToast and drinking $finalCoffee")
    }
}

// 1 - cooperative scheduling  - coroutines yield manually.
suspend fun workingHard() {
    logger.info("Working")
    while (true) {
        // nothing
    }
    delay(100L)
    logger.info("Work done")
}

// 1 - cooperative scheduling  - coroutines yield manually.
suspend fun workingNicely() {
    logger.info("Working")
    while (true) {
        delay(100L)
    }
    delay(100L)
    logger.info("Work done")
}

suspend fun takeABreak() {
    logger.info("Taking a break")
    delay(1000L)
    logger.info("Break done")
}

suspend fun workHardRoutine() {
    val dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1) // force 1 thread
    coroutineScope {
        launch(dispatcher){
            workingHard()
        }
        launch(dispatcher) {
            takeABreak()
        }
    }
}

suspend fun workNicelyRoutine() {
    val dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1) // force 1 thread
    coroutineScope {
        launch(dispatcher){
            workingNicely()
        }
        launch(dispatcher) {
            takeABreak()
        }
    }
}

val simpleDispatcher = Dispatchers.Default // normal dispatcher - short code or yielding coroutines
val blockingDispatcher  = Dispatchers.IO // blocking code - e.g. Database IO, long running computations
val customeDispather = Executors.newFixedThreadPool(8).asCoroutineDispatcher() // customer dispatcher


suspend fun forgettingFriendsBirthDayRoutine() {
    coroutineScope {
        val workingJob = launch { workingNicely() }
        launch {
            delay(2000L)
            workingJob.cancel() // cancellation happens at the first yielding point e.g. delay()
            workingJob.join()
            logger.info("I forgot my friend's birthday, buying a present now")
        }
    }
}

// if the coroutine doesn't yield, it cannot be cancelled.
suspend fun forgettingFriendsBirthDayUncancellable() {
    coroutineScope {
        val workingJob = launch { workingHard() }
        launch {
            delay(2000L)
            logger.info("Trying to stop working...")
            workingJob.cancel() // cancellation happens at the first yielding point e.g. delay()
            workingJob.join()
            logger.info("I forgot my friend's birthday, buying a present now")
        }
    }
}

class Desk: AutoCloseable {
    init {
        logger.info("Starting to work on the desk")
    }
    override fun close() {
        logger.info("Cleaning up the desk")
    }
}
suspend fun forgettingFriendsBirthDayRoutineWithResource() {

    val desk = Desk()
    coroutineScope {
        val workingJob = launch {
            desk.use {_ -> // this resource will be closed, when the coroutine is cancelled
                workingNicely()
            }
        }

        // can also define your own cleanup code.
        workingJob.invokeOnCompletion {ex: Throwable? ->
            // can handle completion and cancellation differently
            logger.info("Make sure to inform the colleagues")
        }
        launch {
            delay(2000L)
            workingJob.cancel() // cancellation happens at the first yielding point e.g. delay()
            workingJob.join()
            logger.info("I forgot my friend's birthday, buying a present now")
        }
    }
}

// cancellation propagates also to the child routines.
suspend fun drinkWater() {
    while (true) {
        logger.info("drinking water")
        delay(1000L)
    }
}

suspend fun forgettingFriendsBirthDayStayHydratedRoutine() {
    coroutineScope {
        val workingJob = launch {
            launch { workingNicely() }
            launch { drinkWater() }
        }
        launch {
            delay(2000L)
            workingJob.cancel() // cancellation happens at the first yielding point e.g. delay()
            workingJob.join()
            logger.info("I forgot my friend's birthday, buying a present now")
        }
    }
}

suspend fun asynchronousGreeting() {
    coroutineScope {
        launch(CoroutineName("Greeting Coroutine") + Dispatchers.Default /* this 2 are combined in a coroutine context*/) {
            logger.info("Hello, everyone!")
        }
    }
}

suspend fun demoContextInheritance() {
    coroutineScope {
        launch(CoroutineName("Greeting Coroutine")) {
            logger.info("[parent coroutine] Hello!")
            launch(CoroutineName("Child Greeting Coroutine")) {// coroutine context will be inherited here
                logger.info("[child coroutine] Hi there!")
            }
            delay(200L)
            logger.info("[parent coroutine] Hi again from parent!")
        }
    }
}

suspend fun main(args: Array<String>) {
    logger.info("Just woke up")
    // forgettingFriendsBirthDayUncancellable()
    // forgettingFriendsBirthDayRoutineWithResource()
    // forgettingFriendsBirthDayStayHydratedRoutine()
    demoContextInheritance()

}