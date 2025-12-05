package cpsc4620;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public final class DBNinja {
	private static Connection conn;

	public final static String pickup = "pickup";
	public final static String delivery = "delivery";
	public final static String dine_in = "dinein";

	public final static String size_s = "Small";
	public final static String size_m = "Medium";
	public final static String size_l = "Large";
	public final static String size_xl = "XLarge";

	public final static String crust_thin = "Thin";
	public final static String crust_orig = "Original";
	public final static String crust_pan = "Pan";
	public final static String crust_gf = "Gluten-Free";

	public enum order_state {
		PREPARED,
		DELIVERED,
		PICKEDUP
	}


	private static boolean connect_to_db() throws SQLException, IOException 
	{

		try {
			conn = DBConnector.make_connection();
			return true;
		} catch (SQLException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

	}

	public static void addOrder(Order o) throws SQLException, IOException 
	{
		connect_to_db();

		conn.setAutoCommit(false);

		String query = "INSERT INTO ordertable (customer_CustID, ordertable_OrderType, ordertable_OrderDateTime," +
				       " ordertable_CustPrice, ordertable_BusPrice, ordertable_isComplete) VALUES (?, ?, ?, ?, ?, ?)";

		try (PreparedStatement os = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

			if (o.getCustID() == -1) {
				os.setNull(1, java.sql.Types.INTEGER);
			} else {
				os.setInt(1, o.getCustID());
			}
			os.setString(2, o.getOrderType());
			os.setTimestamp(3, strToTimeStamp(o.getDate()));
			os.setBigDecimal(4, BigDecimal.valueOf(o.getCustPrice()));
			os.setBigDecimal(5, BigDecimal.valueOf(o.getBusPrice()));
			os.setBoolean(6, o.getIsComplete());
			os.executeUpdate();

			ResultSet rs = os.getGeneratedKeys();

			if (!rs.next()) {
				throw new SQLException("Order ID generation failed");
			}

			int orderID = rs.getInt(1);

			if (o instanceof DeliveryOrder) {
				DeliveryOrder deliveryOrder = (DeliveryOrder) o;

				String[] components = parseAddress(deliveryOrder.getAddress());

				String deliQuery = "INSERT INTO delivery (ordertable_OrderID, delivery_HouseNum, delivery_Street, " +
						           "delivery_City, delivery_State, delivery_Zip, delivery_IsDelivered) " +
						           "VALUES (?, ?, ?, ?, ?, ?, ?)";
				try (PreparedStatement deliveryps = conn.prepareStatement(deliQuery)) {
					deliveryps.setInt(1, orderID);
					deliveryps.setInt(2, Integer.parseInt(components[0]));
					deliveryps.setString(3, components[1]);
					deliveryps.setString(4, components[2]);
					deliveryps.setString(5, components[3]);
					deliveryps.setInt(6, Integer.parseInt(components[4]));
					deliveryps.setBoolean(7, deliveryOrder.getIsComplete());

					deliveryps.executeUpdate();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} else if (o instanceof PickupOrder) {
				PickupOrder pickupOrder = (PickupOrder) o;
				String pickupQuery = "INSERT INTO pickup (ordertable_OrderID, pickup_IsPickedUp) VALUES (?, ?)";

				try (PreparedStatement pickupps = conn.prepareStatement(pickupQuery)) {
					pickupps.setInt(1, orderID);
					pickupps.setBoolean(2, pickupOrder.getIsPickedUp());

					pickupps.executeUpdate();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} else if (o instanceof DineinOrder) {
				DineinOrder dineinOrder = (DineinOrder) o;
				String dineinQuery = "INSERT INTO dinein (ordertable_OrderID, dinein_TableNum) VALUES (?, ?)";

				try (PreparedStatement dineinps = conn.prepareStatement(dineinQuery)) {
					dineinps.setInt(1, orderID);
					dineinps.setInt(2, dineinOrder.getTableNum());

					dineinps.executeUpdate();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

			insertOrderDiscount (o.getDiscountList(), orderID);

			for (Pizza pizza : o.getPizzaList()) {
				int pizzaID = addPizza(strToDate(o.getDate()), orderID, pizza);

				if (pizzaID == -1) {
					throw new SQLException("Failed to add pizza");
				}

				pizzaToppings(pizzaID, pizza.getToppings());
				pizzaDiscounts(pizzaID, pizza.getDiscounts());
				updateToppingInventory(pizzaID, pizza.getToppings());
			}

			conn.commit();
		} catch (SQLException e) {
			if(conn != null) conn.rollback();
			throw e;
		}


	}

	private static void insertOrderDiscount (ArrayList<Discount> discountList, int orderID) {
		if (discountList != null && !discountList.isEmpty()) {
			String orderDiscQuery = "INSERT INTO order_discount (ordertable_OrderID, discount_DiscountID) VALUES (?, ?)";

			try (PreparedStatement orderDiscPs = conn.prepareStatement(orderDiscQuery)) {
				for (Discount discount : discountList) {
					orderDiscPs.setInt(1, orderID);
					orderDiscPs.setInt(2, discount.getDiscountID());

					orderDiscPs.addBatch();
				}
				orderDiscPs.executeBatch();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static int addPizza(java.util.Date d, int orderID, Pizza p) throws SQLException, IOException
	{
		String addPizzaQuery = "INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, " +
				               "pizza_CustPrice, pizza_BusPrice, ordertable_OrderID) VALUES (?, ?, ?, ?, ?, ?, ?)";

		try (PreparedStatement ps = conn.prepareStatement(addPizzaQuery, Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, p.getSize());
			ps.setString(2, p.getCrustType());
			ps.setString(3, p.getPizzaState());
			ps.setTimestamp(4, new java.sql.Timestamp(d.getTime()));
			ps.setDouble(5, p.getCustPrice());
			ps.setDouble(6, p.getBusPrice());
			ps.setInt(7, orderID);

			ps.executeUpdate();

			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next()) {
					return rs.getInt(1);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}

	private static void pizzaToppings (int pizzaID, ArrayList<Topping> toppingList) throws SQLException{
		if (toppingList != null && !toppingList.isEmpty()) {

			String pizzaTopQ = "INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_IsDouble) " +
					           "VALUES (?, ?, ?)";

			try (PreparedStatement ps = conn.prepareStatement(pizzaTopQ)) {
				for (Topping topping: toppingList) {
					ps.setInt(1, pizzaID);
					ps.setInt(2, topping.getTopID());
					ps.setInt(3, topping.getDoubled() ? 1 : 0);

					ps.addBatch();
				}
				ps.executeBatch();
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static void pizzaDiscounts (int pizzaID, ArrayList<Discount> discount) throws SQLException {
		if (discount != null && !discount.isEmpty()) {
			String pizzaDiscQ = "INSERT INTO pizza_discount (pizza_PizzaID, discount_DiscountID) VALUES (?, ?)";

			try (PreparedStatement ps = conn.prepareStatement(pizzaDiscQ)){
				for (Discount d : discount) {
					ps.setInt(1, pizzaID);
					ps.setInt(2, d.getDiscountID());

					ps.addBatch();
				}
				ps.executeBatch();
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static void updateToppingInventory (int pizzaID, ArrayList<Topping> toppingList) throws SQLException {

		String updateQuery = "UPDATE topping SET topping_CurINVT = (topping_CurINVT - ?) " +
				             "WHERE topping_TopID = ? AND topping_CurINVT > 0";

		try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
			for (Topping topping : toppingList) {

				String squery = "SELECT CASE p.pizza_Size " +
						"WHEN 'Small' THEN t.topping_SmallAMT " +
						"WHEN 'Medium' THEN t.topping_MedAMT " +
						"WHEN 'Large' THEN t.topping_LgAMT " +
						"WHEN 'XLarge' THEN t.topping_XLAMT " +
						"END AS SIZEAMT, topping_CurINVT, topping_MinINVT " +
						"FROM pizza p JOIN pizza_topping pt ON p.pizza_PizzaID = pt.pizza_PizzaID " +
						"JOIN topping t ON pt.topping_TopID = t.topping_TopID " +
						"WHERE t.topping_TopID = ? AND p.pizza_PizzaID = ?";

				try (PreparedStatement sps = conn.prepareStatement(squery)) {
					sps.setInt(1, topping.getTopID());
					sps.setInt(2, pizzaID);

					try (ResultSet rs = sps.executeQuery()) {
						if (rs.next()) {
							double amt = rs.getDouble(1);
							int curInvt = rs.getInt(2);
							int minInvt = rs.getInt(3);
							int isDouble = topping.getDoubled() ? 2 : 1;

							double amtToReduce = Math.ceil(amt * isDouble);

							if (curInvt - amtToReduce >= minInvt) {
								ps.setDouble(1, amtToReduce);
								ps.setInt(2, topping.getTopID());
								ps.executeUpdate();
							} else {
								System.out.println("Low inventory warning for " + topping.getTopID());
							}
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static int addCustomer(Customer c) throws SQLException, IOException
	 {
		 connect_to_db();

		 String query = "INSERT INTO customer (customer_FName, customer_LName, customer_PhoneNum) VALUES (?, ?, ?);";

		 int custID = -1;

		 try {
			 PreparedStatement os = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			 os.setString(1, c.getFName());
			 os.setString(2, c.getLName());
			 os.setString(3, c.getPhone());

			 int rowsAffected = os.executeUpdate();
			 if (rowsAffected > 0){
				 ResultSet rs = os.getGeneratedKeys();
				 if (rs.next()){
					 custID = rs.getInt(1);
				 }
			 }
		 } catch (SQLException e) {
			 e.printStackTrace();
		 }

		 conn.close();
		 return custID;

	}

	public static void completeOrder(int OrderID, order_state newState ) throws SQLException, IOException
	{
		connect_to_db();
		String orderQ = "UPDATE ordertable SET ordertable_isComplete = ? WHERE ordertable_OrderID = ?";
		String pizzaQ = "UPDATE pizza SET pizza_PizzaState = ? WHERE ordertable_OrderID = ?";
		String pickupQ = "UPDATE pickup SET pickup_IsPickedUp = ? WHERE ordertable_OrderID = ?";
		String deliQ = "UPDATE delivery SET delivery_IsDelivered = ? WHERE ordertable_OrderID = ?";

		try (PreparedStatement orderps = conn.prepareStatement(orderQ);
			 PreparedStatement pizzaps = conn.prepareStatement(pizzaQ);
			 PreparedStatement pickupps = conn.prepareStatement(pickupQ);
			 PreparedStatement delips = conn.prepareStatement(deliQ)) {

			boolean orderStatus = false;
			boolean pickupStatus = false;
			boolean deliveryStatus = false;
			String pizzaStatus = "In progress";

			switch (newState) {
				case PREPARED:
					orderStatus = true;
					pizzaStatus = "completed";
					break;
				case DELIVERED:
					orderStatus = true;
					pizzaStatus = "completed";
					deliveryStatus = true;
					break;
				case PICKEDUP:
					orderStatus = true;
					pizzaStatus = "completed";
					pickupStatus = true;
					break;
			}
			orderps.setBoolean(1, orderStatus);
			orderps.setInt(2, OrderID);
			orderps.executeUpdate();

			pizzaps.setString(1, pizzaStatus);
			pizzaps.setInt(2, OrderID);
			pizzaps.executeUpdate();

			pickupps.setBoolean(1, pickupStatus);
			pickupps.setInt(2, OrderID);
			pickupps.executeUpdate();

			delips.setBoolean(1, deliveryStatus);
			delips.setInt(2, OrderID);
			delips.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		conn.close();

	}


	public static ArrayList<Order> getOrders(int status) throws SQLException, IOException
	 {
		 connect_to_db();
		 ArrayList<Order> orders = new ArrayList<>();
		 String partQuery = "SELECT * FROM ordertable";
		 String condition = "";

		 if (status == 1) {
			 condition = " WHERE ordertable_isComplete = 0";
		 } else if (status == 2) {
			 condition = " WHERE ordertable_isComplete = 1";
		 }

		 String query = partQuery + condition + " ORDER BY ordertable_OrderDateTime";

		 try (PreparedStatement ps = conn.prepareStatement(query);
			  ResultSet rs = ps.executeQuery()) {

			 while (rs.next()) {
				 int orderID = rs.getInt(1);
				 int custID = rs.getInt(2);
				 String orderType  = rs.getString(3);
				 String orderDate = rs.getTimestamp(4).toString();
				 double custPrice = rs.getDouble(5);
				 double busPrice = rs.getDouble(6);
				 boolean isComplete = rs.getBoolean(7);

				 Order order = null;

				 if(delivery.equalsIgnoreCase(orderType)) {
					 String address = deliveryAddress(orderID);
					 boolean isDelivered = deliveryStatus(orderID);
					 order = new DeliveryOrder(orderID, custID, orderDate, custPrice, busPrice, isComplete, address, isDelivered);
				 }
				 else if (pickup.equalsIgnoreCase(orderType)) {
					 boolean isPickedUp = pickUpStatus(orderID);
					 order = new PickupOrder(orderID, custID, orderDate, custPrice, busPrice, isPickedUp, isComplete);
				 }
				 else if (dine_in.equalsIgnoreCase(orderType)) {
					 int tabNum = tableNumber(orderID);
					 order = new DineinOrder(orderID, custID, orderDate, custPrice, busPrice, isComplete, tabNum);
				 }

				 if (order != null) {

					 ArrayList<Pizza> pizzas = getPizzas(order);
				 	 ArrayList<Discount> orderDiscs = getDiscounts(order);

					 order.setPizzaList(pizzas);
					 order.setDiscountList(orderDiscs);

					 orders.add(order);
				 }
			 }
		 } catch (SQLException e) {
			 e.printStackTrace();
		 }
		conn.close();
		return orders;
	}

	private static String deliveryAddress (int orderID) throws SQLException {
		String address = "";
		String query = "SELECT delivery_HouseNum, delivery_Street, delivery_City, delivery_State, delivery_Zip " +
				       "FROM delivery WHERE ordertable_OrderID = ?";

		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setInt(1, orderID);

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				address = rs.getInt(1) + "\t" + rs.getString(2) + "\t" + rs.getString(3)
						+ "\t" + rs.getString(4) + "\t" + rs.getInt(5);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return address;
	}

	private static boolean deliveryStatus (int orderID) throws SQLException {
		String query = "SELECT delivery_IsDelivered FROM delivery WHERE ordertable_OrderID = ?";

		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setInt(1, orderID);

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getBoolean(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static boolean pickUpStatus (int orderID) throws SQLException {
		String query = "SELECT pickup_IsPickedUp FROM pickup WHERE ordertable_OrderID = ?";

		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setInt(1, orderID);

			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getBoolean(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static int tableNumber (int orderID) throws SQLException {
		String query = "SELECT dinein_TableNum FROM dinein WHERE ordertable_OrderID = ?";

		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setInt(1, orderID);

			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}


	public static Order getLastOrder() throws SQLException, IOException 
	{
		connect_to_db();

		String query = "SELECT * FROM ordertable WHERE ordertable_OrderID = " +
				       "(SELECT MAX(ordertable_OrderID) FROM ordertable)";

		Order latestOrder = null;

		try (Statement s = conn.createStatement()) {
			try (ResultSet rs = s.executeQuery(query)) {
				if (rs.next()) {
					int orderID = rs.getInt(1);
					int custID = rs.getInt(2);
					String orderType  = rs.getString(3);
					String orderDate = rs.getTimestamp(4).toString();
					double custPrice = rs.getDouble(5);
					double busPrice = rs.getDouble(6);
					boolean isComplete = rs.getBoolean(7);

					if(delivery.equalsIgnoreCase(orderType)) {
						String address = deliveryAddress(orderID);
						boolean isDelivered = deliveryStatus(orderID);
						latestOrder = new DeliveryOrder(orderID, custID, orderDate, custPrice, busPrice, isComplete, address, isDelivered);
					}
					else if (pickup.equalsIgnoreCase(orderType)) {
						boolean isPickedUp = pickUpStatus(orderID);
						latestOrder = new PickupOrder(orderID, custID, orderDate, custPrice, busPrice, isPickedUp, isComplete);
					}
					else if (dine_in.equalsIgnoreCase(orderType)) {
						int tabNum = tableNumber(orderID);
						latestOrder = new DineinOrder(orderID, custID, orderDate, custPrice, busPrice, isComplete, tabNum);
					}

					if (latestOrder != null) {

						ArrayList<Pizza> pizzas = getPizzas(latestOrder);
						ArrayList<Discount> orderDiscs = getDiscounts(latestOrder);

						latestOrder.setPizzaList(pizzas);
						latestOrder.setDiscountList(orderDiscs);

					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		conn.close();
		return latestOrder;
	}

	public static ArrayList<Order> getOrdersByDate(String date) throws SQLException, IOException
	 {
		 connect_to_db();
		 ArrayList<Order> orders = new ArrayList<>();
		 String query = "SELECT * FROM ordertable WHERE DATE(ordertable_OrderDateTime) = ? " +
				        "ORDER BY ordertable_OrderDateTime";

		 try (PreparedStatement ps = conn.prepareStatement(query)) {
			 ps.setString(1, date);

			 try (ResultSet rs = ps.executeQuery()) {
				 while (rs.next()) {
					 int orderID = rs.getInt(1);
					 int custID = rs.getInt(2);
					 String orderType  = rs.getString(3);
					 String orderDate = rs.getTimestamp(4).toString();
					 double custPrice = rs.getDouble(5);
					 double busPrice = rs.getDouble(6);
					 boolean isComplete = rs.getBoolean(7);

					 Order order = null;

					 if(delivery.equalsIgnoreCase(orderType)) {
						 String address = deliveryAddress(orderID);
						 boolean isDelivered = deliveryStatus(orderID);
						 order = new DeliveryOrder(orderID, custID, orderDate, custPrice, busPrice, isComplete, address, isDelivered);
					 }
					 else if (pickup.equalsIgnoreCase(orderType)) {
						 boolean isPickedUp = pickUpStatus(orderID);
						 order = new PickupOrder(orderID, custID, orderDate, custPrice, busPrice, isPickedUp, isComplete);
					 }
					 else if (dine_in.equalsIgnoreCase(orderType)) {
						 int tabNum = tableNumber(orderID);
						 order = new DineinOrder(orderID, custID, orderDate, custPrice, busPrice, isComplete, tabNum);
					 }

					 if (order != null) {

						 ArrayList<Pizza> pizzas = getPizzas(order);
						 ArrayList<Discount> orderDiscs = getDiscounts(order);

						 order.setPizzaList(pizzas);
						 order.setDiscountList(orderDiscs);

						 orders.add(order);
					 }
				 }
			 } catch (SQLException e) {
				 e.printStackTrace();
			 }
		 } catch (SQLException e) {
			 e.printStackTrace();
		 }
		 conn.close();
		 return orders;
	}
		
	public static ArrayList<Discount> getDiscountList() throws SQLException, IOException 
	{
		connect_to_db();
		ArrayList<Discount> discounts = new ArrayList<>();
		String query = "SELECT * FROM discount ORDER BY discount_DiscountName";

		try (Statement s = conn.createStatement()) {
			try (ResultSet rs = s.executeQuery(query)) {
				while (rs.next()) {
					int discID = rs.getInt(1);
					String discName = rs.getString(2);
					double discAmt = rs.getDouble(3);
					boolean isPercent = rs.getBoolean(4);

					Discount d = new Discount(discID, discName, discAmt, isPercent);
					discounts.add(d);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		conn.close();
		return discounts;
	}

	public static Discount findDiscountByName(String name) throws SQLException, IOException 
	{
		connect_to_db();
		String query = "SELECT * FROM discount WHERE discount_DiscountName = ?";

		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setString(1, name);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					int discID = rs.getInt(1);
					double discAmt = rs.getDouble(3);
					boolean isPercent = rs.getBoolean(4);

					return new Discount(discID, name, discAmt, isPercent);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		 return null;
	}


	public static ArrayList<Customer> getCustomerList() throws SQLException, IOException 
	{
		connect_to_db();
		ArrayList<Customer> customerList = new ArrayList<>();

		try {
			String query = "SELECT * FROM customer ORDER BY customer_LName, customer_FName, customer_PhoneNum;";

			Statement stmt = conn.createStatement();
			ResultSet rset = stmt.executeQuery(query);

			while (rset.next()) {
				int custID = rset.getInt("customer_CustID");
				String custFName = rset.getString("customer_Fname");
				String custLName = rset.getString("customer_LName");
				String custPhone = rset.getString("customer_PhoneNum");

				Customer customer = new Customer(custID, custFName, custLName, custPhone);
				customerList.add(customer);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		conn.close();

		return customerList;
	}

	public static Customer findCustomerByPhone(String phoneNumber)  throws SQLException, IOException 
	{
		connect_to_db();

		String query = "SELECT c.customer_CustID, c.customer_FName, c.customer_LName, c.customer_PhoneNum, " +
				       "d.delivery_HouseNum, d.delivery_Street, d.delivery_City, d.delivery_State, d.delivery_Zip " +
				       "FROM customer c LEFT JOIN ordertable o ON c.customer_CustID = o.customer_CustID " +
				       "LEFT JOIN delivery d ON o.ordertable_OrderID = d.ordertable_OrderID " +
				       "WHERE c.customer_PhoneNum = ? ";

		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setString(1, phoneNumber);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					int custID = rs.getInt(1);
					String fName = rs.getString(2);
					String lName = rs.getString(3);
					String phoneNum = rs.getString(4);
					String street = rs.getString(6);
					String city = rs.getString(7);
					String state = rs.getString(8);
					String zip = rs.wasNull() ? null : String.valueOf(rs.getInt(9));

					Customer cust = new Customer(custID, fName, lName, phoneNum);

					if (street != null && city != null && state != null && zip != null) {
						cust.setAddress(street, city, state, zip);
					}
					return cust;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		conn.close();
		return null;

	}

	public static String getCustomerName(int CustID) throws SQLException, IOException 
	{
		connect_to_db();

		String cname2 = "";
		try {
			PreparedStatement os;
			ResultSet rset2;
			String query2 = "Select customer_FName, customer_LName From customer WHERE customer_CustID=?;";
			os = conn.prepareStatement(query2);
			os.setInt(1, CustID);
			rset2 = os.executeQuery();
			while(rset2.next())
			{
				cname2 = rset2.getString(1) + " " + rset2.getString(2);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		conn.close();

		return cname2;
	}


	public static ArrayList<Topping> getToppingList() throws SQLException, IOException 
	{
		connect_to_db();

		ArrayList<Topping> toppingList = new ArrayList<>();

		String query = "SELECT * FROM topping ORDER BY topping_TopName";

		try (PreparedStatement ps = conn.prepareStatement(query);
			ResultSet rs = ps.executeQuery()
		) {
			while (rs.next()) {
				int TopID = rs.getInt(1);
				String TopName = rs.getString(2);
				double SmallAMT = rs.getDouble(3);
				double MedAMT = rs.getDouble(4);
				double LgAMT = rs.getDouble(5);
				double XlAMT = rs.getDouble(6);
				double CustPrice = rs.getDouble(7);
				double BusPrice = rs.getDouble(8);
				int MinINVT = rs.getInt(9);
				int CurINVT = rs.getInt(10);

				Topping topping = new Topping(TopID, TopName, SmallAMT, MedAMT, LgAMT, XlAMT, CustPrice, BusPrice, MinINVT, CurINVT);
				toppingList.add(topping);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		conn.close();

		return toppingList;
	}

	public static Topping findToppingByName(String name) throws SQLException, IOException 
	{
		connect_to_db();

		Topping topping = null;

		String query = "SELECT * FROM topping WHERE topping_TopName = ?";

		try (PreparedStatement ps = conn.prepareStatement(query)){
			ps.setString(1, name);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					int TopID = rs.getInt(1);
					String TopName = rs.getString(2);
					double SmallAMT = rs.getDouble(3);
					double MedAMT = rs.getDouble(4);
					double LgAMT = rs.getDouble(5);
					double XlAMT = rs.getDouble(6);
					double CustPrice = rs.getDouble(7);
					double BusPrice = rs.getDouble(8);
					int MinINVT = rs.getInt(9);
					int CurINVT = rs.getInt(10);

					topping = new Topping(TopID, TopName, SmallAMT, MedAMT, LgAMT, XlAMT, CustPrice, BusPrice, MinINVT, CurINVT);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		conn.close();

		return topping;
	}

	public static ArrayList<Topping> getToppingsOnPizza(Pizza p) throws SQLException, IOException 
	{
		String query = "SELECT topping.topping_TopID, topping_TopName, topping_SmallAMT, topping_MedAMT, topping_LgAMT, " +
				       "topping_XLAMT, topping_CustPrice, topping_BusPrice, topping_CurINVT, topping_MinINVT, pizza_topping_IsDouble" +
				       " FROM topping JOIN pizza_topping ON topping.topping_TopID = pizza_topping.topping_TopID" +
				       " WHERE pizza_PizzaID = ?";

		ArrayList<Topping> toppingList = new ArrayList<>();

		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setInt(1, p.getPizzaID());

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int topID = rs.getInt(1);
					String topName = rs.getString(2);
					double topSmall = rs.getDouble(3);
					double topMed = rs.getDouble(4);
					double topLarge = rs.getDouble(5);
					double topXL = rs.getDouble(6);
					double topCust = rs.getDouble(7);
					double topBus = rs.getDouble(8);
					int minInvt = rs.getInt(9);
					int curInvt = rs.getInt(10);
					boolean isDouble = rs.getBoolean(11);

					Topping topping = new Topping(topID, topName, topSmall, topMed, topLarge, topXL, topCust, topBus, minInvt, curInvt);
					topping.setDoubled(isDouble);
					toppingList.add(topping);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return toppingList;
	}

	public static void addToInventory(int toppingID, double quantity) throws SQLException, IOException 
	{
		connect_to_db();

		String query = "UPDATE topping SET topping_CurINVT = topping_CurINVT + ? WHERE topping_TopID = ?";

		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setInt(1, (int) quantity);
			ps.setInt(2, toppingID);

			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		conn.close();
	}
	
	
	public static ArrayList<Pizza> getPizzas(Order o) throws SQLException, IOException 
	{

		ArrayList<Pizza> pizzas = new ArrayList<>();
		String query = "SELECT * FROM pizza WHERE ordertable_OrderID = ?";

		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setInt(1, o.getOrderID());

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int pizzaID = rs.getInt(1);
					String pizzaSize = rs.getString(2);
					String crustType = rs.getString(3);
					String pizzaState = rs.getString(4);

					Timestamp timestamp = rs.getTimestamp(5);
					String pizzaDate = timestamp.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

					double custPrice = rs.getDouble(6);
					double busPrice = rs.getDouble(7);

					Pizza pizza = new Pizza(pizzaID, pizzaSize, crustType, o.getOrderID(), pizzaState, pizzaDate, custPrice, busPrice);

					ArrayList<Discount> pizzaDiscounts = getDiscounts(pizza);
					ArrayList<Topping> pizzaToppings = getToppingsOnPizza(pizza);

					pizza.setDiscounts(pizzaDiscounts);
					pizza.setToppings(pizzaToppings);

					pizzas.add(pizza);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return pizzas;
	}

	public static ArrayList<Discount> getDiscounts(Order o) throws SQLException, IOException 
	{
		ArrayList<Discount> orderDiscs = new ArrayList<>();
		String query = "SELECT d.discount_DiscountID, d.discount_DiscountName, d.discount_Amount, d.discount_IsPercent " +
				       "FROM  discount d JOIN order_discount od ON d.discount_DiscountID = od.discount_DiscountID " +
			     	   "WHERE ordertable_OrderID = ?";

		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setInt(1, o.getOrderID());

			try (ResultSet rs = ps.executeQuery()) {

				while (rs.next()) {
					int discID = rs.getInt(1);
					String discName = rs.getString(2);
					double discAmount = rs.getDouble(3);
					boolean isPercent = rs.getBoolean(4);

					Discount discount = new Discount(discID, discName, discAmount, isPercent);
					orderDiscs.add(discount);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return orderDiscs;
	}

	public static ArrayList<Discount> getDiscounts(Pizza p) throws SQLException, IOException 
	{
		ArrayList<Discount> pizzaDiscs = new ArrayList<>();
		String query = "SELECT d.discount_DiscountID, d.discount_DiscountName, d.discount_Amount, d.discount_IsPercent " +
						"FROM  discount d JOIN pizza_discount pd ON d.discount_DiscountID = pd.discount_DiscountID " +
				 		"WHERE pizza_PizzaID = ?";

		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setInt(1, p.getPizzaID());

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int discID = rs.getInt(1);
					String discName = rs.getString(2);
					double discAmount = rs.getDouble(3);
					boolean isPercent = rs.getBoolean(4);

					Discount discount = new Discount(discID, discName, discAmount, isPercent);
					pizzaDiscs.add(discount);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	
		return pizzaDiscs;
	}

	public static double getBaseCustPrice(String size, String crust) throws SQLException, IOException 
	{
		connect_to_db();
		String query = "SELECT baseprice_CustPrice FROM baseprice WHERE baseprice_Size = ? AND baseprice_CrustType = ?";

		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setString(1, size);
			ps.setString(2, crust);

			try(ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getDouble(1);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		conn.close();
		return 0.0;
	}

	public static double getBaseBusPrice(String size, String crust) throws SQLException, IOException 
	{
		connect_to_db();
		String query = "SELECT baseprice_BusPrice FROM baseprice WHERE baseprice_Size = ? AND baseprice_CrustType = ?";

		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setString(1, size);
			ps.setString(2, crust);

			try(ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getDouble(1);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		conn.close();
		return 0.0;
	}

	
	public static void printToppingPopReport() throws SQLException, IOException
	{
		connect_to_db();
		String query = "SELECT * FROM ToppingPopularity";

		try (Statement s = conn.createStatement()) {
			try (ResultSet rs = s.executeQuery(query)) {
				System.out.printf("%-20s%-20s\n", "Topping", "Topping Count");
				System.out.printf("%-20s%-20s\n", "-------", "-------------");

				while (rs.next()) {
					String name = rs.getString(1);
					int count = rs.getInt(2);

					System.out.printf("%-20s%-20s\n", name, count);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		conn.close();
	}
	
	public static void printProfitByPizzaReport() throws SQLException, IOException 
	{
		connect_to_db();
		String query = "SELECT * FROM ProfitByPizza";

		try (Statement s = conn.createStatement()) {
			try (ResultSet rs = s.executeQuery(query)) {
				System.out.printf("%-20s%-20s%-20s%-20s\n", "Pizza Size", "Pizza Crust", "Profit", "Last Order Date");
				System.out.printf("%-20s%-20s%-20s%-20s\n", "----------", "-----------", "------", "---------------");

				while (rs.next()) {
					String size = rs.getString(1);
					String crust = rs.getString(2);
					double profit = rs.getDouble(3);
					String lastDate = rs.getString(4);

					System.out.printf("%-20s%-20s%-20.2f%-20s\n", size, crust, profit, lastDate);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		conn.close();
	}
	
	public static void printProfitByOrderType() throws SQLException, IOException
	{
		connect_to_db();
		String query = "SELECT * FROM ProfitByOrderType";

		try (Statement s = conn.createStatement()) {
			try (ResultSet rs = s.executeQuery(query)) {
				System.out.printf("%-20s%-20s%-20s%-20s%-20s\n", "Customer Type", "Order Month", "Total Order Price", "Total Order Cost", "Profit");
				System.out.printf("%-20s%-20s%-20s%-20s%-20s\n", "-------------", "-----------", "-----------------", "----------------", "------");

				while (rs.next()) {
					String type = rs.getString(1);
					type = (type != null) ? type : "";
					String month = rs.getString(2);
					double totalPrice = rs.getDouble(3);
					double totalCost = rs.getDouble(4);
					double profit = rs.getDouble(5);


					System.out.printf("%-20s%-20s%-20.2f%-20.2f%-20.2f\n", type, month, totalPrice, totalCost, profit);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		conn.close();
	}

	// method to convert String to Timestamp
	public static Timestamp strToTimeStamp(String dateTimeString) {
		DateTimeFormatter formatter = new DateTimeFormatterBuilder()
				.appendPattern("yyyy-MM-dd HH:mm:ss")
				.optionalStart()
				.appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
				.optionalEnd()
				.toFormatter();
		LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, formatter);
		return Timestamp.valueOf(localDateTime);
	}

	// method to string to Date
	public static Date strToDate (String dateString) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

		try {
			return formatter.parse(dateString);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	// method to parse address
	private static String[] parseAddress (String address) {
		String[] components = address.split("\t");

		try {
			if (components.length != 5) {
				throw new IllegalAccessException("Enter Valid Address Format");
			}
		} catch (IllegalAccessException e) {
			return null;
		}
		return components;
	}

}