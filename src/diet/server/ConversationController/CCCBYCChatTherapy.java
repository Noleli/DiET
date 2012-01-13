package diet.server.ConversationController;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import diet.message.MessageCBYCDocChangeFromClient;
import diet.message.MessageCBYCTypingUnhinderedRequest;
import diet.parameters.ExperimentSettings;
import diet.parameters.Parameter;
import diet.parameters.StringParameter;
import diet.server.Conversation;
import diet.server.Participant;
import diet.server.CbyC.DocChange;
import diet.server.CbyC.Sequences;
import diet.server.CbyC.Sequence.EditSequence;
import diet.server.CbyC.Sequence.LimitedRecipientsSequence;
import diet.server.CbyC.Sequence.POSTagFilterSequenceNonTriggering;
import diet.server.CbyC.Sequence.Sequence;
import diet.utils.Dictionary;
import diet.utils.POSTagRegex;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class CCCBYCChatTherapy extends CCCBYCAbstractProbeCR {
	
	
	
	public void processCBYCTypingUnhinderedRequest(Participant sender,
			MessageCBYCTypingUnhinderedRequest mWTUR) {
		Vector participants=getC().getParticipants().getAllParticipants();
	    	if (participants.size() != 2)
			return;
		
		if (sender.equals(participants.elementAt(0)))
		{
		    Conversation.printWSln("CCCbyCProbe", "negative therapy");
		    
		    this.positiveTherapy=false;
		}
		else
		{
		    Conversation.printWSln("CCCbyCProbe", "positive therapy");
		    this.positiveTherapy=true;
		}
		
		
		
		Conversation.printWSln("FloorHolder", "Processing Floor request from"
				+ sender.getUsername());
		Conversation.printWSln("FloorHolder", "Finish Time of Frag is:"
				+ this.finishTimeOfFragSending + "\n");
		if (this.waitingOnNetworkError)
			return;
		Sequence topSeq=sS.getCurrentSequence();
		if (topSeq==null)
		{
			fhc.processFloorRequest(sender, mWTUR);
			return;
		}
		POSTagFilterSequenceNonTriggering pfs;
		if (topSeq instanceof POSTagFilterSequenceNonTriggering)
		{
			pfs=(POSTagFilterSequenceNonTriggering) topSeq;
		
			if (fhc.isTheSpeakerChanging(sender, mWTUR))
			{
				
				
				String fragFromCurTurn=pfs.getCurrentFrag();
			
				
				if (this.positiveTherapy)
				{
				    int randomIndex=this.r.nextInt(this.positiveInterventions.length);//we won't get repeats here. . . . 
				    this.setCaptureResponse(false);
				    if (randomIndex<this.positiveInterventions.length)
				    {					   
					
					if (!this.triggerFragSending(topSeq.getSender(), this.positiveInterventions[randomIndex])) fhc.processFloorRequest(sender, mWTUR);
					else return;
				    }
				    else
				    {
					Conversation.printWSln("CCCbyCProbe", "Chose to send frag if available");
					if (fragFromCurTurn==null)
					{
					    Conversation.printWSln("CCCbyCProbe", "Not available");
					    fhc.processFloorRequest(sender, mWTUR);
					    return;
					}
					if (!this.triggerFragSending(topSeq.getSender(), fragFromCurTurn)) fhc.processFloorRequest(sender, mWTUR);
					else return;
					
				    }
				}
				else
				{
				    int randomIndex=this.r.nextInt(this.negativeInterventions.length);
				    //int randomIndex=this.negativeInterventions.length;
				    this.setCaptureResponse(true);
				    if (randomIndex<this.negativeInterventions.length)
				    {					   
					    
					if (!this.triggerFragSending(topSeq.getSender(), this.negativeInterventions[randomIndex])) fhc.processFloorRequest(sender, mWTUR);
					else return;
				    }
				    else
				    {
					Conversation.printWSln("CCCbyCProbe", "Chose to send frag if available");
					if (fragFromCurTurn==null)
					{
					    Conversation.printWSln("CCCbyCProbe", "Not available");
					    fhc.processFloorRequest(sender, mWTUR);
					    return;
					}
					if (!this.triggerFragSending(topSeq.getSender(), fragFromCurTurn)) fhc.processFloorRequest(sender, mWTUR);
					else return;
					
				    }
					
				}
				
			}
			else fhc.processFloorRequest(sender, mWTUR);
		}
		else fhc.processFloorRequest(sender, mWTUR);
	}
	
   Vector<POSTagRegex> regexes = new Vector<POSTagRegex>();
	
	
	
	File regexFile = new File(System.getProperty("user.dir")
			+ File.separator + "fragmentFilters" + File.separator
			+ "constBoundaryRegexes.txt");
	
	File misspellingsFile = new File(System.getProperty("user.dir")
			+ File.separator + "fragmentFilters" + File.separator
			+ "misspellings.txt");
	String taggerFileName = System.getProperty("user.dir") + File.separator
			+ "utils" + File.separator
			+ "bidirectional-distsim-wsj-0-18.tagger";
	
	String[] positiveInterventions={"yeah.","yeah", "yep", "ok", "great", "okay", "uhu", "uhhu", "yes", "ye", "right", "I agree", "fine", "absolutely", "totally"};
	String[] negativeInterventions={"huh", "why", "what", "??", "what do you mean", "wat", "wot", "what are you on about", "I don't get it", "no way"};
	boolean positiveTherapy=false;
	MaxentTagger tagger;
	Dictionary dict;
	Parameter therapyCondition;
	

	public void initialize(Conversation c, ExperimentSettings expS) {
		super.initialize(c, expS);
		super.minimumSpeakerChangesBetweenInterventions.setValue(4);
		this.sendRandNetError=false;
		this.therapyCondition = this.expSettings
		.getParameter("Therapy Condition");//"positive" or "negative"
		String condition=(String)this.therapyCondition.getValue();
		if (condition.equalsIgnoreCase("positive")) this.positiveTherapy=true;
		else this.positiveTherapy=false;
		
		Conversation
				.printWSln("Main", "Initialising Stanford POS-Tagger . . .");
		try {

			this.tagger = new MaxentTagger(this.taggerFileName);
			Conversation
					.printWSln("Main", "Initialisation ended successfully.");
			Conversation.printWSln("Main", "Loading Regexes");
			this.loadRegexes();
			Conversation.printWSln("Main", "Loaded Regexes");
			Conversation.printWSln("Main", "Loading Dictionary");
			this.dict = new Dictionary(System.getProperty("user.dir")
					+ File.separator + "utils" + File.separator
					+ "fulldictionary.txt");
			Conversation.printWSln("Main", "Loaded Dictionary");
			Conversation.printWSln("Main", "Loading Misspellings Map");
			this.loadMisspellings();
			Conversation.printWSln("Main", "Loaded Misspellings Map");

		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	TreeMap<String, String> misspellings = new TreeMap<String, String>();

	public synchronized void loadMisspellings() throws IOException {
		misspellings = new TreeMap<String, String>();
		Conversation.printWSln("Main", "loading misspellings");
		BufferedReader in = new BufferedReader(new FileReader(
				this.misspellingsFile));
		String line;
		while ((line = in.readLine()) != null) {
			String[] halves = getHalves(line);
			misspellings.put(halves[0], halves[1]);
		}

	}

	private String[] getHalves(String whole) {
		String[] result = new String[2];
		for (int i = 0; i < whole.length(); i++) {
			if (whole.substring(i, i+1).matches("\\s+")) {
				result[0] = whole.substring(0, i);
				result[1] = whole.substring(i + 1, whole.length());
				return result;
			}
		}
		return null;
	}

	private void loadRegexes() throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(
				this.regexFile));
		
		String regex;
		while ((regex = reader.readLine()) != null) {
			if (regex.startsWith("%"))
				continue;
			String[] split = regex.split(" ");
			regexes.add(new POSTagRegex(split[2], split[0], split[1]));
		}
		reader.close();
		
	}
	public Sequence getNextSequence_Speaker_Change(Sequence prior,
			int sequenceNo, Sequences ss, Participant p,
			MessageCBYCTypingUnhinderedRequest mCTUR) {

		if (this.finishTimeOfFragSending > 0 && fragAppOrigin != null) {
			System.out.println("Returning limited rec sequence");
			Vector v = getC().getParticipants().getAllOtherParticipants(
					fragAppOrigin);
			Vector v2 = (Vector) v.clone();
			v2.remove(p);
			receivingResponse = true;
			return new LimitedRecipientsSequence(ss, this, p.getUsername(), v2,
					p, this.probeCondition);
		} else {
			
			this.speakerChangesSinceLastIntervention++;
			return new POSTagFilterSequenceNonTriggering(ss, this, p.getUsername(), mCTUR,
					tagger, this.regexes, this.dict,
					this.misspellings);
		}
	}

	public Sequence getNextSequence_Edit_By_Different_Speaker(Sequence prior,
			int sequenceNo, Sequences ss, Participant p,
			MessageCBYCTypingUnhinderedRequest mCTUR) {
		Conversation
				.printWSln(
						"ERROR",
						"getNextSequence_Edit_By_Different_Speaker: This should not happen. Edits should be disabled");
		return new EditSequence(sS, this, p.getUsername(), mCTUR);
	}

	public Sequence getNextSequence_NewLine_By_Same_Speaker(Sequence prior,
			int sequenceNo, Sequences sS, String sender,
			MessageCBYCDocChangeFromClient mCDC) {
		Conversation.printWSln("CCCbyCProbe",
				"getNextSequence_NewLine_By_Same_Speaker");
		Participant p = this.getC().getParticipants()
				.findParticipantWithUsername(sender);
		if (this.finishTimeOfFragSending > 0 && fragAppOrigin != null) {
			System.out.println("Returning limited rec sequence");
			Vector v = getC().getParticipants().getAllOtherParticipants(
					fragAppOrigin);
			Vector v2 = (Vector) v.clone();
			v2.remove(p);
			receivingResponse = true;
			return new LimitedRecipientsSequence(sS, this, p.getUsername(), v2,
					p, this.probeCondition);
		} else {
			/* commented out as we do not want to intervene when speaker presses enter.
			String frag;
			Sequence s=sS.getCurrentSequence();
			POSTagFilterSequenceNonTriggering pfs;
			if (s instanceof POSTagFilterSequenceNonTriggering)
			{
				pfs=(POSTagFilterSequenceNonTriggering)s;
			}
			else 
			{
					Conversation.printWSln("ERROR", "Got wrong top sequence type in new line by same speaker");
					return null;
			}
			frag=pfs.getCurrentFrag();
			if (frag!=null)
			{
				if (this.triggerFragSending(p.getUsername(), frag)) return null;
				else
				{
					DocChange dc = mCDC.getDocChange();
					return new POSTagFilterSequenceNonTriggering(sS, this, sender, mCDC
							.getTimeStamp(), dc.elementString, dc.elementStart,
							dc.elementFinish, this.tagger, this.regexes, this.dict,
							this.misspellings, this.probeCondition);
					
				}
				
			}
			else
			{
			
			*/
				DocChange dc = mCDC.getDocChange();
				return new POSTagFilterSequenceNonTriggering(sS, this, sender, mCDC
						.getTimeStamp(), dc.elementString, dc.elementStart,
						dc.elementFinish, this.tagger, this.regexes, this.dict,
						this.misspellings, this.probeCondition);
			//}
		}
	}

	public Sequence getNextSequence_Edit_By_Same_Speaker(Sequence prior,
			int sequenceNo, Sequences sS, String sender,
			MessageCBYCDocChangeFromClient mCDC) {
		Conversation
				.printWSln(
						"ERROR",
						"getNextSequence_Edit_By_Same_Speaker: This should not happen. Edits should be disabled");

		DocChange dc = mCDC.getDocChange();
		EditSequence seq2 = new EditSequence(sS, this, sender, mCDC
				.getTimeStamp(), dc.elementString, dc.elementStart,
				dc.elementFinish);
		return seq2;
	}
	
	
	
	
	protected Participant chooseFragApparentOrigin(String antecedentOwner) {
		Participant antecedentOwnerP = getC().getParticipants()
		.findParticipantWithUsername(antecedentOwner);
		return this.chooseRandomParticipant(getC()
				.getParticipants().getAllOtherParticipants(antecedentOwnerP));
		
	}

	private Participant chooseRandomParticipant(Vector participants) {

		Random r = new Random(new Date().getTime());

		int pIndex = r.nextInt(participants.size());
		Participant p = (Participant) participants.elementAt(pIndex);
		return p;
	}
	

}