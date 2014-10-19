package model.stanford;

public class StanfordToken {
	public String word;
	public String lemma;
	public int CharacterOffsetBegin;
	public int CharacterOffsetEnd;
	public String POS;
	public String ner;
	public int id;
	
	public int sentenceId;

	String NormalizedNER;

	public int getSentenceId() {
		return sentenceId;
	}

	public void setSentenceId(int sentenceId) {
		this.sentenceId = sentenceId;
	}

	public StanfordTimex getTimex() {
		return timex;
	}

	public void setTimex(StanfordTimex timex) {
		this.timex = timex;
	}

	StanfordTimex timex;
	
	public String getNormalizedNER() {
		return NormalizedNER;
	}

	public void setNormalizedNER(String normalizedNER) {
		NormalizedNER = normalizedNER;
	}
	
	public int hashCode() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.CharacterOffsetBegin).append("_").append(this.CharacterOffsetEnd);
		return this.word.hashCode();
	}
	
	public boolean equals(Object obj) {
		return (this.CharacterOffsetBegin==((StanfordToken)obj).CharacterOffsetBegin
				&& (this.CharacterOffsetEnd==((StanfordToken)obj).CharacterOffsetEnd));
	}
	
	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public String getLemma() {
		return lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public int getCharacterOffsetBegin() {
		return CharacterOffsetBegin;
	}

	public void setCharacterOffsetBegin(int characterOffsetBegin) {
		CharacterOffsetBegin = characterOffsetBegin;
	}

	public int getCharacterOffsetEnd() {
		return CharacterOffsetEnd;
	}

	public void setCharacterOffsetEnd(int characterOffsetEnd) {
		CharacterOffsetEnd = characterOffsetEnd;
	}

	public String getPOS() {
		return POS;
	}

	public void setPOS(String pOS) {
		POS = pOS;
	}

	public String getNer() {
		return ner;
	}

	public void setNer(String ner) {
		this.ner = ner;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}