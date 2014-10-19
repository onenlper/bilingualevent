package model;

public class Depend {
	public String type;
	
	public int first;
	
	public Depend(String type, int first, int second) {
		this.type = type;
		this.first = first;
		this.second = second;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getFirst() {
		return first;
	}

	public void setFirst(int first) {
		this.first = first;
	}

	public int getSecond() {
		return second;
	}

	public void setSecond(int second) {
		this.second = second;
	}

	public int second;
}
