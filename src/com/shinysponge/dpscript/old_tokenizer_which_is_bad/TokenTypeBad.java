package com.shinysponge.dpscript.old_tokenizer_which_is_bad;

public enum TokenTypeBad {

	/** Absolutely nothing. */
	EMPTY,
	
	/** A token. For example, ( ) = , */
	TOKEN,
	
	/** First character is a letter, any proceeding characters are letters or numbers. */
	IDENTIFIER,
	
	/** A number. */
	INTEGER_LITERAL,
	
	/** Anything enclosed in double quotes. "Hello" "1" */
	STRING_LITERAL,

	OPERATOR,

	LINE_END;
}