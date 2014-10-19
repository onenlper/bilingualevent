package model.stanford;

public class StanfordTimex {
	String tid;
	
	public String getTid() {
		return tid;
	}

	public void setTid(String tid) {
		this.tid = tid;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTimex() {
		return timex;
	}

	public void setTimex(String timex) {
		this.timex = timex;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	String type;
	
	String timex;
	
	String value;
}
