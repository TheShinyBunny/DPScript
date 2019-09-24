package com.shinysponge.dpscript.old_tokenizer_which_is_bad;

public class TokenBad {

	private String token;
	private TokenTypeBad type;
	
	public TokenBad(String token, TokenTypeBad type) {
		this.token = token;
		this.type = type;
	}
	
	public String getToken() {
		return token;
	}
	
	public TokenTypeBad getType() {
		return type;
	}

	@Override
	public String toString() {
		return String.format("%s - %s", type, token);
	}
}