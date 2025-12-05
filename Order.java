package cpsc4620;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

public class Order 
{
	private int OrderID;
	private int CustID;
	private String OrderType;
	private String Date;
	private double CustPrice;
	private double BusPrice;
	private boolean isComplete;
	private ArrayList<Pizza> PizzaList;
	private ArrayList<Discount> DiscountList;

	public Order(int orderID, int custID, String orderType, String date, double custPrice, double busPrice, boolean iscomplete) {
		OrderID = orderID;
		CustID = custID;
		OrderType = orderType;
		Date = date;
		CustPrice = custPrice;
		BusPrice = busPrice;
		this.isComplete = iscomplete;
		PizzaList = new ArrayList<Pizza>();
		DiscountList = new ArrayList<Discount>();
	}
	
	public void addPizza(Pizza p)
	{
		PizzaList.add(p);
	}
	public void addDiscount(Discount d)
	{
		DiscountList.add(d);
		if(d.isPercent())
		{
			//this.BusPrice = (this.BusPrice*(1-d.getAmount()));
			this.CustPrice = (this.CustPrice*(1-d.getAmount()/100.0));
		}
		else
		{
			//this.BusPrice -= d.getAmount();
			this.CustPrice -= d.getAmount();
		}
	}

	public int getOrderID() {
		return OrderID;
	}

	public int getCustID() {
		return CustID;
	}

	public String getOrderType() {
		return OrderType;
	}

	public String getDate() {
		return Date;
	}

	public double getCustPrice() {
		return CustPrice;
	}

	public double getBusPrice() {
		return BusPrice;
	}
	
	public boolean getIsComplete() {
		return isComplete;
	}

	public ArrayList<Pizza> getPizzaList() {
		return PizzaList;
	}

	public ArrayList<Discount> getDiscountList() {
		return DiscountList;
	}

	public void setOrderID(int orderID) {
		OrderID = orderID;
	}

	public void setCustID(int custID) {
		CustID = custID;
	}

	public void setOrderType(String orderType) {
		OrderType = orderType;
	}

	public void setDate(String date) {
		Date = date;
	}

	public void setCustPrice(double custPrice) {
		CustPrice = custPrice;
	}

	public void setBusPrice(double busPrice) {
		BusPrice = busPrice;
	}

	public void setIsComplete(boolean iscomplete) {
		this.isComplete = iscomplete;
	}
	public void setPizzaList(ArrayList<Pizza> pizzaList) {
		PizzaList = pizzaList;
	}

	public void setDiscountList(ArrayList<Discount> discountList) {
		DiscountList = discountList;
	}

	@Override
	public String toString() {
		try 
		{
			String name = DBNinja.getCustomerName(CustID);
			name = (name != "")?name:"IN STORE";
			return "OrderID=" + OrderID + " | Customer name= " + name + ", OrderType= " + OrderType + ", IsComplete= " + ((isComplete)?"Yes":"No");
		} 
		catch (SQLException | IOException e)
		{
			e.printStackTrace();
			return "ERROR IN PRINT ORDER";
		}
	}

	public String toSimplePrint() 
	{
		try 
		{
			return "OrderID=" + OrderID + " | Customer name= " + DBNinja.getCustomerName(CustID) + ", OrderType= " + OrderType + ", IsComplete= " + ((isComplete)?"Yes":"No");
		} 
		catch (SQLException e) {
			e.printStackTrace();
			return "ERROR IN ORDER PRINT";
		} 
		catch (IOException e) {
			e.printStackTrace();
			return "ERROR IN ORDER PRINT";
		}
	}

	public String toFullPrint()
	{
		String orderSummary;
		String pizzaSummary;
		try
		{
			orderSummary = "OrderID=" + OrderID + " | For customer: " + DBNinja.getCustomerName(CustID) + " | OrderType= " + OrderType + ", Placed on: " + Date
					+ " | CustPrice= " + CustPrice + ", BusPrice= " + BusPrice;
			return orderSummary;
		}
		catch (SQLException e) {
			e.printStackTrace();
			return "ERROR IN ORDER PRINT";
		}
		catch (IOException e) {
			e.printStackTrace();
			return "ERROR IN ORDER PRINT";
		}
	}
}
