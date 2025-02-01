package me.despical.pixelpainter.utils.data;

/**
 * @author Despical
 * <p>
 * Created at 5.08.2022
 */
public class AtomicHolder {

	private String string;
	private int integer;

	public void setString(String string) {
		this.string = string;
	}

	public String getString() {
		return string;
	}

	public void setInteger(int integer) {
		this.integer = integer;
	}

	public int getInteger() {
		return integer;
	}
}