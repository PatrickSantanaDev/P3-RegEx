package re;

import java.util.Iterator;

import fa.nfa.NFA;
import fa.nfa.NFAState;
import fa.State;

public class RE implements REInterface {
	
	//NFA object
	NFA nfaObj = null;
	//RegEx string
	String regExString = null;
	//state count for NFA
	int stateCount = 0;
	
	/**
	 * To set up the parsing object, we need to store its the input string internally
	 * @param regEx - regular expression
	 */
	public RE(String regEx) {
		this.regExString = regEx;
	}
	
	/**
	 * parse at least one term, and whether we parse another depends only on what we find afterward
	 * @return - term
	 */
	public NFA regex() {
		NFA term = term();

		if (more() && peek() == '|') {
			eat('|');
			NFA regex = regex();
			return choice(term, regex);
		} else {
			return term;
		}
	}
	
	/**
	 * returns the next item of input without consuming it
	 * @return - next item of input
	 */
	private char peek() {
		return regExString.charAt(0);
	}
	
	/**
	 * Establish new NFA that is union between two NFAs using epsilon transition
	 * @param nfaA
	 * @param nfaB
	 * @return - union of two NFAs
	 */
	private NFA choice(NFA nfaA, NFA nfaB) {
		NFA newNFA = new NFA();
		newNFA.addStartState(Integer.toString(stateCount++));
		
		newNFA.addNFAStates(nfaA.getStates());
		newNFA.addNFAStates(nfaB.getStates());
		
		//epsilon transition from new NFA to start states of nfaA and nfaB
		((NFAState)newNFA.getStartState()).addTransition('e', (NFAState)nfaA.getStartState());
		((NFAState)newNFA.getStartState()).addTransition('e', (NFAState)nfaB.getStartState());
		
		return newNFA;
	}
	
	/**
	 * consumes the next item of input, failing if not equal to item
	 * @param c
	 */
	private void eat(char c) {
		if (peek() == c) 
		{
			this.regExString = this.regExString.substring(1);
		}
		
		else
		{
			throw new RuntimeException("Expected: " + c + "; got: " + peek());
		}
	}
	
	/**
	 * checks if there is more input available in regExString
	 * @return
	 */
	private boolean more() {
		return regExString.length() > 0;
	}
	
	/**
	 * A term is a (possibly empty) sequence of factors
	 * check that it has not reached the boundary of a term or the end of the input
	 * record the concatenation if term contains additional factors
	 * @return factor(s) in term
	 */
	private NFA term() {
		NFA factor = null;

		while (more() && peek() != ')' && peek() != '|') {
			NFA nextFactor = factor();
			factor = sequence(factor, nextFactor);
		}

		return factor;
	}
	
	/**
	 * Record the concatenation of factors
	 * Connect nfaA's final state to nfaB's start state using epsilon as transition
	 * @param nfaA
	 * @param nfaB
	 * @return nfaA
	 */
	private NFA sequence(NFA nfaA, NFA nfaB) {
		
		if (nfaA == null) {
			return nfaB;
		}
		
		Iterator<State> iter = nfaA.getFinalStates().iterator();
		
		while (iter.hasNext()) 
		{
			NFAState state = (NFAState)iter.next();
			state.setNonFinal();
			state.addTransition('e', (NFAState)nfaB.getStartState());
			
		}
		
		nfaA.addNFAStates(nfaB.getStates());
		return nfaA;
	}
	
	/**
	 * parse a base and then any number of Kleene stars
	 * @return base
	 */
	private NFA factor() {
		NFA base = base();

		while (more() && peek() == '*') 
		{
			eat('*');
			base = repetition(base);
		}

		return base;
	}
	
	/**
	 * Capture repetition in NFA base. Establish epsilon transitions for start/final
	 * @param nfa
	 * @return nfa
	 */
	private NFA repetition(NFA nfa) {
		Iterator<State> finalStates = nfa.getFinalStates().iterator();

		while (finalStates.hasNext()) 
		{
			NFAState currentState = (NFAState)finalStates.next();
			((NFAState)nfa.getStartState()).addTransition('e', currentState);
			currentState.addTransition('e', (NFAState)nfa.getStartState());
		}

		return nfa;
	}
	
	/**
	 * A base is a character, an escaped character, or a parenthesized regular expression
	 * checks to see which of the three cases it has encountered
	 * @return - one of three cases
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
	 * holds an individual character for new start/final transition
	 * @param c - individual character
	 * @return - new nfa
	 */
	private NFA primitive(char c) {
		NFA nfa = new NFA();
		
		if (!nfaObj.getABC().contains(c)) 
		{
			nfaObj.getABC().add(c);
		}
		
		nfa.addStartState(Integer.toString(stateCount++));
		nfa.addFinalState(Integer.toString(stateCount++));
		
		NFAState state = (NFAState)nfa.getStartState();
		state.addTransition(c, (NFAState)nfa.getFinalStates().iterator().next());
		return nfa;
	}
	
	/**
	 * returns the next item of input and consumes it
	 * @return - char c
	 */
	private char next() {
		char c = peek();
		eat(c);
		return c;
	}
	
	/**
	 * apply parsing to regex to build new NFA
	 * @return - newNFA
	 */
	private NFA parse() {
		NFA newNFA = new NFA();
		while (more()) {
			newNFA = regex();
		}
		
		return newNFA;
	}
	
	/**
	 * 
	 */
	@Override
	public NFA getNFA() {
		nfaObj = new NFA();
		NFA compNFA = parse();
		compNFA.addAbc(nfaObj.getABC());
		
		return compNFA;
	}
	
	
	
//<regex> ::= <term> '|' <regex>
//		   |  <term>
//
//<term> ::= { <factor> }
//
//<factor> ::= <base> { '*' }
//     
//<base> ::= <char>
//      	|  '\' <char>
//   	    |  '(' <regex> ')'  
	

}
