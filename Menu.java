package cpsc4620;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
 * This file is where the front end magic happens.  You should NOT make any changes to this file.
 * FYI, a modified version of this progra will be used to test your aplication!
 *
 * Simply removing menu methods because you don't know how to implement it will result in a major error penalty (akin to your program crashing)
 *
 * Speaking of crashing. Your program shouldn't do it. Use exceptions, or if statements, or whatever it is you need to do to keep your program from breaking.
 *
 */


public class Menu {
	public static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

	public static void main(String[] args) throws SQLException, IOException {

		System.out.println("Welcome to Pizzas-R-Us!");

		int menu_option = 0;

		// present a menu of options and take their selection
		PrintMenu();
		String option = reader.readLine();
		menu_option = Integer.parseInt(option);

		while (menu_option != 9) {
			switch (menu_option) {
			case 1:// enter order
				EnterOrder();
				break;
			case 2:// view customers
				viewCustomers();
				break;
			case 3:// enter customer
				EnterCustomer();
				break;
			case 4:// view order
				// all/open/closed/date/last
				ViewOrders();
				break;
			case 5:// mark order as complete
				MarkOrderAsComplete();
				break;
			case 6:// view inventory levels
				ViewInventoryLevels();
				break;
			case 7:// add to inventory
				AddInventory();
				break;
			case 8:// view reports
				PrintReports();
				break;
			}
			PrintMenu();
			option = reader.readLine();
			menu_option = Integer.parseInt(option);
		}

	}

	public static Integer getMyCustomer() throws SQLException, IOException {
		ArrayList<Customer> customerList = DBNinja.getCustomerList();
		int customerID = -1;

		System.out.println("Is this order for an existing customer? Answer y/n: ");
		String yn = reader.readLine();
		if (yn.contains("y")) {
			System.out.println("Here's a list of current customers: ");
			for (Customer c : customerList) {
				System.out.println(c);
			}
			System.out.println("which customer is this order for? \nEnter ID Number");
			customerID = Integer.parseInt(reader.readLine());
		}//yes existing customer
		else if (yn.contains("n")) {
			customerID = Menu.EnterCustomer();
		}//no existing customer
		else {
			customerID = -1;
			System.out.println("ERROR: I don't understand your input for: is this order an existing customer?");
		}

		return customerID;
	}

		// allow for a new order to be placed
	public static void EnterOrder() throws SQLException, IOException {
		//BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String yndiscount = "";
		double custprice = 0.0;
		double busprice = 0.0;
		int morePizza = 0;
		int customerID = -1;
		int orderID = -1; //DBNinja.getNextOrderID();
		ArrayList<Pizza> tempPizzaList = new ArrayList<Pizza>();
		ArrayList<Discount> discs = DBNinja.getDiscountList();

		//get the time of order creation
		Date date = new Date();
		Object param =  new java.sql.Timestamp(date.getTime());

		System.out.println("Is this order for: \n1.) Dine-in\n2.) Pick-up\n3.) Delivery\nEnter the number of your choice:");
		int answer = Integer.parseInt(reader.readLine());

		switch (answer) {
			case 1:
				System.out.println("What is the table number for this order?");
				int tableNum = Integer.parseInt(reader.readLine());

				System.out.println("Let's build a pizza!");
				do
				{
					Pizza builtPizza = buildPizza(orderID);
					tempPizzaList.add(builtPizza);
					System.out.println("Enter -1 to stop adding pizzas...Enter anything else to continue adding more pizzas to the order.");
					morePizza = Integer.parseInt(reader.readLine());
				}while(morePizza != -1);

				//calculate prices
				for(Pizza p : tempPizzaList)
				{
					custprice += p.getCustPrice();
					busprice += p.getBusPrice();//both of these already have the calculated total price (base + toppings + pizza discounts)
				}

				//create order
				DineinOrder myDineInOrder = new DineinOrder(orderID, -1, param.toString(), custprice, busprice, false, tableNum);
				myDineInOrder.setPizzaList(tempPizzaList);

				//check for order discounts
				System.out.println("Do you want to add discounts to this order? Enter y/n?");
				yndiscount = reader.readLine();
				while(yndiscount.equals("y"))
				{
//					// System.out.println("Getting discount list...");
//					for(Discount d : discs)
//					{
//						System.out.println(d);
//					}
					printDiscounts(discs);
					System.out.println("Which Order Discount do you want to add? \nEnter the Discount ID. Enter -1 to stop adding Discounts: ");
					int DiscountID = Integer.parseInt(reader.readLine());
					while(DiscountID != -1)
					{
						Discount temp = discs.get(DiscountID-1);//same deal as above
						myDineInOrder.addDiscount(temp);//this not only adds it to the order, but also modifies the two prices as needed.
//						for(Discount d : discs)
//						{
//							System.out.println(d);
//						}
						printDiscounts(discs);
						System.out.println("Which Order Discount do you want to add? \nEnter the Discount ID. Enter -1 to stop adding Discounts: ");
						DiscountID = Integer.parseInt(reader.readLine());
					}
					yndiscount = "n";
					//System.out.println("Do you want to add more discounts to this order? Enter y/n?");
					//yndiscount = reader.readLine();
				}
				//add order
				DBNinja.addOrder(myDineInOrder);
//				for(Discount d : myDineInOrder.getDiscountList())
//				{
//					DBNinja.useOrderDiscount(myDineInOrder, d);
//				}

				break;
			case 2:
				//We need to know if this customer already exists
				customerID = getMyCustomer();
				if (customerID == -1){
					return;
				}

				System.out.println("Let's build a pizza!");
				do
				{
					Pizza builtPizza = buildPizza(orderID);
					tempPizzaList.add(builtPizza);
					System.out.println("Enter -1 to stop adding pizzas...Enter anything else to continue adding more pizzas to the order.");
					morePizza = Integer.parseInt(reader.readLine());
				}while(morePizza != -1);

				//calculate prices
				for(Pizza p : tempPizzaList)
				{
					custprice += p.getCustPrice();
					busprice += p.getBusPrice();//both of these already have the calculated total price (base + toppings + pizza discounts)
				}


				//create order
				PickupOrder myPickupOrder = new PickupOrder(orderID, customerID, param.toString(), custprice, busprice, false, false);
				myPickupOrder.setPizzaList(tempPizzaList);

				//check for order discounts
				//ArrayList<Discount> discs = DBNinja.getDiscountList();
				System.out.println("Do you want to add discounts to this order? Enter y/n?");
				yndiscount = reader.readLine();
				while(yndiscount.equals("y"))
				{
					// System.out.println("Getting discount list...");
//					for(Discount d : discs)
//					{
//						System.out.println(d);
//					}
					printDiscounts(discs);
					System.out.println("Which Order Discount do you want to add? \nEnter the Discount ID. Enter -1 to stop adding Discounts: ");
					int DiscountID = Integer.parseInt(reader.readLine());
					while(DiscountID != -1)
					{
						Discount temp = discs.get(DiscountID-1);//same deal as above
						myPickupOrder.addDiscount(temp);//this not only adds it to the order, but also modifies the two prices as needed.
//						for(Discount d : discs)
//						{
//							System.out.println(d);
//						}
						printDiscounts(discs);
						System.out.println("Which Order Discount do you want to add? \nEnter the Discount ID. Enter -1 to stop adding Discounts: ");
						DiscountID = Integer.parseInt(reader.readLine());
					}
					yndiscount = "n";
				}
				//add order
				DBNinja.addOrder(myPickupOrder);

				break;
			case 3:
				//We need to know if this customer already exists
				customerID = getMyCustomer();
				if (customerID == -1){
					return;
				}

				//We need the address first
				System.out.println("What is the House Number for this order? (i.e., 111)");
				String houseNum = reader.readLine();
				System.out.println("What is the Street for this order? (i.e., smilestreet)");
				String street = reader.readLine();
				System.out.println("What is the City for this order? (i.e., greenville)");
				String city = reader.readLine();
				System.out.println("What is the State Abbreviation for this order? (i.e., SC)");
				String state = reader.readLine();
				System.out.println("What is the Zip Code for this order? (i.e., 20605)");
				String zip = reader.readLine();
				String address = houseNum + "\t" + street + "\t" + city + "\t" + state + "\t" + zip;

				System.out.println("Let's build a pizza!");
				do
				{
					Pizza builtPizza = buildPizza(orderID);
					tempPizzaList.add(builtPizza);
					System.out.println("Enter -1 to stop adding pizzas...Enter anything else to continue adding more pizzas to the order.");
					morePizza = Integer.parseInt(reader.readLine());
				}while(morePizza != -1);

				//calculate prices
				for(Pizza p : tempPizzaList)
				{
					custprice += p.getCustPrice();
					busprice += p.getBusPrice();//both of these already have the calculated total price (base + toppings + pizza discounts)
				}

				//create order
				DeliveryOrder myDeliveryOrder = new DeliveryOrder(orderID, customerID, param.toString(), custprice, busprice, false, address);
				myDeliveryOrder.setPizzaList(tempPizzaList);

				//check for order discounts
				System.out.println("Do you want to add discounts to this order? Enter y/n?");
				yndiscount = reader.readLine();
				while(yndiscount.equals("y"))
				{
					//ArrayList<Discount> discs = DBNinja.getDiscountList();
					// System.out.println("Getting discount list...");
					for(Discount d : discs)
					{
						System.out.println(d);
					}
					System.out.println("Which Order Discount do you want to add? \nEnter the Discount ID. Enter -1 to stop adding Discounts: ");
					int DiscountID = Integer.parseInt(reader.readLine());
					while(DiscountID != -1)
					{
						Discount temp = discs.get(DiscountID-1);//same deal as above
						myDeliveryOrder.addDiscount(temp);//this not only adds it to the order, but also modifies the two prices as needed.
//						for(Discount d : discs)
//						{
//							System.out.println(d);
//						}
						printDiscounts(discs);
						System.out.println("Which Order Discount do you want to add? \nEnter the Discount ID. Enter -1 to stop adding Discounts: ");
						DiscountID = Integer.parseInt(reader.readLine());
					}
					yndiscount = "n";
				}

				//add order
				DBNinja.addOrder(myDeliveryOrder);

				break;
			default:
				System.out.println("ERROR: I do not understand that order option...");
				return;
		}
		
		System.out.println("Finished adding order...Returning to menu...");
	}
	
	public static void viewCustomers()
	{
		try 
		{
			ArrayList<Customer> custs = DBNinja.getCustomerList();
			for(Customer c : custs)
			{
				System.out.println(c);
			}
		}
		catch (SQLException | IOException e) 
		{
			e.printStackTrace();
		}
		
	}

	// Enter a new customer in the database
	public static int EnterCustomer() throws SQLException, IOException {
		int cid = -1;

		System.out.println("Please Enter the Customer name (First Name <space> Last Name): ");
		String name = reader.readLine();
		String fName = name.split(" ")[0];
		String lName = name.split(" ")[1];

		System.out.println("What is this customer's phone number (##########) (No dash/space):");
		String phone = reader.readLine();
		
		ArrayList<Customer> tempList = DBNinja.getCustomerList();
		int customerID = tempList.get(tempList.size()-1).getCustID() + 1;

		Customer new_cust = new Customer(customerID, fName, lName, phone);

		cid = DBNinja.addCustomer(new_cust);

		return cid;
	}

	// View any orders that are not marked as completed
	public static void ViewOrders() throws SQLException, IOException {
		int count = 0;
		Order currOrder = null;
		ArrayList<Order> currOrders = null;
		System.out.println("Would you like to:\n(a) display all orders [open or closed]\n(b) display all open orders\n(c) display all completed orders\n(d) display orders on a specific date\n(e) display last order");
		String ans = reader.readLine();

		switch (ans) {
			case "a": //all
				currOrders = DBNinja.getOrders(3);
				for (Order o : currOrders) {
					System.out.println(o);
				}
				count = currOrders.size();
				break;
			case "b": //open
				currOrders = DBNinja.getOrders(1);
				for (Order o : currOrders) {
					System.out.println(o);
				}
				count = currOrders.size();
				break;
			case "c": //closed
				currOrders = DBNinja.getOrders(2);
				for (Order o : currOrders) {
					System.out.println(o);
				}
				count = currOrders.size();
				break;
			case "d": //by date
				System.out.println("What is the date you want to restrict by? (FORMAT= YYYY-MM-DD)");
				String date = reader.readLine();
				String[] splitDate = date.split("-");
				// Print off high level information about the order
				currOrders = DBNinja.getOrdersByDate(date);
				for (Order o : currOrders) {
					System.out.println(o.toSimplePrint());
				}
				count = currOrders.size();
				break;
			case "e": //lastorder
				currOrder = DBNinja.getLastOrder();
				//System.out.println(currOrder.toSimplePrint());
				printOrderDetails(currOrder);
				return;

			default:
				System.out.println("I don't understand that input, returning to menu");
				return;
		}

		if (count > 0) {

			System.out.println("Which order would you like to see in detail? Enter the number (-1 to exit): ");
			int chosen_order = Integer.parseInt(reader.readLine());
			if (chosen_order == -1) { return; }
			Order tempO = null;
			for (Order o : currOrders) {
				if (o.getOrderID() == chosen_order) {
					tempO = o;
				}
			}

			if (tempO != null) {
				printOrderDetails(tempO);
			} else {
				System.out.println("Incorrect entry, returning to menu.");
				return;
				}
		} else {
			System.out.println("No orders to display, returning to menu.");
			return;
		}

		}

	// When an order is completed, we need to make sure it is marked as complete
	public static void MarkOrderAsComplete() throws SQLException, IOException {
		ArrayList<Order> currOrders = DBNinja.getOrders(1);
		// see all open orders
		if(currOrders.size() == 0)//this means that within all the current orders, none of them are incomplete
		{
			System.out.println("There are no open orders currently... returning to menu...");
			return;
		}
		
		for (Order o : currOrders) 
		{
				System.out.println(o);
		}

		// pick the order to mark as completed
		System.out.println("Which order would you like mark as complete? Enter the OrderID: ");
		int chosen_order = Integer.parseInt(reader.readLine());

		if (chosen_order == -1) { return; }
		Order tempO = null;
		for (Order o : currOrders) {
			if (o.getOrderID() == chosen_order) {
				tempO = o;
			}
		}

		if (tempO != null) {
			DBNinja.completeOrder(tempO.getOrderID(), DBNinja.order_state.PREPARED);
		} else {
			System.out.println("No such order.");
		}

	}

	// See the list of inventory and it's current level
	public static void ViewInventoryLevels() throws SQLException, IOException 
	{
		ArrayList<Topping> curInventory = DBNinja.getToppingList();
		printInventory(curInventory);
	}

	// Select an inventory item and add more to the inventory level to re-stock the
	// inventory
	public static void AddInventory() throws SQLException, IOException {
		ArrayList<Topping> curInventory = DBNinja.getToppingList();
		printInventory(curInventory);
		//DBNinja.printInventory();
		// select a topping to add inventory to

		System.out.println("Which topping do you want to add inventory to? Enter the number: ");
		int chosen_t = Integer.parseInt(reader.readLine());
		if (chosen_t <= curInventory.size()) 
		{
			System.out.println("How many units would you like to add? ");
			double add = Double.parseDouble(reader.readLine());
			DBNinja.addToInventory(chosen_t, add);
		} 
		else 
		{
			System.out.println("Incorrect entry, not an option");
		}
	}

	// A function that builds a pizza. Used in our add new order function
	public static Pizza buildPizza(int orderID) throws SQLException, IOException {

		// select size
		System.out.println("What size is the pizza?");
		System.out.println("1."+DBNinja.size_s);
		System.out.println("2."+DBNinja.size_m);
		System.out.println("3."+DBNinja.size_l);
		System.out.println("4."+DBNinja.size_xl);
		System.out.println("Enter the corresponding number: ");
		int size_option = Integer.parseInt(reader.readLine());
		String size = "";
		if (size_option == 1) {
			size = DBNinja.size_s;
		} else if (size_option == 2) {
			size = DBNinja.size_m;
		} else if (size_option == 3) {
			size = DBNinja.size_l;
		} else {
			size = DBNinja.size_xl;
		}

		// select crust
		System.out.println("What crust for this pizza?");
		System.out.println("1."+DBNinja.crust_thin);
		System.out.println("2."+DBNinja.crust_orig);
		System.out.println("3."+DBNinja.crust_pan);
		System.out.println("4."+DBNinja.crust_gf);
		System.out.println("Enter the corresponding number: ");
		int c_option = Integer.parseInt(reader.readLine());
		String crust = "";
		if (c_option == 1) {
			crust = DBNinja.crust_thin;
		} else if (c_option == 2) {
			crust = DBNinja.crust_orig;
		} else if (c_option == 3) {
			crust = DBNinja.crust_pan;
		} else {
			crust = DBNinja.crust_gf;
		}

		// get the base prices
		double base_CustPrice = DBNinja.getBaseCustPrice(size, crust);
		double base_BusPrice = DBNinja.getBaseBusPrice(size, crust);
		Date date = new Date();
		Object param =  new java.sql.Timestamp(date.getTime());
		
		int pizzaID = -1; //DBNinja.getNextPizzaID();

		Pizza newPizza = new Pizza(pizzaID, size, crust, orderID, "In Progress", param.toString(), base_CustPrice, base_BusPrice);

		// add toppings to the pizza
		int TopID = 0;
		//DBNinja.printInventory();
		ArrayList<Topping> tops = DBNinja.getToppingList();
		Topping myTop = null;
		printInventory(tops);
		System.out.println("Which topping do you want to add? Enter the TopID. Enter -1 to stop adding toppings: ");
		TopID = Integer.parseInt(reader.readLine());
		while(TopID != -1)
		{
			for(Topping t : tops)
			{
				if (t.getTopID() == TopID) {
					myTop = t;
					break;
				}
			}

			//do we have enough toppings?
			if(myTop.getCurINVT() > myTop.getMinINVT())//we use -1 because the DB starts at index 1 and java starts at index 0
			{
				boolean isExtra = false;
				System.out.println("Do you want to add extra topping? Enter y/n");
				String yn = reader.readLine();
				if(yn.contains("y"))
				{
					isExtra = true;
					myTop.setDoubled(true);
				}
				newPizza.addToppings(myTop, isExtra);//adds the topping to the pizza and increases the two prices
				//newPizza.modifyDoubledArray(TopID-1, isExtra);
			}
			else
			{
				System.out.println("We don't have enough of that topping to add it...");
			}
			System.out.println("Available Toppings:");
			printInventory(tops);
			System.out.println("Which topping do you want to add? Enter the TopID. Enter -1 to stop adding toppings: ");
			TopID = Integer.parseInt(reader.readLine());
		}	
		
		// add discounts that apply to the pizza
		System.out.println("Do you want to add discounts to this Pizza? Enter y/n?");
		String yndiscount = reader.readLine();
		while(yndiscount.equals("y"))
		{
			ArrayList<Discount> discs = DBNinja.getDiscountList();
//			for(Discount d : discs)
//			{
//				System.out.println(d);
//			}
			printDiscounts(discs);
			System.out.println("Which Pizza Discount do you want to add? \nEnter the Discount ID. Enter -1 to stop adding Discounts: ");
			int DiscountID = Integer.parseInt(reader.readLine());
			while(DiscountID != -1)
			{
				Discount temp = discs.get(DiscountID-1);//same deal as above
				newPizza.addDiscounts(temp);//this not only adds it to the pizza, but also modifies the two prices as needed.
//				for(Discount d : discs)
//				{
//					System.out.println(d);
//				}
				printDiscounts(discs);
				System.out.println("Which Pizza Discount do you want to add? \nEnter the Discount ID. Enter -1 to stop adding Discounts: ");
				DiscountID = Integer.parseInt(reader.readLine());
			}
			yndiscount = "n";
		}
		//pizza is complete, return it
		return newPizza;
	}

	private static void printInventory (ArrayList<Topping> tops ){
		System.out.printf("%-5s%-20s%-20s\n","ID","Topping Name","Inventory");
		System.out.printf("%-5s%-20s%-20s\n","--","------------","---------");
		for(Topping t : tops)
		{
			//System.out.println(t);
			System.out.printf("%-5s%-20s%-20s\n",t.getTopID(), t.getTopName(), t.getCurINVT());
		}
	}

	private static void printDiscounts (ArrayList<Discount> discs ){
		System.out.printf("%-5s%-25s%-20s\n","ID","Discount Name","Amount");
		System.out.printf("%-5s%-25s%-20s\n","--","------------","-------");
		for(Discount d : discs)
		{
			//System.out.println(t);
			System.out.printf("%-5s%-25s%-20s\n",d.getDiscountID(), d.getDiscountName(), ((d.isPercent())?"":"$")+d.getAmount()+((d.isPercent())?"%":""));
		}
	}

	public static void PrintReports() throws SQLException, NumberFormatException, IOException
	{
		System.out.println("Which report do you wish to print? \n(a) ToppingPopularity\n(b) ProfitByPizza\n(c) ProfitByOrderType:");
		String ans = reader.readLine();
		if(ans.equals("a"))
			DBNinja.printToppingPopReport();
		else if(ans.equals("b"))
			DBNinja.printProfitByPizzaReport();
		else if(ans.equals("c"))
			DBNinja.printProfitByOrderType();
		else
		{
			System.out.println("I don't understand that input... returning to menu...");
			return;
		}
		System.out.println("\n");
	}

	public static void printOrderDetails (Order o)  {
		Order tempO = null;
		tempO = o;

		if (tempO instanceof DeliveryOrder) {
			DeliveryOrder temp = (DeliveryOrder) tempO;
			System.out.println(temp);
		} else if (tempO instanceof DineinOrder) {
			DineinOrder temp = (DineinOrder) tempO;
			System.out.println(temp);
		} else if (tempO instanceof PickupOrder) {
			PickupOrder temp = (PickupOrder) tempO;
			System.out.println(temp);
		}

		Discount.printDiscounts(o.getDiscountList());
		for (Pizza pizza : o.getPizzaList())
		{
			System.out.println(pizza.toString());
			Discount.printDiscounts(pizza.getDiscounts());
			Topping.printToppings(pizza.getToppings());
//			}
		}
	}



	//Prompt - NO CODE SHOULD TAKE PLACE BELOW THIS LINE
	// DO NOT EDIT ANYTHING BELOW HERE, THIS IS NEEDED TESTING.
	// IF YOU EDIT SOMETHING BELOW, IT BREAKS THE AUTOGRADER WHICH MEANS YOUR GRADE WILL BE A 0 (zero)!!

	public static void PrintMenu() {
		System.out.println("\n\nPlease enter a menu option:");
		System.out.println("1. Enter a new order");
		System.out.println("2. View Customers ");
		System.out.println("3. Enter a new Customer ");
		System.out.println("4. View orders");
		System.out.println("5. Mark an order as completed");
		System.out.println("6. View Inventory Levels");
		System.out.println("7. Add Inventory");
		System.out.println("8. View Reports");
		System.out.println("9. Exit\n\n");
		System.out.println("Enter your option: ");
	}

	/*
	 * autograder controls....do not modiify!
	 */

	public final static String autograder_seed = "6f1b7ea9aac470402d48f7916ea6a010";

	private static void autograder_compilation_check() {

		try {
			Order o = null;
			Pizza p = null;
			Topping t = null;
			Discount d = null;
			Customer c = null;
			ArrayList<Order> alo = null;
			ArrayList<Discount> ald = null;
			ArrayList<Customer> alc = null;
			ArrayList<Topping> alt = null;
			ArrayList<Pizza> alp = null;
			double v = 0.0;
			String s = "";
			int id = -1;
			Date dts = new Date();

			DBNinja.addOrder(o);
			DBNinja.addPizza(dts, id, p);
			//DBNinja.useOrderDiscount(o, d);
			id = DBNinja.addCustomer(c);
			DBNinja.completeOrder(o.getOrderID(), DBNinja.order_state.PREPARED);
			alo = DBNinja.getOrders(1);
			o = DBNinja.getLastOrder();
			alo = DBNinja.getOrdersByDate("01/01/1999");
			ald = DBNinja.getDiscountList();
			d = DBNinja.findDiscountByName("Discount");
			alc = DBNinja.getCustomerList();
			c = DBNinja.findCustomerByPhone("0000000000");
			s = DBNinja.getCustomerName(0);
			alt = DBNinja.getToppingList();
			t = DBNinja.findToppingByName("Topping");
			alt = DBNinja.getToppingsOnPizza(p);
			DBNinja.addToInventory(id, 1000.0);
			alp = DBNinja.getPizzas(o);
			ald = DBNinja.getDiscounts(o);
			ald = DBNinja.getDiscounts(p);
			v = DBNinja.getBaseCustPrice("size", "crust");
			v = DBNinja.getBaseBusPrice("size", "crust");
			DBNinja.printToppingPopReport();
			DBNinja.printProfitByPizzaReport();
			DBNinja.printProfitByOrderType();
		} catch (SQLException | IOException e) {
			e.printStackTrace();
		}
	}


}
