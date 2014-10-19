package event.trigger;

import java.util.ArrayList;

import model.EventMention;

public interface TriggerIndent {
	public ArrayList<EventMention> extractTrigger(String file);
	public ArrayList<EventMention> extractTriggerJi(String file);
}
