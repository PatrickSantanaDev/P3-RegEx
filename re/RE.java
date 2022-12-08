package re;

import java.util.Iterator;

import fa.nfa.NFA;
import fa.nfa.NFAState;
import fa.State;

/**
 * An object to take in a regex and convert it to an equivalent NFA
 * 
 * @author patricksantana austinnelson
 */
public class RE implements REInterface {

	// Input string
	String regExString = null;
	// NFA object
	NFA nfaObj = null;
	// NFA state count
	int stateCount = 0;

	/**
	 * Retrieves the input string
	 * 
	 * @param regEx inputted regular expression
	 */
	public RE(String regEx) {
		this.regExString = regEx;
	}

	@Override
	public NFA getNFA() {
		/* Creates NFA based on regex */
		nfaObj = new NFA();
		NFA compNFA = parse();
		// Adds character to new NFA alphabet
		compNFA.addAbc(nfaObj.getABC());

		return compNFA;
	}

	/**
	 * Parses through regex to generate NFA
	 * 
	 * @return newNFA
	 */
	private NFA parse() {
		NFA newNFA = new NFA();
		/* Parsing */
		while (more()) {
			newNFA = regex();
		}

		return newNFA;
	}

	/**
	 * Gets next input item without consuming
	 * 
	 * @return next item of input
	 */
	private char peek() {
		return regExString.charAt(0);
	}

	/**
	 * Consumes the next item of input, failing if not equal to item
	 * 
	 * @param c item to consume
	 * @throws RuntimeException if next item isn't passed in value
	 */
	private void eat(char c) {
		if (peek() == c) {
			this.regExString = this.regExString.substring(1);
		} else {
			throw new RuntimeException("Expected: " + c + "; got: " + peek());
		}
	}

	/**
	 * Returns the next item of input and consumes it
	 * 
	 * @return consumed item
	 */
	private char next() {
		char c = peek();
		eat(c);
		return c;
	}

	/**
	 * Checks if there is more input available in regExString
	 * 
	 * @return true if there's more, false if empty
	 */
	private boolean more() {
		return regExString.length() > 0;
	}

	/**
	 * Parse at least one term parse and perhaps another depending on what is
	 * returned
	 * 
	 * @return parsed term
	 */
	private NFA regex() {
		NFA term = term();

		/* Check to see if there is more to parse */
		if (more() && peek() == '|') {
			eat('|');
			// Parse again
			NFA regex = regex();
			return choice(term, regex);
		} else {
			return term;
		}
	}

	/**
	 * Establish a new NFA that is union between two NFAs using epsilon transition
	 * 
	 * @param nfaA first NFA
	 * @param nfaB second NFA
	 * @return union of the two NFAs
	 */
	private NFA choice(NFA nfaA, NFA nfaB) {
		NFA newNFA = new NFA();
		newNFA.addStartState(Integer.toString(stateCount++));

		/* Add states */
		newNFA.addNFAStates(nfaA.getStates());
		newNFA.addNFAStates(nfaB.getStates());

		/* Epsilon transition from new NFA to start states of nfaA and nfaB */
		((NFAState) newNFA.getStartState()).addTransition('e', (NFAState) nfaA.getStartState());
		((NFAState) newNFA.getStartState()).addTransition('e', (NFAState) nfaB.getStartState());

		return newNFA;
	}

	/**
	 * Extract a term ((possibly empty) sequence of factors)
	 * 
	 * @return factor(s) in term
	 */
	private NFA term() {
		NFA factor = null;

		/* Check to make sure it has not reached a boundary or end of input */
		while (more() && peek() != ')' && peek() != '|') {
			NFA nextFactor = factor();
			// Record the concatenation
			factor = sequence(factor, nextFactor);
		}

		return factor;
	}

	/**
	 * Record the concatenation of factors
	 * 
	 * @param nfaA first NFA
	 * @param nfaB second NFA
	 * @return concatenated NFA
	 */
	private NFA sequence(NFA nfaA, NFA nfaB) {

		/* Ensure not empty */
		if (nfaA == null) {
			return nfaB;
		}

		Iterator<State> iter = nfaA.getFinalStates().iterator();

		/* Connect nfaA's final state to nfaB's start state using epsilon transition */
		while (iter.hasNext()) {
			NFAState state = (NFAState) iter.next();
			state.setNonFinal();
			state.addTransition('e', (NFAState) nfaB.getStartState());

		}

		nfaA.addNFAStates(nfaB.getStates());
		return nfaA;
	}

	/**
	 * Parse a base and then any number of Kleene stars
	 * 
	 * @return base
	 */
	private NFA factor() {
		NFA base = base();

		/* Check for more */
		while (more() && peek() == '*') {
			eat('*');
			base = repetition(base);
		}

		return base;
	}

	/**
	 * Capture repetition in NFA base
	 * 
	 * @param nfa nfa to repeat
	 * @return altered (repeated) nfa
	 */
	private NFA repetition(NFA nfa) {
		Iterator<State> finalStates = nfa.getFinalStates().iterator();

		/* Establish epsilon transitions for start/final */
		while (finalStates.hasNext()) {
			NFAState currentState = (NFAState) finalStates.next();
			((NFAState) nfa.getStartState()).addTransition('e', currentState);
			currentState.addTransition('e', (NFAState) nfa.getStartState());
		}

		return nfa;
	}

	/**
	 * Checks to see which base (a character, an escaped character, or a
	 * parenthesized regular expression) has been encountered
	 * 
	 * @return encountered case
	 */
	private NFA base() {
		switch (peek()) {
			case '(':
				eat('(');
				NFA regEx = regex();
				eat(')');
				return regEx;

			case '\\':
				eat('\\');
				char esc = next();
				return primitive(esc);

			default:
				return primitive(next());
		}
	}

	/**
	 * Holds an individual character for new start/final transition
	 * 
	 * @param c individual character
	 * @return new nfa
	 */
	private NFA primitive(char c) {
		NFA nfa = new NFA();

		/* Add character to alphabet if not already */
		if (!nfaObj.getABC().contains(c)) {
			nfaObj.getABC().add(c);
		}

		/* Add states */
		nfa.addStartState(Integer.toString(stateCount++));
		nfa.addFinalState(Integer.toString(stateCount++));

		NFAState state = (NFAState) nfa.getStartState();
		state.addTransition(c, (NFAState) nfa.getFinalStates().iterator().next());
		return nfa;
	}
}