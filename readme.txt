backend [kotlin] 
1. running the server
	-> ./gradlew run 
		/Users/ljiang/Desktop/imfeelingluckyfooddelivery/jvm-js-fullstack)
	-> ./gradlew run --continuous
	-> http://localhost:9090

frontend [reactjs]
1. running the frontend
	-> npm start
		/Users/ljiang/Desktop/imfeelingluckyfooddelivery/react_frontend

database
1. running mongodb
	-> brew services start mongodb-community@5.0
	-> brew services stop mongodb-community@5.0


Frontend
[]landing page
[] cart of order
[] checkout & tracking page


Backend
[]landing page
	- getRestaurants(string Address): List<Resturant>
	- randomizeMeal(Restaurant res): Meal
[]cart of order
[]checkout 
	- completeOrder(order)
[]tracking
	- getOrderStatus(order)


Database
mongodb/dynamodb -> nosql store


** notes
1. creating a database collection 
	-> val database = client.getDatabase("${collectionName}")
	-> val collection = database.getCollection<${collectionDocumentModel}>() 
	-> collection.insertOne(call.receive<ShoppingListItem>())


== Third party Providers ==
Doordash
Uber eats
Postmates
Grubhub
Ghost Kitchens
