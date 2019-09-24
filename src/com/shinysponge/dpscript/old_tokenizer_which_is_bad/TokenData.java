package com.shinysponge.dpscript.old_tokenizer_which_is_bad;

import java.util.regex.Pattern;

public class TokenData {

	private Pattern pattern;
	private TokenTypeBad type;
	
	public TokenData(Pattern pattern, TokenTypeBad type) {
		this.pattern = pattern;
		this.type = type;
	}
	
	public Pattern getPattern() {
		return pattern;
	}
	
	public TokenTypeBad getType() {
		return type;
	}
}