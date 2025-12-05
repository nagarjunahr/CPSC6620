package cpsc4620;

import java.util.ArrayList;

public class Discount
{
	private int DiscountID;
	private String DiscountName;
	private double Amount;
	private boolean isPercent;
	
	public Discount(int discountID, String discountName, double amount, boolean isPercent) {
		DiscountID = discountID;
		DiscountName = discountName;
		Amount = amount;
		this.isPercent = isPercent;
	}

	public int getDiscountID() {
		return DiscountID;
	}

	public String getDiscountName() {
		return DiscountName;
	}

	public double getAmount() {
		return Amount;
	}

	public boolean isPercent() {
		return isPercent;
	}

	public void setDiscountID(int discountID) {
		DiscountID = discountID;
	}

	public void setDiscountName(String discountName) {
		DiscountName = discountName;
	}

	public void setAmount(double amount) {
		Amount = amount;
	}

	public void setPercent(boolean isPercent) {
		this.isPercent = isPercent;
	}

	public static void printDiscounts (ArrayList<Discount> myDiscounts) {
		if (myDiscounts.isEmpty()) {
			System.out.println("NO DISCOUNTS");
		} else {
			System.out.print("DISCOUNTS: ");
			for (Discount d : myDiscounts) {
				System.out.print(d.getDiscountName()+" ");
			}
			System.out.println();
		}
	}

	@Override
	public String toString() {
		return "Discount ID: "+ DiscountID + " Discount Name: " + DiscountName + ", Amount: " + ((isPercent)?"":"$")+Amount+((isPercent)?"%":"");
	}
	
}
