import model.Restaurant
import mu.KotlinLogging
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.FluentWait
import java.time.Duration
import java.util.stream.Collectors

private val logger = KotlinLogging.logger {}

fun main() {
    val browser = HeadlessBrowser(isHeadlessBrowser = true)

    val address = "1739 N Milwaukee Ave, Chicago, 60647"
    val restaurants: List<Restaurant> = browser.getNearbyRestaurants(address)
    logger.info { "restaurants: ${restaurants.mapNotNull{ it.toString()}.joinToString(",")}" }

    browser.quit()
}

class HeadlessBrowser(val isHeadlessBrowser: Boolean = false) {
    private val browserDriver: ChromeDriver
    private val wait: FluentWait<WebDriver>
    private val logger = KotlinLogging.logger {}

    init {
        System.setProperty("webdriver.chrome.driver", "driver/chromedriver")

        val options = ChromeOptions()
        // Fix to avoid "Please Wait..we are checking your browser" detection.
        options.addArguments("--disable-blink-features=AutomationControlled")
        if (isHeadlessBrowser) {
            options.setHeadless(true)
        }
        browserDriver = ChromeDriver(options)

        wait = FluentWait<WebDriver>(browserDriver)
            .withTimeout(Duration.ofSeconds(60*3))
            .pollingEvery(Duration.ofSeconds(5))
            .ignoring(NoSuchElementException::class.java)
    }


    fun getNearbyRestaurants(address: String): List<Restaurant> {
        logger.info{ "Retrieving near by restaurants to $address" }
        browserDriver.get("https://www.doordash.com/")

        // enter address
        val file = browserDriver.getScreenshotAs(OutputType.FILE)
        val addressInput = wait.until {browserDriver.findElement(By.cssSelector("input[placeholder='Enter delivery address']"))}
        addressInput.sendKeys(address)
        addressInput.sendKeys(Keys.ENTER)

        val isNearbyStoreLoaded = wait.until {
            try {
                // sometimes the Enter is ignored
                addressInput.sendKeys(Keys.ENTER)
                browserDriver.findElements(By.cssSelector("div[data-anchor-id='CarouselStoreContainer']")).size > 1
            } catch (re: java.lang.RuntimeException) {
                // keep retrying
            }
        }

        val nearByStoreCategories = browserDriver.findElements(By.cssSelector("div[data-anchor-id='CarouselStoreContainer']"))
        val nearbyStore = nearByStoreCategories.flatMap { category -> category.findElements(By.cssSelector("div[data-testid='card.store']")) }
        val restaurants = nearbyStore.mapNotNull { store ->
            try {
                val storeInfo = store.getAttribute("innerText") // China Doll, 2.5 mi, 62 min
                val storeName = storeInfo.split("\n")[0]
                val storeUri = store.getAttribute("innerHTML").split("\"")[1] // "/store/1375646/?pickup=false"
                val storeId = storeUri.split("/")[2]
                Restaurant(storeId = storeId, name= storeName , uri = storeUri)
            } catch (re: RuntimeException) {
                null
            }
        }

        return restaurants
    }

    fun quit() {
        browserDriver.quit()
    }
}