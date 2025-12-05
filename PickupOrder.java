package cpsc4620;

public class PickupOrder extends Order
{

	private boolean isPickedUp;
	
	public PickupOrder(int orderID, int custID, String date, double custPrice, double busPrice, boolean isPickedUp, boolean isComplete) {
		super(orderID, custID, DBNinja.pickup, date, custPrice, busPrice, isComplete);
		this.isPickedUp = isPickedUp;
	}

	public boolean getIsPickedUp() {
		return isPickedUp;
	}

	public void setIsPickedUp(boolean isPickedUp) {
		this.isPickedUp = isPickedUp;
	}
	
	@Override
	public String toString() {
		return super.toString() + " | Picked Up: " + ((isPickedUp)?"Yes":"No");
	}
}
