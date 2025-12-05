package cpsc4620;

import java.util.ArrayList;

public class Topping
{
	private int TopID;
	private String TopName;
	private double SmallAMT;
	private double MedAMT;
	private double LgAMT;
	private double XLAMT;
	private double CustPrice;
	private double BusPrice;
	private int MinINVT;
	private int CurINVT;
	private boolean isDoubled;
	
	public Topping(int topID, String topName, double smallAMT, double medAMT, double lgAMT, double xLAMT,
			double custPrice, double busPrice, int minINVT, int curINVT) {
		TopID = topID;
		TopName = topName;
		SmallAMT = smallAMT;
		MedAMT = medAMT;
		LgAMT = lgAMT;
		XLAMT = xLAMT;
		CustPrice = custPrice;
		BusPrice = busPrice;
		MinINVT = minINVT;
		CurINVT = curINVT;
		isDoubled = false;
	}

	public Topping () {

	}

	public int getTopID() {
		return TopID;
	}

	public String getTopName() {
		return TopName;
	}

	public double getSmallAMT() {
		return SmallAMT;
	}

	public double getMedAMT() {
		return MedAMT;
	}

	public double getLgAMT() {
		return LgAMT;
	}

	public double getXLAMT() {
		return XLAMT;
	}

	public boolean getDoubled() { return isDoubled; }

	public double getCustPrice() {
		return CustPrice;
	}

	public double getBusPrice() {
		return BusPrice;
	}

	public int getMinINVT() {
		return MinINVT;
	}

	public int getCurINVT() {
		return CurINVT;
	}

	public void setTopID(int topID) {
		TopID = topID;
	}

	public void setTopName(String topName) {
		TopName = topName;
	}

	public void setSmallAMT(double smallAMT) {
		SmallAMT = smallAMT;
	}

	public void setMedAMT(double medAMT) {
		MedAMT = medAMT;
	}

	public void setLgAMT(double lgAMT) {
		LgAMT = lgAMT;
	}

	public void setXLAMT(double xLAMT) {
		XLAMT = xLAMT;
	}

	public void setCustPrice(double custPrice) {
		CustPrice = custPrice;
	}

	public void setBusPrice(double busPrice) {
		BusPrice = busPrice;
	}

	public void setMinINVT(int minINVT) {
		MinINVT = minINVT;
	}
	public void setCurINVT(int curINVT) {
		CurINVT = curINVT;
	}

	public void setDoubled(boolean doubled) { isDoubled = doubled; }

	public static void printToppings (ArrayList<Topping> myToppings) {
		if (myToppings.isEmpty()) {
			System.out.println("NO TOPPINGS");
		} else {
			//System.out.print("Toppings: ");
			for (Topping t: myToppings) {
				System.out.println(t.pizzaTopping());
			}
			System.out.println();
		}
	}

	@Override
	public String toString() {
		return "Topping [TopID=" + TopID + ", TopName=" + TopName + ", smallAMT=" + SmallAMT + ", MedAMT=" + MedAMT
				+ ", LgAMT=" + LgAMT + ", XLAMT=" + XLAMT + ", CustPrice=" + CustPrice + ", BusPrice=" + BusPrice
				+ ", MinINVT=" + MinINVT + ", CurINVT=" + CurINVT + "]";
	}

	public String pizzaTopping() {
		return "Topping: " + TopName + ", Doubled?: " + ((isDoubled) ? "Yes" : "No");
	}

}
