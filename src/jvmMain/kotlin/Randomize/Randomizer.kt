package Randomize

import model.MenuItem
import model.Restaurant
import kotlin.random.Random

class Randomizer {
    companion object{
        fun getRandomRestaurant(restaurants: List<Restaurant>): Restaurant {
            // Use kotlin randomizer
            return restaurants[Random.nextInt(until = restaurants.size)]
        }

        fun getRandomMenuItems(menuItems: List<MenuItem>): List<MenuItem> {
            //now - randomly pick three items
            //future - pick one meal item + drink
            //future - cap at 25 dollars
            return menuItems.asSequence().shuffled().take(3).toList()
        }
    }
}