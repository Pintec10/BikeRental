## GENERAL DESCRIPTION

This is a backend application for a bike rental store with two types of bikes (3 normal bikes and 2 mountain bikes). 
The rental price is 10€/day for normal bikes and 12€/day for mountain bikes. The application allows to:
-	rent a bike from a specified date and for a specified number of days; payment is made upfront
-	return the bike and calculate the final cost. If a bike is returned late, a surcharge of 3€ is applied for each extra day if the bike is returned late.
The app was designed with extensibility criteria in mind: if in the future additional features are required such as changing the amount of bikes, introducing new bike types or changing the prices, this could be done without extensive rewriting of existing code, but rather extending it.


## CLASSES
### Bike, Customer
-	Other fields could be added such as e.g. identification document number etc.
-	The **bikeType** field is of String type; an enum would have been less error-prone, but more rigid towards addition of other bike types in the future. The front-end is advised to use select or radio button elements as input fields for this.
-	The **phoneNumber** field is a String, to allow the use of e.g. the '+' sign for international codes

### Rental
-	Has a ManyToOne relation with Bike and with Customer.
-	Contains all the time and cost information for each rental event. 
-	Each instance stores also its daily costs for bike renting and late return; since in principle the price list might be changed in the future, this ensures that the correct prices are stored for each past rental event.

### PriceList
-	Contains all the price values for bike types and extra days; it is used as a reference table to determine costs when a new Rental instance is created.
-	Only a single persistent instance of this class exists, and it is initiated in the Command Line Runner
-	Compared to hard-coding the prices during the creation of a Rental instance, this solution allows more flexibility. If in the future endpoints are added to perform CRUD operations, the user could change prices or add bike types by him/herself without requiring modifications to the back-end code.

### RentalForm
-	This is a wrapper class used for the renting endpoint, since the required information to be placed in the body of the request contains elements from both the Bike and Customer classes. 

### AppController
-	Contains the REST controller and its methods 

### BikeRentalApplication
-	Contains the main method and the Command Line Runner
-	All existing Bike instances (3 normal, 2 mountain) and the single PriceList instance are initiated in the Command Line Runner; to make sure this happens only once, the code first checks that the corresponding repositories are empty.
-	There is also a testbed that is currently commented out


## ENDPOINTS
### Renting a bike (`/api/rent`), method: POST
-	The front end should send the following data as stringified JSON in the body of the request, using the same key names:
	{
	 name: *(name and surname of customer)*
	 email: *(email of customer)*
	 phoneNumber: *(phone number of customer)*
	 bikeType: *(either 'normal' or 'mountain')*
	 startDate: *(date in YYYY-MM-DD format)*
	 agreedDurationDays: *(number of days agreed in advance for the rental)*
	}
-	Checks if a bike of the requested type is available in the desired days 
-	Uses the customer email as unique identifier to determine if the customer is already in the database; otherwise it will create a new Customer instance
-	In the end returns also an **upfrontPayment** field which contains the price to be paid upfront for the requested bike type and rental duration 

### Returning a bike (`/api/return/{rentalId}`), method: POST
-	The front-end should provide the **rentalId** as a path variable. For practical reasons, the back-end should also expose a GET method returning a JSON with all the list of Rentals and related data (see below).
-	If having additional endpoints is not desired, an alternative approach is shown in a comment inside the AppController class. The idea is to provide a RentalForm object in the body of the request, using a JSON with keys named 'bikeId' and 'startDate' to unequivocally identify the corresponding Rental instance. 
	The Id of the bike could be physically placed on the bike itself. This approach is obviously less recommended though.
-	In the end, it returns a **finalCost** field which contains the total price including possible extra fees due to late return of the bike.
-	It is assumed that if a bike is returned earlier than the expected date, no discount or refund will be applied, and the **finalCost** is the same as the **upfrontPayment**.
-	Error messages are issued if:
  -	the return event for the requested Rental has already been handled (**actualEndDate** has been set)
  -	the return date (which is the day when the POST request is made) is before the starting rental date; i.e., cancelling a reservation cannot be done through this endpoint. One for deleting a rental has been included here (see below).

### Fetching a list of all rentals (`/api/rentals`), method: GET
-	This provides a convenient method for the front-end to retrieve **rentalId** values and display a database of all rentals

### Deleting a single Rental (`/api/rentals/{rentalId}/delete`), method: DELETE
-	This provides the possibility to delete asingle Rental instance from the database, e.g. if a reservation is cancelled. The **rentalId** must be provided as a path variable.

````
