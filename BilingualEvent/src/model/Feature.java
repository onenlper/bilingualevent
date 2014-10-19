package model;

public class Feature implements Comparable{
	public int idx;
	
	public double value;

	@Override
	public int compareTo(Object arg0) {
		return (this.idx-((Feature)arg0).idx);
	}
	
	public Feature(int idx, double value) {
		this.idx = idx;
		this.value = value;
	}
	
	public int hashCode() {
		return idx;
	}
	
	public boolean equals(Object arg0) {
		return (this.idx==((Feature)arg0).idx);
	}
}
