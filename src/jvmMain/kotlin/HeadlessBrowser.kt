import Randomize.Randomizer
import model.MenuItem
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
    // Running as headless browser gets blocked by doordash's cloudflare.
    val browser = HeadlessBrowser(isHeadlessBrowser = false)

    val address = "1739 N Milwaukee Ave, Chicago, 60647"
    val restaurants: List<Restaurant> = browser.getNearbyRestaurants(address)
    logger.info { "restaurants: ${restaurants.mapNotNull{ it.toString()}.joinToString(",\n")}" }

    val restaurant = Randomizer.getRandomRestaurant(restaurants)
    val menuItems = browser.getRestaurantMenu(restaurant)
    val selectedMenuItems = Randomizer.getRandomMenuItems(menuItems)
    logger.info {"selected restaurant: $restaurant"}
    logger.info {"menuItems: ${menuItems.mapNotNull { it.toString() }.joinToString( ",\n")}" }
    logger.info {"selectedMenuItems: ${selectedMenuItems.mapNotNull { it.toString() }.joinToString( ",\n")}" }
    browser.quit()
}

class HeadlessBrowser(val isHeadlessBrowser: Boolean = false) {
    private val browserDriver: ChromeDriver
    private val wait: FluentWait<WebDriver>
    private val doorDashBaseUrl = "https://www.doordash.com"
    private val logger = KotlinLogging.logger {}

    init {
        System.setProperty("webdriver.chrome.driver", "driver/chromedriver")

        val options = ChromeOptions()
        if (isHeadlessBrowser) {
            options.setHeadless(true)
            // Fix to avoid "Please Wait...we are checking your browser" detection.
            options.setExperimentalOption("useAutomationExtension", false)
            options.addArguments("window-size=1200x1200")
            options.addArguments("--disable-blink-features=AutomationControlled")
        }
        browserDriver = ChromeDriver(options)

        wait = FluentWait<WebDriver>(browserDriver)
            .withTimeout(Duration.ofSeconds(60*3))
            .pollingEvery(Duration.ofSeconds(5))
            .ignoring(NoSuchElementException::class.java)
            .ignoring(java.lang.RuntimeException::class.java)
    }

    fun getRestaurantMenu(restaurant: Restaurant): List<MenuItem> {
        // get a store menu, given restuarant id
        // i.e) Restaurant(storeId=27400, name=Small Cheval, uri=/store/27400/?pickup=false)
        browserDriver.get(doorDashBaseUrl + restaurant.uri)
        val menuItemsWebElements = wait.until {browserDriver.findElements(By.cssSelector("div[data-anchor-id='MenuItem']"))}
        val menuItems: List<MenuItem> = menuItemsWebElements.mapNotNull { item ->
            try {
                val menuItemId = item.getAttribute("data-item-id")
                val menuItemName = item.getAttribute("innerText").split("\n")[0]
                val menuItemPrice = item.getAttribute("innerText").split("\n")[2]
                MenuItem(id=menuItemId, name=menuItemName, price=menuItemPrice)
            } catch (re: java.lang.RuntimeException) {
                logger.warn { "Could not parse MenuItem: ${item?.getAttribute("innerText")}" }
                null
            }
        }

        return menuItems
    }


    fun getNearbyRestaurants(address: String): List<Restaurant> {
        logger.info{ "Retrieving near by restaurants to $address" }
        browserDriver.get(doorDashBaseUrl)

        // enter address
        val file = browserDriver.getScreenshotAs(OutputType.FILE)
        logger.info{"location of screenshot: $file"}
        val addressInput = wait.until {browserDriver.findElement(By.cssSelector("input[placeholder='Enter delivery address']"))}
        addressInput.sendKeys(address)
        addressInput.sendKeys(Keys.ENTER)

        val isNearbyStoreLoaded = wait.until {
            try {
                // "Keys.Enter" is ignored, retry multiple times.
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